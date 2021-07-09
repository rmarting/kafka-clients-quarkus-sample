package io.jromanmartin.kafka.service;

import io.jromanmartin.kafka.dto.MessageDTO;
import io.jromanmartin.kafka.dto.MessageListDTO;
import io.jromanmartin.kafka.schema.avro.Message;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

/**
 * Service for Kafka service which includes a set of primitives to manage events and topics such as:
 * 1. consume events from topic
 * 2. send event to topic
 * 3. subscribe topic
 * <p>
 * Additionally, it only processes messages of type {@link Message}
 *
 * @author rmarting
 */
@Singleton
public class MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageService.class);

    @Inject
    Producer<String, Message> producer;

    @Inject
    Consumer<String, Message> consumer;

    @ConfigProperty(name = "app.consumer.poolTimeout")
    Long poolTimeout;

    public MessageDTO publishSync(final @NotEmpty String topicName, final @NotNull MessageDTO messageDTO) {
        return publishRawMessage(topicName, messageDTO, false);
    }

    public MessageDTO publishAsync(final @NotEmpty String topicName, final @NotNull MessageDTO messageDTO) {
        return publishRawMessage(topicName, messageDTO, true);
    }

    private MessageDTO publishRawMessage(final @NotEmpty String topicName,
                                         final @NotNull MessageDTO messageDTO,
                                         final boolean async) {
        // Message to send
        // TODO Create a Mapper
        var message = new Message();
        message.setContent(messageDTO.getContent());
        message.setTimestamp(System.currentTimeMillis());

        // Record with a CustomMessage as value
        ProducerRecord<String, Message> record = null;

        // Topic Name depends of the runtime mode
        String finalTopicName = topicName;

        if (null == messageDTO.getKey()) {
            // Value as CustomMessage
            record = new ProducerRecord<>(finalTopicName, message);
        } else {
            // Value as CustomMessage
            record = new ProducerRecord<>(finalTopicName, messageDTO.getKey(), message);
        }

        try {
            if (async) {
                producer.send(record, (metadata, exception) -> {
                    LOGGER.info("Record ASYNCHRONOUSLY sent to topics and partition {}{} with offset {}",
                            finalTopicName, metadata.partition(), metadata.offset());

                    // Update model
                    messageDTO.setPartition(metadata.partition());
                    messageDTO.setOffset(metadata.offset());
                    messageDTO.setTimestamp(message.getTimestamp());
                }).get();
            } else {
                RecordMetadata metadata = producer.send(record).get();

                LOGGER.info("Record sent to topic and partition {}{} with offset {}",
                        finalTopicName, metadata.partition(), metadata.offset());

                // Update model
                messageDTO.setPartition(metadata.partition());
                messageDTO.setOffset(metadata.offset());
                messageDTO.setTimestamp(message.getTimestamp());
            }
        } catch (ExecutionException e) {
            LOGGER.warn("Execution Error in sending record", e);
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted Error in sending record", e);
        } finally {
            producer.flush();
            producer.close();
        }

        return messageDTO;
    }

    public MessageListDTO pollEvents(final @NotEmpty String topicName, final Integer partition, final boolean commit) {
        // Response objects
        var messageListDTO = new MessageListDTO();

        // Topic Name depends of the runtime mode
        String finalTopicName = topicName;

        try {
            // Assign to partition defined
            if (null != partition) {
                var topicPartition = new TopicPartition(finalTopicName, partition);
                consumer.assign(Collections.singletonList(topicPartition));

                LOGGER.info("Consumer assigned to topic {} and partition {}", finalTopicName, partition);
            } else {
                // Subscribe to Topic
                consumer.subscribe(Collections.singletonList(finalTopicName));

                LOGGER.info("Consumer subscribed to topic {}", finalTopicName);
            }

            LOGGER.info("Polling records from topic {}", finalTopicName);

            ConsumerRecords<String, Message> consumerRecords = consumer.poll(Duration.ofSeconds(poolTimeout));

            LOGGER.info("Polled #{} records from topic {}", consumerRecords.count(), finalTopicName);

            consumerRecords.forEach(record -> {
                var messageDTO = new MessageDTO();
                // TODO Create a Mapper
                messageDTO.setTimestamp((Long) record.value().get("timestamp"));
                messageDTO.setContent(record.value().get("content").toString());
                // Record Metadata
                messageDTO.setKey((null != record.key() ? record.key() : null));
                messageDTO.setPartition(record.partition());
                messageDTO.setOffset(record.offset());

                messageListDTO.addCustomMessage(messageDTO);
            });

            // Commit consumption
            if (commit) {
                consumer.commitAsync();

                LOGGER.info("Records committed in topic {} from consumer", finalTopicName);
            }
        } finally {
            consumer.close();
        }

        return messageListDTO;
    }

}
