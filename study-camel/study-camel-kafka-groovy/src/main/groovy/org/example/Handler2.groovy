package org.example

import org.apache.camel.Exchange
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Handler2 {
    private static Logger LOG = LoggerFactory.getLogger(Handler2)

    void handle(Exchange exchange) {
        LOG.info "handle:{}", exchange.in.body
    }
}
