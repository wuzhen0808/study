package org.example

import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.component.bean.BeanComponent
import org.apache.camel.component.kafka.DefaultKafkaClientFactory
import org.apache.camel.component.kafka.KafkaComponent
import org.apache.camel.component.log.LogComponent
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.SimpleTypeConverter
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer

import java.text.ParseException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

abstract class App {
    static class AppOptions {
        String app
    }

    static String readOption(String[] instructs) {

        int selected;
        while (true) {
            println "Please select one of the options by input the number ahead each of them."

            for (int i = 0; i < instructs.length; i++) {
                println "${i + 1}: ${instructs[i].split(":")[1]}"
            }

            print ">"

            String option = new LineNumberReader(new InputStreamReader(System.in)).readLine()
            try {
                selected = Integer.parseInt(option);
            } catch (NumberFormatException e) {
                continue
            }
            selected -= 1;
            if (selected in 0..instructs.length) {
                break
            }
        }

        return instructs[selected].split(":")[0]

    }

    static AppOptions resolveInitParameters(CommandLine cmdLine) {
        AppOptions appOptions = new AppOptions();
        String interactive = cmdLine.getOptionValue("i", "y")
        if (interactive && (interactive == 'y' || interactive == 'Y')) {
            appOptions.app = readOption("1:Run with external(localhost port 9092)kafka instance.",
                    "2:Run with embedded kafka by using docker.")
        } else {
            throw new Exception("non-interactive not supported.")
        }

        return appOptions

    }

    static void main(String[] args) {
        Options options = new Options()
                .addOption("v", "verbose", false, "Verbose")
                .addOption("i", "interactive", false, "Interactive")

        var parser = new DefaultParser()

        CommandLine cmdLine;
        try {
            cmdLine = parser.parse options, args
        } catch (ParseException e) {
            e.printStackTrace()
            new HelpFormatter().printHelp "apache args...", options
            return;
        }

        AppOptions appOptions = resolveInitParameters(cmdLine)

        App app;
        switch (appOptions.app) {
            case "2":
                app = new EmbeddedKafkaApp(cmdLine: cmdLine)
                break
            case "1":

                app = new DefaultApp(cmdLine: cmdLine)
                break
            default:
                throw new Exception("no such app:${app}, cannot run!")
        }
        app.run()
    }

    CommandLine cmdLine

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
        Semaphore done = new Semaphore(0);
        ConsoleComponent consoleComponent = new ConsoleComponent({
            stop(cc)
            done.release()
        })

        cc.addComponent("console", consoleComponent)

        cc.addRoutes(new MyRouterBuilder())
        start(cc)

        boolean isDone = false
        while (!isDone) {
            isDone = done.tryAcquire(10, TimeUnit.SECONDS)
        }
    }

    void start(CamelContext cc) {
        cc.start()
    }

    void stop(CamelContext cc) {
        cc.stop()
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

        return component
    }

}

