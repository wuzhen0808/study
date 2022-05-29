package org.example;

import org.apache.camel.builder.RouteBuilder;

public class MyRouterBuilder extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        from("console:in")
                .to("log:console-in-events?showAll=true&multiline=true")
                .to("kafka:quickstart-events")
        ;

        from("kafka:quickstart-events")
                .to("log:quickstart-events?showAll=true&multiline=true")
                .to("bean:h1")
                .to("bean:h2")
                .to("bean:h3")
        ;


    }
}
