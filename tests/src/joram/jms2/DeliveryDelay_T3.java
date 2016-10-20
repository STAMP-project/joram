/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C)  2016 ScalAgent Distributed Technologies
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

import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Message;

import org.objectweb.joram.client.jms.Topic;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;
import org.objectweb.joram.shared.security.SimpleIdentity;

import framework.TestCase;

/**
 * Test the delivery delay and redelivery
 */
public class DeliveryDelay_T3 extends TestCase {

  public static void main(String[] args) {
    new DeliveryDelay_T3().run();
  }
  
  JMSContext context;
  JMSConsumer consumer;
  JMSConsumer consumer1;
  
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
  Topic dest = null;
  
  void admin() throws Exception {
    cf = TcpConnectionFactory.create("localhost", 2560);
    AdminModule.connect(cf, "root", "root"); 
    
    System.out.println("Configure User redelivery delay.");
    Properties prop = new Properties();
    prop.setProperty(User.REDELIVERY_DELAY, "10");
    User.create("anonymous", "anonymous", 0, SimpleIdentity.class.getName(), prop);
    
    dest = Topic.create("topic");
    dest.setFreeReading();
    dest.setFreeWriting();
    AdminModule.disconnect();
  }

  long send = 0L;
  long recv = 0L;

  void test() throws Exception {
    JMSContext context = cf.createContext(JMSContext.AUTO_ACKNOWLEDGE);
    context.setClientID("client_dursub");
    consumer = context.createDurableConsumer(dest, "dursub");
    consumer.close();
    
    JMSContext ctxTransacted = cf.createContext(JMSContext.SESSION_TRANSACTED);
    ctxTransacted.setClientID("client_dursub1");
    consumer1 = ctxTransacted.createDurableConsumer(dest, "dursub1");
    consumer1.close();
    
    JMSProducer producer = context.createProducer();
    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    producer.setDeliveryDelay(10000L);
    Message msg = context.createTextMessage("test redeliveryTime");
    send = System.currentTimeMillis();
    producer.send(dest, msg);
    System.out.println("Message sent.");
    context.close();
    ctxTransacted.close();

    // 1 stop and restart
    Thread.sleep(1000);
    stopAgentServer((short) 0);
    Thread.sleep(1000);
    startAgentServer((short) 0);
    Thread.sleep(1000);

    System.out.println("1: Server restarted.");

    context = cf.createContext(JMSContext.AUTO_ACKNOWLEDGE);
    context.setClientID("client_dursub");
    consumer = context.createDurableConsumer(dest, "dursub");
    context.start();
    
    ctxTransacted = cf.createContext(JMSContext.SESSION_TRANSACTED);
    ctxTransacted.setClientID("client_dursub1");
    consumer1 = ctxTransacted.createDurableConsumer(dest, "dursub1");
    ctxTransacted.start();
    
    msg = consumer.receive(12000);
    System.out.println("durSub: Message received: " + msg);
    assertTrue("Don't receive scheduled message to durSub", (msg != null));
    
    // rollback the transacted context (dursub1)
    msg = consumer1.receive(12000);
    System.out.println("durSub1: Message received: " + msg);
    assertTrue("Don't receive scheduled message to durSub1", (msg != null));
    ctxTransacted.rollback();
    
    context.close();
    ctxTransacted.close();

    // 2 stop and restart
    Thread.sleep(1000);
    stopAgentServer((short) 0);
    Thread.sleep(1000);
    startAgentServer((short) 0);
    Thread.sleep(1000);

    System.out.println("2: Server restarted.");

    context = cf.createContext(JMSContext.AUTO_ACKNOWLEDGE);
    context.setClientID("client_dursub");
    consumer = context.createDurableConsumer(dest, "dursub");
    context.start();
    
    ctxTransacted = cf.createContext(JMSContext.SESSION_TRANSACTED);
    ctxTransacted.setClientID("client_dursub1");
    consumer1 = ctxTransacted.createDurableConsumer(dest, "dursub1");
    ctxTransacted.start();
    
    msg = consumer1.receive(12000);
    System.out.println("durSub1: Message received: " + msg);
    assertTrue("Don't receive scheduled message to durSub1", (msg != null));
    ctxTransacted.commit();

    msg = consumer.receive(12000);
    System.out.println("durSub: (attempt msg=null) Message received: " + msg); 
    assertTrue("Receive scheduled message to durSub (attempt msg=null)", (msg == null));
    context.close();
    ctxTransacted.close();
  }
}
