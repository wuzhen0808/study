package org.example

import org.apache.camel.CamelContext
import org.apache.camel.component.kafka.KafkaComponent
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

class EmbeddedKafkaApp extends App {
    KafkaComponent kafkaComponent;
    KafkaContainer kafkaContainer;

    @Override
    KafkaComponent buildKafkaComponent() {
        kafkaComponent = super.buildKafkaComponent()
        return kafkaComponent
    }

    @Override
    void start(CamelContext cc) {
        startKafka(cc)
        kafkaComponent.configuration.brokers = kafkaContainer.getHost() + ":" + kafkaContainer.getMappedPort(9093)
        super.start(cc)

    }

    @Override
    void stop(CamelContext cc) {
        super.stop(cc)
        kafkaContainer.stop()
    }

    private void startKafka(CamelContext cc) {
        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.2"))
        kafkaContainer.start()
    }


}
