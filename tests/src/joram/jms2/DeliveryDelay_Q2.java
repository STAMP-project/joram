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
import javax.jms.JMSProducer;
import javax.jms.Message;

import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;

import framework.TestCase;

/**
 * Test the delivery delay: Verify that a delayed message is delivered after restart.
 */
public class DeliveryDelay_Q2 extends TestCase {

  public static void main(String[] args) {
    new DeliveryDelay_Q2().run();
  }
  
  JMSContext context;
  JMSConsumer consumer;
  
  public void run() {
    try {
      startAgentServer((short) 0);
      Thread.sleep(1000);
      
      admin();
      test();
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

  void test() throws Exception {
    JMSContext context = cf.createContext(JMSContext.AUTO_ACKNOWLEDGE);
    JMSProducer producer = context.createProducer();
    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    producer.setDeliveryDelay(10000L);
    Message msg = context.createTextMessage("test DeliveryDelay");

    producer.send(dest, msg);
    System.out.println("Message sent.");
    context.close();

    Thread.sleep(1000);
    stopAgentServer((short) 0);
    Thread.sleep(1000);
    startAgentServer((short) 0);
    Thread.sleep(1000);

    System.out.println("Server restarted.");

    context = cf.createContext(JMSContext.AUTO_ACKNOWLEDGE);
    consumer = context.createConsumer(dest);
    context.start();

    msg = consumer.receive(10000);
    System.out.println("Message received: " + msg);
    assertTrue("Don't receive scheduled message", (msg != null));

    context.close();
  }
}
