package io.github.dougllasfps.config;

import io.github.dougllasfps.constants.RabbitConstants;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class RabbitMQConfig {

    public static final String EXCHANGE_AMQ_DIRECT = "amq.direct";

    public static final String EXCHANGE_PEDIDO_DLX = "exchange.pedido.dlx";

    private final AmqpAdmin amqpAdmin;

    public RabbitMQConfig(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public Queue queue(String queueName) {
        return new Queue(queueName, true, false, false);
    }

    public DirectExchange directExchange(String nameExchange) {
        return new DirectExchange(nameExchange, true, false, null);
    }
    @Bean
    public DirectExchange directExchangeDLX() {
        return new DirectExchange(EXCHANGE_PEDIDO_DLX, true, false, null);
    }

    public Binding bindingExchangeQueue(Queue queueName, DirectExchange exchangeName) {
        return new Binding(queueName.getName(),
                Binding.DestinationType.QUEUE, EXCHANGE_AMQ_DIRECT,
                RabbitConstants.QUEUE_PEDIDO, null);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @PostConstruct
    public void addQueueToRabbitMQ() {
        Queue newQueue = this.queue(RabbitConstants.QUEUE_PEDIDO);
        DirectExchange useAmqDirectExchange = this.directExchange(EXCHANGE_AMQ_DIRECT);

        Binding binding = this.bindingExchangeQueue(newQueue, useAmqDirectExchange);

        this.amqpAdmin.declareQueue(newQueue);
        this.amqpAdmin.declareExchange(useAmqDirectExchange);
        this.amqpAdmin.declareBinding(binding);
    }
}
