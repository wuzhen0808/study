package org.example

import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.component.bean.BeanComponent
import org.apache.camel.component.kafka.DefaultKafkaClientFactory
import org.apache.camel.component.kafka.KafkaComponent
import org.apache.camel.component.log.LogComponent
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.SimpleTypeConverter
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer

class App {
    static void main(String[] args) {
        new App().run()
    }


    private brokers = '172.18.226.42:9092'

    void run() throws Exception {

        CamelContext cc = new DefaultCamelContext()
        runInternal cc

    }

    void log(String msg) {
        System.out.println msg
    }

    void runInternal(CamelContext cc) throws Exception {
        log "running."

        cc.registry.bind "h1", new Handler1()
        cc.registry.bind "h2", new Handler2()
        cc.registry.bind "h3", new Handler3()

        cc.typeConverterRegistry.addTypeConverter Handler3.MyMessage,
                String,
                new SimpleTypeConverter(true, new SimpleTypeConverter.ConversionMethod() {
                    @Override
                    Object doConvert(Class<?> type, Exchange exchange, Object value) throws Exception {
                        new Handler3.MyMessage(msg: (String) value)

                    }
                })

        cc.addComponent("kafka", buildKafkaComponent())
        cc.addComponent("log", new LogComponent())
        cc.addComponent("bean", new BeanComponent())
        cc.addComponent("console", new ConsoleComponent())
        cc.addRoutes(new MyRouterBuilder())

        cc.start()
    }

    KafkaComponent buildKafkaComponent() {
        KafkaComponent component = new KafkaComponent()
        component.kafkaClientFactory = new DefaultKafkaClientFactory() {
            @Override
            Consumer getConsumer(Properties kafkaProps) {
                //TODO provide a host resolver by which the host name and ip address pair configurable
                return new KafkaConsumer(kafkaProps)

            }
        }
        component.configuration.brokers = brokers
        return component
    }
}

