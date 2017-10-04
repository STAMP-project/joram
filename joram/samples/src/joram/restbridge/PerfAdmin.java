/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2017 ScalAgent Distributed Technologies
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
package restbridge;

import java.util.Properties;

import javax.jms.ConnectionFactory;

import org.objectweb.joram.client.jms.Destination;
import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;
import org.objectweb.joram.client.jms.admin.JMSAcquisitionQueue;
import org.objectweb.joram.client.jms.admin.JMSDistributionQueue;
import org.objectweb.joram.client.jms.admin.RestAcquisitionQueue;
import org.objectweb.joram.client.jms.admin.RestDistributionQueue;

public class PerfAdmin {
  public static void main(String[] args) throws Exception {
    System.out.println();
    System.out.println("Rest Bridge administration...");

    ConnectionFactory bridgeCF = TcpConnectionFactory.create("localhost", 16011);

    AdminModule.connect(bridgeCF, "root", "root");
    
    // Creates queues forwarding their messages to the configured rest queue.
    
    Queue queueDist1 = new RestDistributionQueue()
        .setHost("localhost")
        .setPort(8989)
        .setIdleTimeout(10000)
        .create(1, "queueDist1", "queue");
    queueDist1.setFreeWriting();
    System.out.println("joram distribution queue = " + queueDist1);

    Queue queueDist2 = new RestDistributionQueue()
        .setHost("localhost")
        .setPort(8989)
        .setIdleTimeout(10000)
        .create(1, "queueDist2", "queue");
    queueDist2.setFreeWriting();
    System.out.println("joram distribution queue = " + queueDist2);

    // Creates queues getting its messages from the configured rest queue.
    
    Queue queueAcq1 = new RestAcquisitionQueue()
        .setHost("localhost")
        .setPort(8989)
        .setTimeout(5000)
        .setIdleTimeout(10000)
        .create(1, "queueAcq1", "queue");
    queueAcq1.setFreeReading();
    System.out.println("joram acquisition queue = " + queueAcq1);
    
    Queue queueAcq2 = new RestAcquisitionQueue()
        .setHost("localhost")
        .setPort(8989)
        .setTimeout(5000)
        .setIdleTimeout(10000)
        .create(1, "queueAcq2", "queue");
    queueAcq2.setFreeReading();
    System.out.println("joram acquisition queue = " + queueAcq2);

    User.create("anonymous", "anonymous");
    
    // bind foreign destination and connectionFactory
    Properties jndiProps = new Properties();
    jndiProps.setProperty("java.naming.factory.initial", "fr.dyade.aaa.jndi2.client.NamingContextFactory");
    jndiProps.setProperty("java.naming.factory.host", "localhost");
    jndiProps.setProperty("java.naming.factory.port", "16401");
 
    javax.naming.Context jndiCtx = new javax.naming.InitialContext(jndiProps);
    jndiCtx.bind("bridgeCF", bridgeCF);
    jndiCtx.bind("queueDist1", queueDist1);
    jndiCtx.bind("queueDist2", queueDist2);
    jndiCtx.bind("queueAcq1", queueAcq1);
    jndiCtx.bind("queueAcq2", queueAcq2);
    jndiCtx.close();
    
    AdminModule.disconnect();
    System.out.println("Admin closed.");
  }
}
