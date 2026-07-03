package com.bank.loanengine.config;

import com.bank.loanengine.messaging.producer.LoanEventProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares Kafka topics as Spring beans. Spring Boot's {@code KafkaAdmin} auto-creates them
 * on application startup if they do not already exist in the broker.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.loan-created.partitions:3}")
    private int loanCreatedPartitions;

    @Value("${app.kafka.topic.loan-created.replicas:1}")
    private short loanCreatedReplicas;

    @Value("${app.kafka.topic.prepayment-applied.partitions:3}")
    private int prepaymentAppliedPartitions;

    @Value("${app.kafka.topic.prepayment-applied.replicas:1}")
    private short prepaymentAppliedReplicas;

    @Bean
    public NewTopic loanCreatedTopic() {
        return TopicBuilder.name(LoanEventProducer.TOPIC_LOAN_CREATED)
                .partitions(loanCreatedPartitions)
                .replicas(loanCreatedReplicas)
                .build();
    }

    @Bean
    public NewTopic prepaymentAppliedTopic() {
        return TopicBuilder.name(LoanEventProducer.TOPIC_PREPAYMENT_APPLIED)
                .partitions(prepaymentAppliedPartitions)
                .replicas(prepaymentAppliedReplicas)
                .build();
    }
}
