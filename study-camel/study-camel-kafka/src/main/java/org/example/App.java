package org.example;

import org.apache.camel.CamelContext;
import org.apache.camel.component.bean.BeanComponent;
import org.apache.camel.component.kafka.DefaultKafkaClientFactory;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.log.LogComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.SimpleTypeConverter;
import org.apache.kafka.clients.consumer.Consumer;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Properties;

/**
 * Hello world!
 */
public class App {

    public static void main(String[] args) throws Exception {
        new App().run();
    }

    private String brokers = "172.18.226.42:9092";


    private void run() throws Exception {

        CamelContext cc = new DefaultCamelContext();
        runInternal(cc);

    }

    private void log(String msg) {
        System.out.println(msg);
    }

    private void runInternal(CamelContext cc) throws Exception {
        log("running.");

        cc.getRegistry().bind("h1", new Handler1());
        cc.getRegistry().bind("h2", new Handler2());
        cc.getRegistry().bind("h3", new Handler3());

        cc.getTypeConverterRegistry().addTypeConverter(
                Handler3.MyMessage.class,
                String.class,
                new SimpleTypeConverter(true, (type, exchange, value) -> Handler3.MyMessage.valueOf((String) value)));

        cc.addComponent("kafka", buildKafkaComponent());
        cc.addComponent("log", new LogComponent());
        cc.addComponent("bean", new BeanComponent());
        cc.addComponent("console", new ConsoleComponent());
        cc.addRoutes(new MyRouterBuilder());

        cc.start();
    }

    KafkaComponent buildKafkaComponent() {
        KafkaComponent component = new KafkaComponent();
        component.setKafkaClientFactory(new DefaultKafkaClientFactory() {
            @Override
            public Consumer getConsumer(Properties kafkaProps) {
                //TODO provide a host resolver by which the host name and ip address pair configurable
                return new org.apache.kafka.clients.consumer.KafkaConsumer(kafkaProps);

            }
        });
        component.getConfiguration().setBrokers(brokers);
        return component;
    }
}
