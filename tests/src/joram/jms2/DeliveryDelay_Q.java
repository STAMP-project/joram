/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C)  2016 - 2019 ScalAgent Distributed Technologies
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
 * Test the delivery delay.
 */
public class DeliveryDelay_Q extends TestCase implements javax.jms.MessageListener {

  public static void main(String[] args) {
    new DeliveryDelay_Q().run();
  }
  
  JMSContext context;
  JMSConsumer consumer;
  
  public void run() {
    try {
      startAgentServer((short) 0);
      Thread.sleep(1000);
      
      admin();
      test();
      
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

  long send = 0L;
  long recv = 0L;

  void test() throws InterruptedException {
    context = cf.createContext(JMSContext.AUTO_ACKNOWLEDGE);
    consumer = context.createConsumer(dest);
    consumer.setMessageListener(this);
    context.start();

    JMSContext prodCtx = cf.createContext();
    JMSProducer producer = prodCtx.createProducer();
    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    producer.setDeliveryDelay(5000L);
    TextMessage msg = prodCtx.createTextMessage("test redeliveryTime");
    send = System.currentTimeMillis();
    producer.send(dest, msg);

    // Wait to receive the message.
    Thread.sleep(10000);

    assertTrue("The message is received before the delivery delay", (recv - send) > 5000);

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
    recv = System.currentTimeMillis();
  }

}
