/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2017 ScalAgent Distributed Technologies
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA.
 *
 * Initial developer(s): ScalAgent D.T.
 * Contributor(s):
 */
package joram.client;

import javax.jms.Session;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.admin.AdminModule;

import framework.TestCase;

/**
 * After closing a connection with an active MessageConsumer the queue shall not
 * try to deliver the next message to this consumer (JORAM-281).
 * In this test the first message is delivered to the closed consumer then denied,
 * so the 2 messages are delivered in a bad order.
 */
public class ClientTest31 extends TestCase {

  public static void main(String[] args) {
    new ClientTest31().run();
  }

  Queue queue = null;

  public void run() {
    try {
      startAgentServer((short) 0, new String[] { "-DTransaction=fr.dyade.aaa.util.NullTransaction" });
      Thread.sleep(1000);

      AdminModule.connect("localhost", 2560, "root", "root", 60);

      org.objectweb.joram.client.jms.admin.User user = org.objectweb.joram.client.jms.admin.User.create("anonymous",
          "anonymous", 0);

      queue = Queue.create(0, "test_queue");
      queue.setFreeReading();
      queue.setFreeWriting();

      ConnectionFactory cf = org.objectweb.joram.client.jms.tcp.TcpConnectionFactory.create("localhost", 2560);

      testDelivered(queue, cf);

      Thread.sleep(1000);
    } catch (Throwable exc) {
      exc.printStackTrace();
      error(exc);
    } finally {
      stopAgentServer((short) 0);
      endTest();
    }
  }

  public void testDelivered(final Destination dest, ConnectionFactory cf) throws Exception {
    try (Connection connection = cf.createConnection()) {
      connection.start();
      
      new Thread(() -> {
        try {
          Session session = connection.createSession();
          MessageConsumer consumer = session.createConsumer(queue);
          System.out.println("Cons1, waiting..");
          TextMessage jmsMessage = (TextMessage) consumer.receive();
          consumer.close();
          System.out.println("Cons1, Message received: " + jmsMessage);
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
      }).start();
      Thread.sleep(50L);
    }
    
    // Without the fix, the request from cons1 is still active in the queue.
    
    try (Connection connection = cf.createConnection()) {
      connection.start();

      new Thread(() -> {
        try {
          Session session1 = connection.createSession();
          MessageConsumer consumer = session1.createConsumer(queue);
          System.out.println("Cons2, waiting..");
          TextMessage jmsMessage = (TextMessage) consumer.receive();
          consumer.close();
          System.out.println("Cons2, Message received: " + jmsMessage.getText());
          assertTrue("Delivered message should be 0", jmsMessage.getText().equals("MESSAGE1"));
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
      }).start();

      Thread.sleep(1000L);
      
      Session session2 = connection.createSession(true, Session.SESSION_TRANSACTED);
      MessageProducer producer = session2.createProducer(queue);
      producer.send(session2.createTextMessage("MESSAGE1"));
      producer.send(session2.createTextMessage("MESSAGE2"));
      session2.commit();
      System.out.println("Messages sent");
      
      System.out.println(queue.getStatistics());
    }

    assertTrue("Delivered message should be 0", queue.getDeliveredMessages() == 1);
  }
}
