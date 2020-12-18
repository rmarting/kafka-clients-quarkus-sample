package io.jromanmartin.kafka.listener;

import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class ConsumerListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerListener.class);

    @Incoming("messages")
    public CompletionStage handleMessages(Message message) {
        IncomingKafkaRecord<String, io.jromanmartin.kafka.schema.avro.Message> incomingKafkaRecord =
                (IncomingKafkaRecord<String, io.jromanmartin.kafka.schema.avro.Message>) message.unwrap(IncomingKafkaRecord.class);

        LOGGER.info("Received record from Topic-Partition '{}-{}' with Offset '{}' -> Key: '{}' - Value '{}'",
                incomingKafkaRecord.getTopic(),
                incomingKafkaRecord.getPartition(),
                incomingKafkaRecord.getOffset(),
                incomingKafkaRecord.getKey(),
                incomingKafkaRecord.getPayload());

        // Commit message
        return message.ack();
    }

}
