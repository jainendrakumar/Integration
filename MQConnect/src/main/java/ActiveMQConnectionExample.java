import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class ActiveMQConnectionExample {
    //private static final String BROKER_URI = "amqps://mq1.usec-hostingtd-quintiq.com:443";
    private static final String BROKER_URI = "http://mq1.usec-hostingtd-quintiq.com:443";
    private static final String USERNAME = "COPA_TEST_quser";
    private static final String PASSWORD = "1n7lj5Z6PiGu2L0v2066jWlj"; // Replace with your password
    private static final String QUEUE_NAME = "COPA_TEST.Queue1";

    public static void main(String[] args) {
        // Create a ConnectionFactory
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(USERNAME, PASSWORD, BROKER_URI);

        Connection connection = null;

        try {
            // Establish a connection
            connection = connectionFactory.createConnection();
            connection.start();
            //connectionFactory.setTrustAllPackages(true); // If using serialized objects


            // Create a session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create the destination (queue)
            Destination destination = session.createQueue(QUEUE_NAME);

            // Create a producer
            MessageProducer producer = session.createProducer(destination);

            // Create and send a message
            String text = "Hello, ActiveMQ!";
            TextMessage message = session.createTextMessage(text);
            producer.send(message);
            System.out.println("Sent message: " + text);

            // Create a consumer to receive the message
            MessageConsumer consumer = session.createConsumer(destination);
            Message receivedMessage = consumer.receive(1000); // Wait up to 1 second for a message

            if (receivedMessage instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) receivedMessage;
                System.out.println("Received message: " + textMessage.getText());
            } else {
                System.out.println("No message received.");
            }

            // Clean up
            producer.close();
            consumer.close();
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
