/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C)  2018 ScalAgent Distributed Technologies
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
 * Initial developer(s):
 * Contributor(s): 
 */
package joram.jms2;

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;

import framework.TestCase;

/**
 * Test the queue pause mechanism.
 */
public class QueuePause extends TestCase implements javax.jms.MessageListener {

  public static void main(String[] args) {
    new QueuePause().run();
  }
  
  JMSContext context;
  JMSConsumer consumer;
  
  public void run() {
    try {
      startAgentServer((short) 0);
      Thread.sleep(1000);
      
      admin();
      pause(true);
      test();
      pause(true);
      
      Thread.sleep(1000);
      stopAgentServer((short) 0);
      Thread.sleep(1000);
      startAgentServer((short) 0);
      Thread.sleep(1000);

      System.out.println("Server restarted.");
      test();
      
      assertTrue("Should receive 2 messages: " + nbrecv, (nbrecv == 2));
    } catch (Throwable exc) {
      exc.printStackTrace();
      error(exc);
    } finally {
      stopAgentServer((short) 0);
      endTest();     
    }
  }
  
  ConnectionFactory cf = null;
  Queue dest = null;
  
  void admin() throws Exception {
    cf = TcpConnectionFactory.create("localhost", 2560);
    AdminModule.connect(cf, "root", "root");   
    User.create("anonymous", "anonymous", 0);
    dest = Queue.create("queue");
    dest.setFreeReading();
    dest.setFreeWriting();
    AdminModule.disconnect();
  }

  void pause(boolean pause) throws Exception {
    cf = TcpConnectionFactory.create("localhost", 2560);
    AdminModule.connect(cf, "root", "root");   
    dest = Queue.create("queue");
    dest.setPause(pause);
    AdminModule.disconnect();
  }
  
  boolean received;

  void test() throws Exception {
    context = cf.createContext(JMSContext.AUTO_ACKNOWLEDGE);
    consumer = context.createConsumer(dest);
    consumer.setMessageListener(this);
    context.start();

    JMSContext prodCtx = cf.createContext();
    JMSProducer producer = prodCtx.createProducer();
    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    TextMessage msg = prodCtx.createTextMessage("test pause");
    received = false;
    producer.send(dest, msg);

    // Wait to receive the message.
    Thread.sleep(10000);

    assertFalse("The message is received before starting queue ", received);

    pause(false);
    
    // Wait to receive the message.
    Thread.sleep(5000);
    assertTrue("The message is not received after starting queue ", received);
    
    context.close();
    prodCtx.close();
  }
  
  int nbrecv = 0;

  public void onMessage(Message message) {
    try {
      nbrecv += 1;
      System.out.println("#" + nbrecv + " - " + message.getJMSMessageID() + ", JMSRedelivered = " + message.getJMSRedelivered() + 
                         ", JMSTimestamp = " + message.getJMSTimestamp());
      System.out.println(System.currentTimeMillis() + ": message received deliveryTime = " + message.getJMSDeliveryTime());
    } catch (JMSException e) {
      e.printStackTrace();
    }
    received = true;
  }

}
