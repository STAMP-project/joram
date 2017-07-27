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

import javax.naming.*;

import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.RestDistributionQueue;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;

import framework.TestCase;

/**
 * Test: Test the behavior of BridgeDistributionQueue during stop / restart of bridge server.
 */
public class BridgeRestTest4 extends TestCase implements MessageListener {
  public static void main(String[] args) {
    new BridgeRestTest4().run();
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
        .setHostName("localhost")
        .setPort(8989)
        .setPeriod(500)
        .setIdleTimeout(10)
        .setBatch(true)
        .create(0, "distQueue", "foreignQueue");
    distQueue.setFreeWriting();
    System.out.println("joram distribution queue = " + distQueue);


    jndiCtx.bind("distQueue", distQueue);
    jndiCtx.rebind("bridgeCF", bridgeCF);
    jndiCtx.close();

    AdminModule.disconnect();

    System.out.println("admin config ok");
    Thread.sleep(1000);
  }

  int nbmsg = 10;
  Object lock = new Object();
  
  public void test() throws Exception {
    Context jndiCtx = new InitialContext();
    ConnectionFactory bridgeCF = (ConnectionFactory) jndiCtx.lookup("bridgeCF");
    Destination distQueue = (Destination) jndiCtx.lookup("distQueue");
    ConnectionFactory foreignCF = (ConnectionFactory) jndiCtx.lookup("foreignCF");
    Destination foreignQueue = (Destination) jndiCtx.lookup("foreignQueue");
    jndiCtx.close();

    Connection bridgeCnx = bridgeCF.createConnection();
    Session bridgeSess = bridgeCnx.createSession(false, Session.AUTO_ACKNOWLEDGE);
    MessageProducer bridgeProd = bridgeSess.createProducer(distQueue);
    bridgeCnx.start(); 

    Connection foreignCnx = foreignCF.createConnection();
    Session foreignSess = foreignCnx.createSession(false, Session.AUTO_ACKNOWLEDGE);
    MessageConsumer foreignCons = foreignSess.createConsumer(foreignQueue);
    foreignCons.setMessageListener(this);
    foreignCnx.start();
    
    TextMessage msg = foreignSess.createTextMessage();
    for (int i = 0; i < nbmsg; i++) {
      System.out.println("Send msg #" + i);
      msg.setText("Message number #" + i);
      bridgeProd.send(msg);
    }
    
    synchronized (lock) {
      if (counter != nbmsg)
        lock.wait((1000L*nbmsg) + 10000L);
    }
    assertTrue("Assertion#1", (counter == nbmsg));
    
    if (counter != nbmsg)
      throw new Exception("Bad message count");
    
    Thread.sleep(500L);
    System.out.println("Kill server#0");
    killAgentServer((short)0);
    Thread.sleep(1000);
    System.out.println("Start server#0");
    startAgentServer0();
    Thread.sleep(1000);
    System.out.println("Server#0 started");
    
    nbmsg = 20;

    bridgeCnx = bridgeCF.createConnection();
    bridgeSess = bridgeCnx.createSession(false, Session.AUTO_ACKNOWLEDGE);
    bridgeProd = bridgeSess.createProducer(distQueue);
    bridgeCnx.start(); 

    msg = foreignSess.createTextMessage();
    for (int i = 10; i < nbmsg; i++) {
      System.out.println("Send msg #" + i);
      msg.setText("Message number #" + i);
      bridgeProd.send(msg);
    }
    
    synchronized (lock) {
      System.out.println("wait for lock");
      if (counter != nbmsg)
        lock.wait((1000L*nbmsg) + 30000L);
    }
    assertTrue("Assertion#1", (counter == nbmsg));
    
    if (counter != nbmsg)
      throw new Exception("Bad message count");

  }
  
  int counter = 0;
  
  public void onMessage(Message msg) {
    try {
      String txt1 = "Message number #" + counter;
      String txt2 = ((TextMessage) msg).getText();
      if (! txt1.equals(txt2))
        System.out.println("Message " + msg.getJMSMessageID() + ": Expected <" + txt1 + "> but was <" + txt2 + "> ");
      assertEquals("Message " + msg.getJMSMessageID(), txt1, txt2);
      
      System.out.println("Receives " + msg.getJMSMessageID() + " -> " + txt2);
      
      counter += 1;
      if (counter == nbmsg) {
        synchronized (lock) {
          System.out.println("notify");
          lock.notify(); 
        }
      }
    } catch (JMSException exc) {
      error(exc);
      exc.printStackTrace();
    }
  }
}

