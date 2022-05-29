package org.example;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Handler3 {
    public static class MyMessage {
        String msg;

        public static MyMessage valueOf(String stringMessage) {
            MyMessage myMessage = new MyMessage();
            myMessage.msg = stringMessage;
            return myMessage;
        }
    }

    private static Logger LOG = LoggerFactory.getLogger(Handler3.class);

    public void handle(MyMessage msg) {
        LOG.info("h3:{}", msg);
    }
}
