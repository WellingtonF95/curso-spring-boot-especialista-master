package io.github.dougllasfps.config;

import java.util.HashMap;
import java.util.Map;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.consumerPedido.PedidoAvro;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServer;

    @Value("${spring.kafka.producer.key-serializer}")
    private String keySerialization;

    @Value("${spring.kafka.producer.value-serializer}")
    private String valueSerialization;

    @Value("${spring.kafka.producer.topic}")
    private String topicName;

    @Value("${spring.kafka.schema-registry-url}")
    private String schemaRegistry;

    @Bean
    public ProducerFactory<String, PedidoAvro> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerialization);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerialization);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistry);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public NewTopic createNewTopic() {
        return new NewTopic(topicName,2, (short) 1);
    }

    @Bean
    public KafkaTemplate<String, PedidoAvro> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}