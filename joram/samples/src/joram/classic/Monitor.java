/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2019 ScalAgent Distributed Technologies
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
 * Initial developer(s): Frederic Maistre (INRIA)
 * Contributor(s): ScalAgent Distributed Technologies
 */
package classic;

import java.util.Hashtable;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.Topic;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;

/**
 * Monitors the queue.
 */
public class Monitor {

  public static void main(String[] args) throws Exception {
    ConnectionFactory cf = TcpConnectionFactory.create("localhost", 16010);
    AdminModule.connect(cf, "root", "root");

    Queue queue = Queue.create("queue");
    Queue dmq = Queue.create("dmq");
    queue.setDMQ(dmq);
    
    int requests = queue.getPendingRequests();
    System.out.println("requests: " + requests);
    int waiting = queue.getPendingMessages();
    System.out.println("waiting: " + waiting);
    int delivered = queue.getDeliveredMessages();
    System.out.println("delivered: " + delivered);
    String dmqName = queue.getDMQ().getQueueName();
    System.out.println("dmqName: " + dmqName);
    String dmqId = queue.getDMQId();
    System.out.println("dmqId: " + dmqId);
    int threshold = queue.getThreshold();
    System.out.println("threshold: " + threshold);
    
    Hashtable statistics = queue.getStatistics();
    System.out.println("statistics: " + statistics);
    
    int deliveryDelay = (Integer) statistics.get("DeliveryDelay");
    System.out.println("deliveryDelay: " + deliveryDelay);

//    statistics = AdminModule.getJMXAttribute("Joram#0:type=Destination,name=queue(WaitingRequestCount,PendingMessageCount,DeliveredMessageCount,DeliveryDelay,Pause,Threshold)");
    statistics = AdminModule.getJMXAttribute("Joram#0:type=Destination,name=queue(*)");
//    statistics = AdminModule.getJMXAttribute("Joram#0:type=Destination,name=queue(Period,Pause,RedeliveryDelay)");
    System.out.println("jmsxatts: " + statistics);
    
    boolean pause = (Boolean) AdminModule.getJMXAttribute("Joram#0:type=Destination,name=queue(Pause)").elements().nextElement();
    System.out.println("pause: " + pause);
 
    AdminModule.disconnect();
    System.out.println("Admin closed.");
  }
}