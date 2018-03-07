/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C)  2012 ScalAgent Distributed Technologies
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
 * Initial developer(s):Badolle Fabien (ScalAgent D.T.)
 * Contributor(s): 
 */
package joram.bridgejms;

import java.util.Properties;

import javax.jms.Connection;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.JMSAcquisitionQueue;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;
import org.objectweb.joram.mom.dest.jms.JMSAcquisition;

import framework.TestCase;

/**
 * Test: Tests a loopback acquisition bridge.
 */
public class BridgeTest23x extends TestCase {
  public static void main(String[] args) {
    new BridgeTest23x().run();
  }

  public void run() {
    try {
      System.out.println("servers start");
      startAgentServer((short)0, new String[]{"-DTransaction.UseLockFile=false"});
      Thread.sleep(1000);

      try{
        javax.jms.ConnectionFactory joramCF = TcpConnectionFactory.create("localhost", 16010);

        AdminModule.connect(joramCF, "root", "root");

        User.create("anonymous", "anonymous", 0);

        // create The foreign destination and connectionFactory
        Queue foreignQueue = Queue.create(0, "foreignQueue");
        foreignQueue.setFreeReading();
        foreignQueue.setFreeWriting();

        javax.jms.ConnectionFactory foreignCF = TcpConnectionFactory.create("localhost", 16010);

        // bind foreign destination and connectionFactory
        javax.naming.Context jndiCtx = new javax.naming.InitialContext();
        jndiCtx.rebind("foreignQueue", foreignQueue);
        jndiCtx.rebind("foreignCF", foreignCF);
        jndiCtx.close();
        
        AdminModule.addJMSBridgeConnection(0, "scn://localhost:16400/?name=cnx1&cf=foreignCF&jndiFactoryClass=fr.dyade.aaa.jndi2.client.NamingContextFactory");

        // Setting the bridge properties
        Properties prop = new Properties();
        prop.setProperty("jms.ConnectionUpdatePeriod", "1000");
        prop.setProperty("period", "1000");
        prop.setProperty("jms.Routing", "cnx1");
        Queue joramInQueue = JMSAcquisitionQueue.create(0, "BridgeInQueue", "foreignQueue", prop);
        joramInQueue.setFreeReading();

        System.out.println("admin done.");
        Thread.sleep(1000);
        
        Connection joramCnx = joramCF.createConnection();
        Session joramSess = joramCnx.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer joramCons = joramSess.createConsumer(joramInQueue);
        joramCnx.start();

        Connection foreignCnx = foreignCF.createConnection();
        Session foreignSess = foreignCnx.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer foreignProd = foreignSess.createProducer(foreignQueue);
        foreignCnx.start();
        
        TextMessage msgOut = joramSess.createTextMessage("Coucou1");
        foreignProd.send(msgOut);
        System.out.println("send a message = " + msgOut.getText());

        TextMessage msgIn = (TextMessage) joramCons.receive(5000);
        assertTrue("Should receive a message: " + msgIn, (msgIn != null));
        if (msgIn != null) {
          System.out.println("Receive message: " + msgIn.getText());
          assertTrue("Should receive a message: " + msgIn, "Coucou1".equals(msgIn.getText()));
        }
        
        AdminModule.deleteJMSPBridgeConnection(0, "cnx1", true);
        System.out.println("Bridge connection deleted");
        Thread.sleep(500L);
        AdminModule.stopServer();
        System.out.println("server stopped");
      } catch(Exception exc){
        exc.printStackTrace();
        error(exc);
      }
    } catch (Throwable exc) {
      exc.printStackTrace();
      error(exc);
    } finally {
      System.out.println("end test ");
      endTest(); 
    }
  }
}

