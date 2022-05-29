package org.example

import org.apache.camel.Consumer
import org.apache.camel.Endpoint
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.Producer
import org.apache.camel.support.DefaultComponent
import org.apache.camel.support.DefaultConsumer
import org.apache.camel.support.DefaultEndpoint

class ConsoleComponent extends DefaultComponent {

    static class ConsoleConsumer extends DefaultConsumer implements Runnable {
        Thread readerThread
        ConsoleEndpoint endpoint

        ConsoleConsumer(ConsoleEndpoint endpoint, Processor processor) {
            super(endpoint, processor)
            this.endpoint = endpoint
        }

        @Override
        protected void doStart() throws Exception {
            super.doStart()
            this.readerThread = new Thread(this)
            this.readerThread.start()


        }

        @Override
        void run() {
            try {
                runInternal()
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    System.out.println("reader thread interrupted.")
                } else {
                    throw new RuntimeException("Unknown exception got.", e)
                }
            } finally {
                System.out.println("reader thread exit!")
            }
        }

        private void runInternal() throws Exception {
            while (true) {
                LineNumberReader reader = new LineNumberReader(new InputStreamReader(System.in))
                System.out.print(">")
                String line = reader.readLine()
                if (line == null) {
                    System.out.println("line is null?")
                    break
                }

                if (line == "q" || line == "quit") {
                    break
                }

                doProcess(line)
            }

        }

        private void doProcess(String line) {

            final Exchange exchange = createExchange true
            exchange.in.body = line
            try {
                processor.process(exchange)
            } catch (Exception e) {
                exchange.setException(e)
            }

            // handle any thrown exception
            try {
                if (exchange.exception != null) {
                    exceptionHandler.handleException "Error processing exchange", exchange, exchange.exception
                }
            } finally {
                releaseExchange exchange, false
            }

        }

        @Override
        protected void doStop() throws Exception {
            readerThread.interrupt()
            super.doStop()
        }
    }

    static class ConsoleEndpoint extends DefaultEndpoint {
        String uri

        ConsoleEndpoint(String uri) {
            this.uri = uri
        }

        @Override
        protected String createEndpointUri() {
            return uri
        }

        @Override
        Producer createProducer() throws Exception {
            throw new RuntimeException("cannot producer to the console endpoint.")
        }

        @Override
        Consumer createConsumer(Processor processor) throws Exception {
            return new ConsoleConsumer(this, processor)
        }
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {

        return new ConsoleEndpoint(uri)
    }
}
