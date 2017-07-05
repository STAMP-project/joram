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

public class Admin {
  public static void main(String[] args) throws Exception {
    System.out.println();
    System.out.println("Rest Bridge administration...");

    ConnectionFactory bridgeCF = TcpConnectionFactory.create("localhost", 16011);

    AdminModule.connect(bridgeCF, "root", "root");
    
    // Create a topic forwarding its messages to the configured rest queue.
//    Properties prop = new Properties();
//    prop.put("distribution.className", "com.scalagent.joram.mom.dest.rest.RESTDistribution");
//    prop.put("rest.hostName", "localhost");
//    prop.put("rest.port", "8989");
//    prop.put("rest.userName", "anonymous");
//    prop.put("rest.password", "anonymous");
//    prop.put("jms.destination", "queue");
//    prop.put("distribution.batch", "true");
//    prop.put("period", "1000");
//    Queue queueDist = Queue.create(1, "queueDist", Destination.DISTRIBUTION_QUEUE, prop);
    Queue queueDist = new RestDistributionQueue()
        .setBatch(true)
        .setHostName("localhost")
        .setPort(8989)
        .setPeriod(10)
        .setBatch(true)
        .create(1, "queueDist", "queue");
    queueDist.setFreeWriting();
    System.out.println("joram distribution queue = " + queueDist);

    // Create a queue getting its messages from the configured rest queue.
//    prop = new Properties();
//    prop.put("acquisition.className", "com.scalagent.joram.mom.dest.rest.RESTAcquisitionDaemon");
//    prop.put("rest.hostName", "localhost");
//    prop.put("rest.port", "8989");
//    prop.put("rest.userName", "anonymous");
//    prop.put("rest.password", "anonymous");
//    prop.put("rest.nbMaxMsgByPeriode", "1000");
//    //prop.put("rest.mediaTypeJson", "false");
//    prop.put("rest.timeout", "5000");
//    prop.put("jms.destination", "queue");
//    //prop.put("acquisition.period", "1000");
//    Queue queueAcq = Queue.create(1, "queueAcq", Destination.ACQUISITION_QUEUE, prop);
    Queue queueAcq = new RestAcquisitionQueue()
        .setTimeout(1000)
        .setNbMaxMsgByPeriode(1000)
        .create(1, "queueAcq", "queue");
    queueAcq.setFreeReading();
    System.out.println("joram acquisition queue = " + queueAcq);

    User.create("anonymous", "anonymous");
    
    // bind foreign destination and connectionFactory
    Properties jndiProps = new Properties();
    jndiProps.setProperty("java.naming.factory.initial", "fr.dyade.aaa.jndi2.client.NamingContextFactory");
    jndiProps.setProperty("java.naming.factory.host", "localhost");
    jndiProps.setProperty("java.naming.factory.port", "16401");
 
    javax.naming.Context jndiCtx = new javax.naming.InitialContext(jndiProps);
    jndiCtx.bind("bridgeCF", bridgeCF);
    jndiCtx.bind("queueDist", queueDist);
    jndiCtx.bind("queueAcq", queueAcq);
    jndiCtx.close();
    
    AdminModule.disconnect();
    System.out.println("Admin closed.");
  }
}
