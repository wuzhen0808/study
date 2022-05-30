package org.example

import org.apache.camel.component.kafka.KafkaComponent

class DefaultApp extends App {

    @Override
    KafkaComponent buildKafkaComponent() {
        KafkaComponent component = super.buildKafkaComponent()
        String brokers = cmdLine.getOptionValue("b")
        if (!brokers) {
            brokers = "localhost:9092"
        }

        log("using brokers:${brokers}")
        component.configuration.brokers = brokers
        return component

    }
}
