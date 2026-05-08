package com.bcommerce.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "bcommerce.fulfillment";
    public static final String QUEUE = "bcommerce.fulfillment.queue";
    public static final String ROUTING_KEY = "order.created";

    public static final String RISK_QUEUE = "bcommerce.risk.queue";
    public static final String RISK_ROUTING_KEY = "order.risk";

    @Bean
    TopicExchange fulfillmentExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue fulfillmentQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    Binding fulfillmentBinding(
            @Qualifier("fulfillmentQueue") Queue fulfillmentQueue, TopicExchange fulfillmentExchange) {
        return BindingBuilder.bind(fulfillmentQueue).to(fulfillmentExchange).with(ROUTING_KEY);
    }

    @Bean
    Queue riskQueue() {
        return new Queue(RISK_QUEUE, true);
    }

    @Bean
    Binding riskBinding(@Qualifier("riskQueue") Queue riskQueue, TopicExchange fulfillmentExchange) {
        return BindingBuilder.bind(riskQueue).to(fulfillmentExchange).with(RISK_ROUTING_KEY);
    }
}
