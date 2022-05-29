package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Handler1 {
    private static final Logger LOG = LoggerFactory.getLogger(Handler1.class);

    public void handle(String msg) {
        LOG.info("h1,msg received:{}", msg);
    }

}
