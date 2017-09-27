/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C)  2017 ScalAgent Distributed Technologies
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
package joram.bridgerest;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.RestAcquisitionQueue;
import org.objectweb.joram.client.jms.admin.RestDistributionQueue;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;

import framework.TestCase;

/**
 * Test: Test the rest bridge behavior during stop / restart of bridge server (#0).
 *  - Architecture: producer --JMS--> [#0 DistributionQueue] --REST--> [#1 foreignQueue]
 *                  consumer <--JMS-- [#0 AcquisitionQueue]  <--REST-- [#1 foreignQueue]
 *  - Sends 1000 messages on a DistributionQueue
 *  - Stops the Joram bridge server, then restarts it.
 *  - Receives the messages through an AcquisitionQueue.
 * Note: This test can either use the administration API or XML script.
 */
public class BridgeRestTest6 extends TestCase implements MessageListener {
  public static void main(String[] args) {
    new BridgeRestTest6().run();
  }

  public void startAgentServer0() throws Exception {
    System.setProperty("felix.config.properties", "file:config0.properties");
    startAgentServer((short)0, new String[]{"-DTransaction.UseLockFile=false"});
  }

  public void startAgentServer1() throws Exception {
    System.setProperty("felix.config.properties", "file:config1.properties");
    startAgentServer((short)1, new String[]{"-DTransaction.UseLockFile=false"});
  }

  public void run() {
    try {
      System.out.println("servers start");
      startAgentServer0();
      startAgentServer1();
      Thread.sleep(1000);
      
      if (Boolean.getBoolean("useXML"))
        AdminModule.executeXMLAdmin("joramAdmin.xml");
      else
        admin();
      
      test();
    } catch (Throwable exc) {
      exc.printStackTrace();
      error(exc);
    } finally {
      System.out.println("Server stop ");
      killAgentServer((short)0);
      killAgentServer((short)1);
      endTest(); 
    }
  }

  
  private void admin() throws Exception {
    javax.jms.ConnectionFactory bridgeCF = TcpConnectionFactory.create("localhost", 16010);

    AdminModule.connect(bridgeCF, "root", "root");
    javax.naming.Context jndiCtx = new javax.naming.InitialContext();

    User.create("anonymous", "anonymous", 0);
    User.create("anonymous", "anonymous", 1);

    // create The foreign destination and connectionFactory on server 1
    Queue foreignQueue = Queue.create(1, "foreignQueue");
    foreignQueue.setFreeReading();
    foreignQueue.setFreeWriting();
    System.out.println("foreign queue = " + foreignQueue);

    javax.jms.ConnectionFactory foreignCF = TcpConnectionFactory.create("localhost", 16011);

    // bind foreign destination and connectionFactory
    jndiCtx.rebind("foreignQueue", foreignQueue);
    jndiCtx.rebind("foreignCF", foreignCF);

    // Create a REST acquisition queue on server.
    Queue distQueue = new RestDistributionQueue()
        .setHost("localhost")
        .setPort(8989)
        .setPeriod(1000)
        .setIdleTimeout(10)
        .setBatch(true)
        .create(0, "distQueue", "foreignQueue");
    distQueue.setFreeWriting();
    System.out.println("joram distribution queue = " + distQueue);

    // Create a REST acquisition queue on server.
    Queue acqQueue = new RestAcquisitionQueue()
        .setMediaTypeJson(true)
        .setTimeout(5000)
        .setIdleTimeout(10)
        .create(0, "acqQueue", "foreignQueue");
    acqQueue.setFreeReading();
    System.out.println("joram acquisition queue = " + acqQueue);

    jndiCtx.bind("acqQueue", acqQueue);
    jndiCtx.bind("distQueue", distQueue);
    jndiCtx.rebind("bridgeCF", bridgeCF);
    jndiCtx.close();

    AdminModule.disconnect();

    System.out.println("admin config ok");
    Thread.sleep(1000);
  }

//  public void test() throws Exception {
//      boolean ret = false;
//      for (int i=0; i<10; i++) {
//        ret = test(1000);
//        if (!ret && failureCount() != 0) break;
//      }
//
//      if (ret) {
//        try {
//          Thread.sleep(100);
//          killAgentServer((short) 0);
//          System.out.println("\nJoram server stopped.");
//          startAgentServer((short) 0, new String[]{"-DTransaction.UseLockFile=false"});
//          Thread.sleep(10000);
//          System.out.println("Joram server started.\n");
//        } catch (Exception e) {
//          e.printStackTrace();
//        }
//        //System.out.println("Joram server status: " + AgentServer.getStatusInfo());
//
//        test(1000);
//      }
//  }
  
  public static final int NB_MSG = 1000;
  public static final Object lock = new Object();
  
  void test() throws Exception {
    Context jndiCtx = new InitialContext();
    ConnectionFactory bridgeCF = (ConnectionFactory) jndiCtx.lookup("bridgeCF");
    Destination acqQueue = (Destination) jndiCtx.lookup("acqQueue");
    Destination distQueue = (Destination) jndiCtx.lookup("distQueue");
    jndiCtx.close();

    Connection cnx = bridgeCF.createConnection();

    Session session1 = cnx.createSession(false, Session.AUTO_ACKNOWLEDGE);
    MessageProducer producer = session1.createProducer(distQueue);

    Session session2 = cnx.createSession(false, Session.AUTO_ACKNOWLEDGE);
    MessageConsumer consumer = session2.createConsumer(acqQueue);
    consumer.setMessageListener(this);

    cnx.start(); 

    TextMessage msg = session1.createTextMessage();
    for (int i=0; i < NB_MSG; i++) {
//      System.out.println("Send msg #" + i);
      msg.setText("Message number #" + i);
      producer.send(msg);
    }
    System.out.println(NB_MSG + " messages sent.");

    Thread.sleep(3000); // Wait for many messages forwarded to foreign server
    
    cnx.stop();
    Thread.sleep(1000); // Needed to wait acknowledge from consumer listener
    
    killAgentServer((short) 0);
    System.out.println("Foreign server stopped: " + nbmsg);
    // Needed to wait the last consume from AcquisitionQueue failed
    // Needed to allow cleaning of remote contexts
    Thread.sleep(30000);
    startAgentServer0();
    System.out.println("Foreign server started: " + nbmsg);
    
    cnx = bridgeCF.createConnection();

    session2 = cnx.createSession(false, Session.AUTO_ACKNOWLEDGE);
    consumer = session2.createConsumer(acqQueue);
    consumer.setMessageListener(this);

    cnx.start(); 

    synchronized (lock) {
      //      if (msgs != nbmsg && failureCount() == 0)
      //        lock.wait((timeout*msgs) +100000L);
      //    }
      lock.wait(240000L);
    }
    System.out.println("Receives " + nbmsg + " messages.");
    assertEquals("Receives " + nbmsg + " messages, should be " + NB_MSG, NB_MSG, nbmsg);

    cnx.close();
  }
  
  int nbmsg = 0;
  
  String previous = null;
  String current = null;
  
  public void onMessage(Message msg) {
    try {
      String current = "Message number #" + nbmsg;
      String txt = ((TextMessage) msg).getText();
      
      System.out.println(txt);
      if (! current.equals(txt)) {
        // Verify if it is a duplicate due to the server's failure.
        if ((previous == null) || (! previous.equals(txt))) {
          System.out.println("Expected <" + current + "> but was <" + txt + "> ");
          assertEquals("Message " + msg.getJMSMessageID(), current, txt);
        } else {
          System.out.println("Duplicate message: " + msg.getJMSMessageID() + " -> " + txt);
          return;
        }
      }
      
      previous = current;
      if (nbmsg % 100 == 0)
        System.out.println("Received " + nbmsg + " messages.");
      
      nbmsg += 1;
      if (nbmsg == NB_MSG) {
        synchronized (lock) {
          System.out.println("notify");
          lock.notify(); 
        }
      }
    } catch (JMSException e) {
      assertTrue("Exception: " + e, false);
      e.printStackTrace();
    }
  }
}

