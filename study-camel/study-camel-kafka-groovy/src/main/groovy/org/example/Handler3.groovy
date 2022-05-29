package org.example

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Handler3 {
    static class MyMessage {
        String msg

    }

    private static Logger LOG = LoggerFactory.getLogger(Handler3)

    void handle(MyMessage msg) {
        LOG.info "h3:{}", msg
    }
}
