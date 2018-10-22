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
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.objectweb.joram.client.jms.Topic;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;
import org.objectweb.joram.shared.security.SimpleIdentity;

import framework.TestCase;

/**
 * Test : The message received by the consumer rollback, recover or throw an exception.
 * The delivery delay is set on server. The message must re-delivered
 * after the delivery delay.
 */
public class RedeliveryDelay_T extends TestCase implements javax.jms.MessageListener {

  public static void main(String[] args) {
    new RedeliveryDelay_T().run(Integer.parseInt(args[0]), args[1]);
  }

  JMSContext context;
  JMSConsumer consumer;

  public void run(int sessionMode, String mode) {
    try {
      startAgentServer((short) 0);
      Thread.sleep(1000);

      admin(mode);
      test(sessionMode);

      Thread.sleep(5000);
      stopAgentServer((short) 0);
      Thread.sleep(1000);
      startAgentServer((short) 0);
      Thread.sleep(1000);

      System.out.println("Server restarted.");
      test(sessionMode);
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

  void admin(String mode) throws Exception {
    cf = TcpConnectionFactory.create("localhost", 2560);
    AdminModule.connect(cf, "root", "root");
    if ("D".equals(mode)) { // "D" -> Default
      System.out.println("Default redelivery delay set.");
      User.create("anonymous", "anonymous", 0);
    } else if ("A".equals(mode)) { // "A" -> Use administration API
      System.out.println("Configure User redelivery delay.");
      User user = User.create("anonymous", "anonymous");
      user.setRedeliveryDelay(5);
    } else {  // "C" -> set at creation time
      System.out.println("Configure User redelivery delay.");
      Properties prop = new Properties();
      prop.setProperty(User.REDELIVERY_DELAY, "5");
      User.create("anonymous", "anonymous", 0, SimpleIdentity.class.getName(), prop);
    }
    dest = Topic.create("topic");
    dest.setFreeReading();
    dest.setFreeWriting();
    AdminModule.disconnect();
  }

  void test(int sessionMode) throws InterruptedException {
    reset();
    switch (sessionMode) {
    case JMSContext.SESSION_TRANSACTED:
      context = cf.createContext(JMSContext.SESSION_TRANSACTED);
      break;
    case JMSContext.CLIENT_ACKNOWLEDGE:
      context = cf.createContext(JMSContext.CLIENT_ACKNOWLEDGE);
      break;
    default:
      context = cf.createContext(JMSContext.AUTO_ACKNOWLEDGE);
      break;
    }
    consumer = context.createConsumer(dest);
    consumer.setMessageListener(this);
    context.start();

    JMSContext prodCtx = cf.createContext();
    JMSProducer producer = prodCtx.createProducer();
    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    TextMessage msg = prodCtx.createTextMessage("test redeliveryTime");
    producer.send(dest, msg);

    // Wait to receive the message.
    Thread.sleep(10000);

    assertTrue("The rollback or recover message not received after the redelivery delay", time >= 5000);

    context.close();
    prodCtx.close();
  }

  public void reset() {
    first = true;
    time = 0;
  }

  private boolean first = true;
  private long time = 0;

  public void onMessage(Message message) {
    try {
      System.out.println(message.getJMSMessageID() + ", JMSRedelivered = " + message.getJMSRedelivered());
      System.out.println(System.currentTimeMillis() + ": message received deliveryTime = " + message.getJMSDeliveryTime());
    } catch (JMSException e) {
      e.printStackTrace();
    }

    time = System.currentTimeMillis() - time;
    System.out.println("Waiting (" + first + ") " + time);

    if (first) {
      first = false;
      switch (context.getSessionMode()) {
      case JMSContext.SESSION_TRANSACTED:
        System.out.println("rollback");
        context.rollback();
        break;
      case JMSContext.CLIENT_ACKNOWLEDGE:
        System.out.println("recover");
        context.recover();
        break;
      default:
        System.out.println("throw RuntimeException");
        throw new RuntimeException("Test redeliveryTime");
      }
    } else {
      switch (context.getSessionMode()) {
      case JMSContext.SESSION_TRANSACTED:
        System.out.println("commit");
        context.commit();
        break;
      case JMSContext.CLIENT_ACKNOWLEDGE:
        System.out.println("acknowledge");
        context.acknowledge();
        break;
      default:
        System.out.println("nothing");
      }      
    }
  }

}
