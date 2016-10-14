/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2015 ScalAgent Distributed Technologies
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
package joram.bridgejms;

import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.JMSAcquisitionQueue;
import org.objectweb.joram.client.jms.admin.JMSDistributionQueue;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;

import framework.TestCase;

/**
 *  Test the JMS bridge with a specific architecture, the central server use 2 JMSModule to
 * communicate with remote servers using each a JNDI server.
 */
public class BridgeTest20x extends TestCase {
  public static void main(String[] args) {
    new BridgeTest20x().run();
  }

  static final int ROUND = 100;
  static final int bigsize = 1000000;
  
  public void run() {
    try {
      System.out.println("servers start");
      startAgentServer((short)0, new String[]{"-DTransaction.UseLockFile=false"});
      startAgentServer((short)1, new String[]{"-DTransaction.UseLockFile=false"});
      startAgentServer((short)2, new String[]{"-DTransaction.UseLockFile=false"});
      Thread.sleep(5000);

      javax.jms.ConnectionFactory cf = TcpConnectionFactory.create("localhost", 16010);
      centralAdmin(cf, "scn://localhost:16400/");

      javax.jms.ConnectionFactory cf1 = TcpConnectionFactory.create("localhost", 16011);
      localAdmin(cf1, "scn://localhost:16401/");

      javax.jms.ConnectionFactory cf2 = TcpConnectionFactory.create("localhost", 16012);
      localAdmin(cf2, "scn://localhost:16402/");

      Connection cnx = cf.createConnection();
      
      Sender20 sender1 = new Sender20("A", cnx, dq1);
      Receiver20 receiver1 = new Receiver20("A", cnx, aq1);
      
      Sender20 sender2 = new Sender20("B", cnx, dq2);
      Receiver20 receiver2 = new Receiver20("B", cnx, aq2);
      
      cnx.start();
      
      for (int i=0; i<ROUND; i++) {
        if ((i%1000) == 999) {
          sender1.sendbig();
          sender2.sendbig();
        } else {
          sender1.send("A" + i);
          sender2.send("B" + i);
        }
        
        Thread.sleep(5, 0);
      }
      
      Thread.sleep(10000L);     
      
      sender1.close(); receiver1.close();
      sender2.close(); receiver2.close();
      
      cnx.close();

      Thread.sleep(1000L);
    } catch (Throwable exc) {
      exc.printStackTrace();
      error(exc);
    } finally {
      System.out.println("Server stop ");
      killAgentServer((short)0);
      killAgentServer((short)1);
      killAgentServer((short)2);
      endTest(); 
    }
  }
  
  public void localAdmin(ConnectionFactory cf, String jndiUrl) throws Exception {
    Properties props = new Properties();
    props.setProperty(Context.PROVIDER_URL, jndiUrl);
    javax.naming.Context jndiCtx = new InitialContext(props);

    // Connects to the server
    AdminModule.connect(cf);
    int sid = AdminModule.getLocalServerId();

    jndiCtx.rebind("cf", cf);

    // Creates the local user
    User.create("anonymous", "anonymous");
    
    Queue queue = org.objectweb.joram.client.jms.Queue.create("queue");
    ((org.objectweb.joram.client.jms.Queue) queue).setFreeReading();
    ((org.objectweb.joram.client.jms.Queue) queue).setFreeWriting();
    jndiCtx.rebind("queue", queue);
    
    System.out.println("creates queue (S#" + sid + ") = " + queue);
    
    AdminModule.disconnect();
  }

  Queue aq1, dq1, aq2, dq2;
  
  public void centralAdmin(ConnectionFactory cf, String jndiUrl) throws Exception {
    Properties jndiProps = new Properties();
    jndiProps.setProperty(Context.PROVIDER_URL, jndiUrl);
    javax.naming.Context jndiCtx = new InitialContext(jndiProps);

    // Connects to the server
    AdminModule.connect(cf);
    
    jndiCtx.rebind("centralCF", cf);

    // Creates the local user
    User.create("anonymous", "anonymous");
    
    // Creates the distribution queue to S#1
    Properties props = new Properties();
    props.setProperty("period", "1000");     
    props.setProperty("jms.ConnectionUpdatePeriod", "1000");
    props.setProperty("distribution.async", "true");
    props.setProperty("jms.Routing", "cnx1");
    dq1 = JMSDistributionQueue.create(0, "dq1", "queue", props);
    ((org.objectweb.joram.client.jms.Queue) dq1).setFreeWriting();
    jndiCtx.rebind("dq1", dq1);
    
    // Creates the acquisition queue from S#1
    props.clear();
    props.setProperty("jms.ConnectionUpdatePeriod", "1000");
    props.setProperty("persistent", "true");
    props.setProperty("acquisition.max_msg", "50");
    props.setProperty("acquisition.min_msg", "20");
    props.setProperty("acquisition.max_pnd", "200");
    props.setProperty("acquisition.min_pnd", "50");
    props.setProperty("jms.Routing", "cnx1");
    aq1 = JMSAcquisitionQueue.create(0, "aq1", "queue", props);
    ((org.objectweb.joram.client.jms.Queue) aq1).setFreeReading();
    jndiCtx.rebind("aq1", aq1);
    
    // Creates the distribution queue to S#2
    props.clear();
    props.setProperty("period", "1000");     
    props.setProperty("jms.ConnectionUpdatePeriod", "1000");
    props.setProperty("distribution.async", "true");
    props.setProperty("jms.Routing", "cnx2");
    dq2 = JMSDistributionQueue.create(0, "dq2", "queue", props);
    ((org.objectweb.joram.client.jms.Queue) dq2).setFreeWriting();
    jndiCtx.rebind("dq2", dq2);
    
    // Creates the acquisition queue from S#2
    props.clear();
    props.setProperty("jms.ConnectionUpdatePeriod", "1000");
    props.setProperty("persistent", "true");
    props.setProperty("acquisition.max_msg", "50");
    props.setProperty("acquisition.min_msg", "20");
    props.setProperty("acquisition.max_pnd", "200");
    props.setProperty("acquisition.min_pnd", "50");
    props.setProperty("jms.Routing", "cnx2");
    aq2 = JMSAcquisitionQueue.create(0, "aq2", "queue", props);
    ((org.objectweb.joram.client.jms.Queue) aq2).setFreeReading();
    jndiCtx.rebind("aq2", aq2);

    jndiCtx.close();
    AdminModule.disconnect();
  }
}

class Sender20 {
  String name = null;
  Connection cnx = null;
  Session session = null;
  MessageProducer prod = null;
  Queue queue;
  
  Sender20(String name, Connection cnx, Queue queue) throws JMSException {
    this.name = name;
    this.cnx = cnx;
    this.queue = queue;
    session = cnx.createSession(false, Session.AUTO_ACKNOWLEDGE);

    prod = session.createProducer(queue);
  }
  
  void send(String text) throws JMSException {
    TextMessage msg = session.createTextMessage(text);
    prod.send(msg);
  }
  
  void sendbig() throws JMSException {
	  byte[] payload = new byte[BridgeTest20x.bigsize];
	  ObjectMessage msg = session.createObjectMessage();
	  msg.setObject(payload);
	  prod.send(msg);
  }
  
  public void close() throws JMSException {
    session.close();
  }
}

class Receiver20 implements MessageListener {
  String name = null;
  Connection cnx = null;
  Session session = null;
  MessageConsumer cons = null;
  Queue queue;

  int cpt = 0;
  
  Receiver20(String name, Connection cnx, Queue queue) throws JMSException {
    this.name = name;
    this.cnx = cnx;
    this.queue = queue;
    session = cnx.createSession(false, Session.AUTO_ACKNOWLEDGE);

    cons = session.createConsumer(queue);
    cons.setMessageListener(this);
  }

  @Override
  public void onMessage(Message m) {
  	try {
  		if (m instanceof TextMessage) {
  			TextMessage msg = (TextMessage) m;
  			String str = msg.getText();
  			TestCase.assertTrue("Should receive #" + cpt, str.equals(name + cpt));
  			if (! str.equals(name + cpt)) {
  				System.out.println(name + cpt + " receives " + str);
  				cpt = Integer.parseInt(str.substring(name.length()));
  			}
  		} else {
  			ObjectMessage msg = (ObjectMessage) m;
  			byte[] payload = (byte[]) msg.getObject();
  			if (payload.length != BridgeTest20x.bigsize)
  				System.out.println(name + cpt + " receives BigObject: " + payload.length);
  			TestCase.assertTrue("Should receive - " + payload.length, (payload.length == BridgeTest20x.bigsize));
  		}
  		cpt++;
  		if ((cpt%10)==0) System.out.println(name + cpt);
  	} catch (Exception e) {
  		e.printStackTrace();
  	}
  }
  
  public void close() throws JMSException {
    TestCase.assertTrue("Received " + cpt + " should be " + BridgeTest20x.ROUND, cpt == BridgeTest20x.ROUND);
    session.close();
  }
}
