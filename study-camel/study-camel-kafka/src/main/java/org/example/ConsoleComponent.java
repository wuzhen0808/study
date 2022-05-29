package org.example;

import org.apache.camel.*;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.DefaultConsumer;
import org.apache.camel.support.DefaultEndpoint;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Map;

public class ConsoleComponent extends DefaultComponent {

    public static class ConsoleConsumer extends DefaultConsumer implements Runnable {
        Thread readerThread;
        ConsoleEndpoint endpoint;

        public ConsoleConsumer(ConsoleEndpoint endpoint, Processor processor) {
            super(endpoint, processor);
            this.endpoint = endpoint;
        }

        @Override
        protected void doStart() throws Exception {
            super.doStart();
            this.readerThread = new Thread(this);
            this.readerThread.start();
            ;

        }

        @Override
        public void run() {
            try {
                runInternal();
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    System.out.println("reader thread interrupted.");
                } else {
                    throw new RuntimeException("Unknown exception got.", e);
                }
            } finally {
                System.out.println("reader thread exit!");
            }
        }

        private void runInternal() throws Exception {
            while (true) {
                LineNumberReader reader = new LineNumberReader(new InputStreamReader(System.in));
                System.out.print(">");
                String line = reader.readLine();
                if (line == null) {
                    System.out.println("line is null?");
                    break;
                }

                if (line.equals("q") || line.equals("quit")) {
                    break;
                }

                doProcess(line);
            }

        }

        private void doProcess(String line) {

            final Exchange exchange = createExchange(true);
            exchange.getIn().setBody(line);
            try {
                getProcessor().process(exchange);
            } catch (Exception e) {
                exchange.setException(e);
            }

            // handle any thrown exception
            try {
                if (exchange.getException() != null) {
                    getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
                }
            } finally {
                releaseExchange(exchange, false);
            }

        }

        @Override
        protected void doStop() throws Exception {
            readerThread.interrupt();
            super.doStop();
        }
    }

    public static class ConsoleEndpoint extends DefaultEndpoint {
        String uri;

        public ConsoleEndpoint(String uri) {
            this.uri = uri;
        }

        @Override
        protected String createEndpointUri() {
            return uri;
        }

        @Override
        public Producer createProducer() throws Exception {
            throw new RuntimeException("cannot producer to the console endpoint.");
        }

        @Override
        public Consumer createConsumer(Processor processor) throws Exception {
            return new ConsoleConsumer(this, processor);
        }
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {

        return new ConsoleEndpoint(uri);
    }
}
