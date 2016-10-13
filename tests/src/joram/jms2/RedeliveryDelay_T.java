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
import org.objectweb.joram.shared.admin.AdminCommandConstant;
import org.objectweb.joram.shared.security.SimpleIdentity;

import fr.dyade.aaa.agent.AgentServer;
import framework.TestCase;

/**
 * Test : The message received by the consumer rollback, recover or throw an exception.
 * The delivery delay is set on server. The message must re-delivered
 * after the delivery delay.
 */
public class RedeliveryDelay_T extends TestCase implements javax.jms.MessageListener {

  public static void main(String[] args) {
    new RedeliveryDelay_T().run(Integer.parseInt(args[0]));
  }
  
  JMSContext context;
  
  public void run(int sessionMode) {
    try {
      startAgentServer((short) 0);
      Thread.sleep(1000);

      ConnectionFactory cf = TcpConnectionFactory.create("localhost", 2560);
      AdminModule.connect(cf, "root", "root");
      if (AgentServer.getProperty(AdminCommandConstant.RE_DELIVERY_DELAY) != null) {
        User.create("anonymous", "anonymous", 0);
      } else {
        Properties prop = new Properties();
        prop.setProperty(AdminCommandConstant.RE_DELIVERY_DELAY, "3");
        User.create("anonymous", "anonymous", 0, SimpleIdentity.class.getName(), prop);
      }
      Topic dest = Topic.create("topic");
      dest.setFreeReading();
      dest.setFreeWriting();
      AdminModule.disconnect();

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
      JMSConsumer consumer = context.createConsumer(dest);
      consumer.setMessageListener(this);
      context.start();

      JMSContext prodCtx = cf.createContext();
      JMSProducer producer = prodCtx.createProducer();
      producer.setDeliveryMode(DeliveryMode.PERSISTENT);
      TextMessage msg = prodCtx.createTextMessage("test redeliveryTime");
      producer.send(dest, msg);

      // Wait to receive the message.
      Thread.sleep(6000);

      assertTrue("The rollback or recover message not received after the redelivery delay", time < 6000);
      
      context.close();
      prodCtx.close();
    } catch (Throwable exc) {
      exc.printStackTrace();
      error(exc);
    } finally {
      stopAgentServer((short) 0);
      endTest();     
    }
  }

  private boolean first = true;
  private long time = 0;
  
  public void onMessage(Message message) {
    try {
      System.out.println(System.currentTimeMillis() + ": message received deliveryTime = " + message.getJMSDeliveryTime());
      System.out.println("JMSRedelivered = " + message.getJMSRedelivered());
    } catch (JMSException e) {
      e.printStackTrace();
    }
    
    time = System.currentTimeMillis() - time;
    
    if (first) {
      time = System.currentTimeMillis();
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
    }
  }

}
