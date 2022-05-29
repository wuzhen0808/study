package org.example

import org.apache.camel.builder.RouteBuilder

class MyRouterBuilder extends RouteBuilder {
    @Override
    void configure() throws Exception {

        from("console:in")
                .to("log:console-in-events?showAll=true&multiline=true")
                .to("kafka:quickstart-events")

        from("kafka:quickstart-events")
                .to("log:quickstart-events?showAll=true&multiline=true")
                .to("bean:h1?method=handle")
                .to("bean:h2?method=handle")
                .to("bean:h3?method=handle")

    }
}
