package com.example;

import javax.jms.*;
import org.apache.qpid.jms.JmsConnectionFactory;

public class AmqpJmsClient {

    // Define connection details
    private static final String AMQP_URL = "amqps://mq1.usec-hostingtd-quintiq.com:443"; 
    private static final String USERNAME = "COPA_TEST_quser";
    private static final String PASSWORD = "";
    private static final String QUEUE_NAME = "COPA_TEST.Queue1"; // Change as needed

    public static void main(String[] args) {

        System.out.println("JMS Client Started...");

        Connection connection = null;
        try {
            // Create a connection factory
            JmsConnectionFactory factory = new JmsConnectionFactory(USERNAME, PASSWORD, AMQP_URL);

            // Create a JMS connection
            connection = factory.createConnection();
            connection.start();  // Start the connection

            // Create a session (Auto-Acknowledge mode)
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create a queue (you can also use a topic)
            Destination destination = session.createQueue(QUEUE_NAME);

            // Create a producer
            MessageProducer producer = session.createProducer(destination);
            TextMessage message = session.createTextMessage("Hello from JMS with AMQP!");
            producer.send(message);
            System.out.println("Message Sent: " + message.getText());

            // Create a consumer
            MessageConsumer consumer = session.createConsumer(destination);
            Message receivedMessage = consumer.receive(5000);  // Wait up to 5 seconds

            if (receivedMessage instanceof TextMessage) {
                System.out.println("Message Received: " + ((TextMessage) receivedMessage).getText());
            } else {
                System.out.println("Received non-text message.");
            }

            // Close resources
            producer.close();
            consumer.close();
            session.close();

        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }
}
