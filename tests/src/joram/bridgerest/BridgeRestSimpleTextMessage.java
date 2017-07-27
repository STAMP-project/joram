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

import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.RestAcquisitionQueue;
import org.objectweb.joram.client.jms.admin.RestDistributionQueue;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;

import framework.TestCase;

/**
 * Test: Test the rest bridge TextMessage.
 *  - Sends on text message on foreign server.
 *  - Receives the message through a JMS AcquisitionQueue.
 */
public class BridgeRestSimpleTextMessage extends TestCase implements MessageListener {
  public static void main(String[] args) {
    new BridgeRestSimpleTextMessage().run();
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

      try{
        javax.jms.ConnectionFactory joramCF = TcpConnectionFactory.create("localhost", 16010);

        AdminModule.connect(joramCF, "root", "root");
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
      
        // Create a REST distribution queue on server 0
        Queue queueDist = new RestDistributionQueue()
            .setHostName("localhost")
            .setPort(8989)
            .setPeriod(500)
            .setBatch(true)
            .create(0, "queueDist", "foreignQueue");
        queueDist.setFreeWriting();
        System.out.println("joram distribution queue = " + queueDist);

        // Create a REST acquisition queue on server.
        Queue queueAcq = new RestAcquisitionQueue().setMediaTypeJson(false).create(0, "queueAcq", "foreignQueue");
        queueAcq.setFreeReading();
        System.out.println("joram acquisition queue = " + queueAcq);
        
        jndiCtx.bind("queueDist", queueDist);
        jndiCtx.bind("queueAcq", queueAcq);
        jndiCtx.rebind("joramCF", joramCF);
        jndiCtx.close();

        AdminModule.disconnect();
        System.out.println("Admin closed.");
      } catch(Exception exc){
        exc.printStackTrace();
      }

      System.out.println("admin config ok");
      Thread.sleep(1000);

      test(1);
     
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

  final static long timeout = 10L;
  private Object lock = new Object();
  private int msgs = 0;
  
  boolean test(final int msgs) throws Exception {
    System.out.println("test start..");
    this.msgs = msgs;
    Connection joramAcqCnx = null;
    Connection joramDistCnx = null;
    Session joramDistSess = null;
    MessageProducer joramDistProd = null;
    try {
      javax.naming.Context jndiCtx = new javax.naming.InitialContext();  
      ConnectionFactory joramCF = (ConnectionFactory) jndiCtx.lookup("joramCF");
      Destination queueDist = (Destination) jndiCtx.lookup("queueDist");
      Destination queueAcq = (Destination) jndiCtx.lookup("queueAcq");
      jndiCtx.close();

      joramAcqCnx = joramCF.createConnection();
      Session joramAcqSess = joramAcqCnx.createSession(false, Session.AUTO_ACKNOWLEDGE);
      MessageConsumer joramAcqCons = joramAcqSess.createConsumer(queueAcq);
      joramAcqCons.setMessageListener(this);
      joramAcqCnx.start(); 

      joramDistCnx = joramCF.createConnection();
      joramDistSess = joramDistCnx.createSession(false, Session.AUTO_ACKNOWLEDGE);
      joramDistProd = joramDistSess.createProducer(queueDist);
    } catch (Exception e) {
      e.printStackTrace();
    }

    nbmsg = 0;
    TextMessage msgOut = joramDistSess.createTextMessage();
    for (int i = 0; i < msgs; i++) {
      msgOut.setText("Message number " + i);
      System.out.println("send msg = " + msgOut.getText());
      joramDistProd.send(msgOut);
      //      Thread.sleep(timeout);
    }
    System.out.println(msgs + " messages sended.");
    joramDistCnx.close();

    synchronized (lock) {
      lock.wait((timeout*msgs) +5000L);
    }
    System.out.println("Receives " + nbmsg + " messages.");
    assertEquals("Receives " + nbmsg + "messages, should be " + msgs, msgs, nbmsg);

    joramAcqCnx.close();
    
    return (nbmsg == msgs);
  }
  
  int nbmsg = 0;
  public void onMessage(Message msg) {
    //System.out.println("receives: " + msg);
    try {
      Thread.sleep(timeout);
    } catch (InterruptedException e1) {}
    try {
      String txt1 = "Message number " + nbmsg;
      String txt2 = ((TextMessage) msg).getText();
      System.out.println("receive: " + txt2);
      if (! txt1.equals(txt2))
        System.out.println("Expected <" + txt1 + "> but was <" + txt2 + "> ");
      assertEquals("Message " + msg.getJMSMessageID(), txt1, txt2);
      
    } catch (JMSException e) {
      assertTrue("Exception: " + e, false);
      e.printStackTrace();
    }
    nbmsg += 1;
    if (nbmsg == msgs) {
      synchronized (lock) {
        lock.notify(); 
      }
    }
  }
}

