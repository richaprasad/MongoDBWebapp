/**
 * 
 */
package com.journaldev.app.messaging.asb;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.Random;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;


/**
 * @author rprasad017
 * <p>Class to send message to Azure Service Bus using AMQP 1.0</p>
 */
public class MessageSender implements ExceptionListener {
	
	private ConnectionFactory cf;
	private Destination queue;
	private Connection connection;
    private Session sendSession;
    private MessageProducer sender;
    private volatile boolean doSend = true;
    
    private static Random randomGenerator = new Random();

	/**
	 * @throws IOException 
	 * @throws NamingException 
	 * @throws JMSException 
	 * @throws SecretKeyInitException 
	 * 
	 */
	public MessageSender() throws IOException, NamingException, JMSException {
		// Configure JNDI environment
    	ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    	InputStream input = classLoader.getResourceAsStream("servicebus.properties");
    	
    	Properties properties = new Properties();
    	properties.load(input);
        Context context = new InitialContext(properties);

        // Lookup ConnectionFactory and Queue
        cf = (ConnectionFactory) context.lookup("SBCF");
        queue = (Destination) context.lookup("QUEUE");
        
        initializeConnection();
        System.out.println("ASB sender is ready");
	}
	
	/**
	 * Initialize connection
	 * @return 
	 * @throws JMSException
	 */
	private boolean initializeConnection() throws JMSException {
		// Create Connection
		connection = cf.createConnection();
		connection.setExceptionListener(this);

		// Create sender-side Session and MessageProducer
		sendSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		sender = sendSession.createProducer(queue);

		return true;
	}

	/**
	 * Creates split zip files, encrypt them and sends to Azure Service Bus
	 * @param fileName
	 * @param msg
	 * @throws ZipCreationException
	 * @throws SplitZipCreationException
	 * @throws JMSException
	 * @throws EncryptException 
	 * @throws IOException 
	 */
	public boolean sendMessage(String msg) throws JMSException, IOException  {
		if(doSend && (msg.trim().length() > 0)) {
			// Create message
			TextMessage message = sendSession.createTextMessage();
			try {
				initMessage(message, msg);
				// Send message to queue
				sender.send(message);
				System.out.println("Sent message to ASB with JMSMessageID = " + message.getJMSMessageID());
				message = null;
				return true;
			}catch (Exception e) {
				System.err.println(e.getMessage());
				return false;
			}
		} else {
			return false;
		}
    }

	/**
	 * Initialize message body
	 * @param encryptedText
	 * @param message2
	 * @throws JMSException
	 * @throws UnsupportedEncodingException 
	 */
    private void initMessage(TextMessage message, String msg) 
    					throws JMSException {
		message.setText(msg);
		
		long randomMessageID = randomGenerator.nextLong() >>>1;
		message.setJMSMessageID("ID:" + randomMessageID);
	}

    /**
     * Close all active connections
     * @throws JMSException
     */
	public void close() throws JMSException {
        if(sender != null) {
        	sender.close();
        }
        if(sendSession != null) {
        	sendSession.close();
        }
    	if(connection!= null) {
    		connection.close();
    	}
    	connection = null;
    	sendSession = null;
    	sender = null;
    }
    
   	@Override
	public void onException(JMSException exception) {
		System.err.println("Error in sender connection, Retrying to connect...");
		/*if(exception instanceof MessageProducerException){
			System.err.println("MessageProducerException");
		}*/
		try {
			close();
		} catch (JMSException e) {
			// We will get an Exception anyway, since the connection to the server is
            // broken, but close() frees up resources associated with the connection
		}
		try {
			boolean setupOK = initializeConnection();
			if(setupOK) {
				doSend = true;
				System.out.println("Sender connection successful");
			}
		} catch (JMSException e) {
			System.err.println("Unable to re-connect");
			doSend = false;
		}
	}

}
