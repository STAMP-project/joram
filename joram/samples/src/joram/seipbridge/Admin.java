/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2018 ScalAgent Distributed Technologies
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
 * Initial developer(s): ScalAgent Distributed Technologies
 * Contributor(s): 
 */
package seipbridge;

import java.util.Properties;

import javax.jms.ConnectionFactory;

import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.Topic;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.JMSAcquisitionQueue;
import org.objectweb.joram.client.jms.admin.JMSDistributionQueue;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;

public class Admin {
  public static void main(String[] args) throws Exception {
    centralAdmin();
    bridgeAdmin();
  }
  
  static void centralAdmin() throws Exception {
    System.out.println();
    System.out.println("Central administration...");

    ConnectionFactory cf = TcpConnectionFactory.create("localhost", 16010);
    AdminModule.connect(cf, "root", "root");
    
    User.create("anonymous", "anonymous");

    Queue queue = Queue.create("mqs_dest");
    queue.setFreeReading();
    queue.setFreeWriting();

    Properties jndiProps = new Properties();
    jndiProps.setProperty("java.naming.factory.initial", "fr.dyade.aaa.jndi2.client.NamingContextFactory");
    jndiProps.setProperty("java.naming.factory.host", "localhost");
    jndiProps.setProperty("java.naming.factory.port", "16401");
    
    javax.naming.Context jndiCtx = new javax.naming.InitialContext(jndiProps);
    jndiCtx.bind("mqs_cf", cf);
    jndiCtx.bind("mqs_dest", queue);
    jndiCtx.close();

    AdminModule.disconnect();
    System.out.println("Admin closed.");
  }
  
  static void bridgeAdmin() throws Exception {
    System.out.println();
    System.out.println("Bridge administration...");

    ConnectionFactory bridgeCF = TcpConnectionFactory.create("localhost", 16011);
    AdminModule.connect(bridgeCF, "root", "root");
    
    User.create("anonymous", "anonymous");

    // Creating a Queue Distribution bridge on bridge server
    Properties queueProps = new Properties();
    queueProps.setProperty("distribution.async", "true");
    Queue distq = JMSDistributionQueue.create(1, "distQ", "mqs_dest", queueProps);
    distq.setFreeWriting();
    System.out.println("joram distribution queue = " + distq);
    
    // bind foreign destination and connectionFactory
    Properties jndiProps = new Properties();
    jndiProps.setProperty("java.naming.factory.initial", "fr.dyade.aaa.jndi2.client.NamingContextFactory");
    jndiProps.setProperty("java.naming.factory.host", "localhost");
    jndiProps.setProperty("java.naming.factory.port", "16401");
 
    javax.naming.Context jndiCtx = new javax.naming.InitialContext(jndiProps);
    jndiCtx.rebind("distq", distq);
    jndiCtx.rebind("bridgeCF", bridgeCF);
    jndiCtx.close();

    AdminModule.disconnect();
    System.out.println("Admin closed.");
  }
}
