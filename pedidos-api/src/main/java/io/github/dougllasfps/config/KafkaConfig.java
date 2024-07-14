package io.github.dougllasfps.config;

import java.util.HashMap;
import java.util.Map;

import io.github.dougllasfps.rest.dto.PedidoDTO;
import org.apache.kafka.clients.producer.ProducerConfig;
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

    @Bean
    public ProducerFactory<String, PedidoDTO> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerialization);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerialization);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, PedidoDTO> kafkaTemplate() {
        KafkaTemplate<String, PedidoDTO> template = new KafkaTemplate<>(producerFactory());
        return new KafkaTemplate<String, PedidoDTO>(producerFactory());
    }
}