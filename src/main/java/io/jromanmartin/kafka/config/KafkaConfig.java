package io.jromanmartin.kafka.config;

import io.apicurio.registry.serde.SerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaDeserializer;
import io.apicurio.registry.serde.avro.AvroKafkaSerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;
import io.apicurio.registry.serde.avro.strategy.RecordIdStrategy;
import io.jromanmartin.kafka.schema.avro.Message;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

@ApplicationScoped
public class KafkaConfig {

    @ConfigProperty(name = "app.kafka.bootstrap-servers", defaultValue = "localhost:9092")
    String kafkaBrokers;

    @ConfigProperty(name = "app.kafka.user.name", defaultValue = "usr-undefined")
    String kafkaUser;

    @ConfigProperty(name = "app.kafka.user.password", defaultValue = "pwd-undefined")
    String kafkaPassword;

    @ConfigProperty(name = "app.kafka.security.protocol", defaultValue = "security-protocol-undefined")
    String kafkaSecurityProtocol;

    @ConfigProperty(name = "app.producer.clienId", defaultValue = "kafka-client-sb-producer-client")
    String producerClientId;

    @ConfigProperty(name = "app.producer.acks", defaultValue = "1")
    String acks;

    @ConfigProperty(name = "app.consumer.groupId", defaultValue = "kafka-client-quarkus-consumer")
    String consumerGroupId;

    @ConfigProperty(name = "app.consumer.clientId", defaultValue = "kafka-client-quarkus-consumer-client")
    String consumerClientId;

    @ConfigProperty(name = "app.consumer.maxPoolRecords", defaultValue = "1000")
    String maxPoolRecords;

    @ConfigProperty(name = "app.consumer.offsetReset", defaultValue = "earliest")
    String offsetReset;

    @ConfigProperty(name = "app.consumer.autoCommit", defaultValue = "false")
    String autoCommit;

    @ConfigProperty(name = "apicurio.registry.url", defaultValue = "http://localhost:8080/api")
    String serviceRegistryUrl;

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "UnknownHost";
        }
    }

    @Produces
    @RequestScoped
    public Producer<String, Message> createProducer() {
        Properties props = new Properties();

        // Kafka Bootstrap
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);

        // Security
        props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, kafkaSecurityProtocol);
        props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");
        props.put(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"" + kafkaUser
                        + "\" password=\"" + kafkaPassword + "\";");

        // Producer Client
        props.putIfAbsent(ProducerConfig.CLIENT_ID_CONFIG, producerClientId + "-" + getHostname());

        // Serializer for Keys and Values
        props.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroKafkaSerializer.class.getName());

        // Service Registry
        props.putIfAbsent(SerdeConfig.REGISTRY_URL, serviceRegistryUrl);
        // Artifact Id Strategies (implementations of ArtifactIdStrategy)
        // Simple Topic Id Strategy (schema = topicName)
        //props.putIfAbsent(SerdeConfig.ARTIFACT_RESOLVER_STRATEGY, SimpleTopicIdStrategy.class.getName());
        // Topic Id Strategy (schema = topicName-(key|value)) - Default Strategy
        //props.putIfAbsent(SerdeConfig.ARTIFACT_RESOLVER_STRATEGY, TopicIdStrategy.class.getName());
        // Record Id Strategy (schema = full name of the schema (namespace.name))
        props.putIfAbsent(SerdeConfig.ARTIFACT_RESOLVER_STRATEGY, RecordIdStrategy.class.getName());
        // Topic Record Id Strategy (schema = topic name and the full name of the schema (topicName-namespace.name)
        //props.putIfAbsent(SerdeConfig.ARTIFACT_RESOLVER_STRATEGY, TopicRecordIdStrategy.class.getName());

        // Global Id Strategies (implementations of GlobalIdStrategy)
        //props.putIfAbsent(AbstractKafkaSerializer.REGISTRY_GLOBAL_ID_STRATEGY_CONFIG_PARAM, FindLatestIdStrategy.class.getName());
        //props.putIfAbsent(AbstractKafkaSerializer.REGISTRY_GLOBAL_ID_STRATEGY_CONFIG_PARAM, FindBySchemaIdStrategy.class.getName());
        //props.putIfAbsent(AbstractKafkaSerializer.REGISTRY_GLOBAL_ID_STRATEGY_CONFIG_PARAM, GetOrCreateIdStrategy.class.getName());
        //props.putIfAbsent(AbstractKafkaSerializer.REGISTRY_GLOBAL_ID_STRATEGY_CONFIG_PARAM, AutoRegisterIdStrategy.class.getName());

        // Auto-register Artifact into Service Registry
        props.putIfAbsent(SerdeConfig.AUTO_REGISTER_ARTIFACT, true);

        // Using JSON encoding (to help in debugging)
        props.put(AvroKafkaSerdeConfig.AVRO_ENCODING, "JSON");

        // Acknowledgement
        props.putIfAbsent(ProducerConfig.ACKS_CONFIG, acks);

        return new KafkaProducer<>(props);
    }

    @Produces
    @RequestScoped
    public Consumer<String, Message> createConsumer() {
        Properties props = new Properties();

        // Kafka Bootstrap
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);

        // Security
        props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, kafkaSecurityProtocol);
        props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");
        props.put(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"" + kafkaUser
                        + "\" password=\"" + kafkaPassword + "\";");

        /*
         * With group id, kafka broker ensures that the same message is not consumed more then once by a
         * consumer group meaning a message can be only consumed by any one member a consumer group.
         *
         * Consumer groups is also a way of supporting parallel consumption of the data i.e. different consumers of
         * the same consumer group consume data in parallel from different partitions.
         */
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);

        /*
         * In addition to group.id, each consumer also identifies itself to the Kafka broker using consumer.id.
         * This is used by Kafka to identify the currently ACTIVE consumers of a particular consumer group.
         */
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, consumerClientId + "-" + getHostname());

        // Deserializers for Keys and Values
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroKafkaDeserializer.class.getName());

        // Pool size
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPoolRecords);

        /*
         * If true the consumer's offset will be periodically committed in the background.
         * Disabled to allow commit or not under some circumstances
         */
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit);

        /*
         * What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the
         * server:
         *   earliest: automatically reset the offset to the earliest offset
         *   latest: automatically reset the offset to the latest offset
         */
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);

        // Service Registry Integration
        props.put(SerdeConfig.REGISTRY_URL, serviceRegistryUrl);
        // Use Specific Avro classes instead of the GenericRecord class definition
        props.put(AvroKafkaSerdeConfig.USE_SPECIFIC_AVRO_READER, true);

        return new KafkaConsumer<>(props);
    }

}
