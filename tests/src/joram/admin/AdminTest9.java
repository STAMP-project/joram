/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C)  2018 ScalAgent Distributed Technologies
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
 * Contributor(s): Badolle Fabien (ScalAgent D.T.)
 */
package joram.admin;

import java.util.Hashtable;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;

import framework.TestCase;

public class AdminTest9 extends TestCase {

  public static void main(String[] args) {
    new AdminTest9().run();
  }

  public void run() {
    try {
      System.out.println("starts...");
      startAgentServer((short)0, new String[]{"-DTransaction=fr.dyade.aaa.ext.NGTransaction"});
      Thread.sleep(1000);
      System.out.println("started.");
      
      ConnectionFactory cf = TcpConnectionFactory.create("localhost", 2560);
      ((TcpConnectionFactory) cf).getParameters().connectingTimer = 60;
      ((TcpConnectionFactory) cf).getParameters().cnxPendingTimer = 15000;

      admin(cf, 1000);
      getPeriod(cf);
      Thread.sleep(10000);
      setPeriod(cf, 5000);
      getPeriod(cf);
      System.out.println("waiting...");
      Thread.sleep(30000);

      System.out.println("stops...");
      stopAgentServer((short)0);
      Thread.sleep(1000);
      System.out.println("starts...");
      startAgentServer((short)0, new String[]{"-DTransaction=fr.dyade.aaa.ext.NGTransaction"});
      Thread.sleep(1000);
      System.out.println("started.");
      getPeriod(cf);
      System.out.println("waiting...");
      Thread.sleep(30000);
    } catch (Throwable exc) {
      exc.printStackTrace();
      error(exc);
    } finally {
      stopAgentServer((short)0);
      endTest();     
    }
  }
  
  private void admin(ConnectionFactory cf, int period) throws Exception {
    Context ctx = new InitialContext();
    AdminModule.connect(cf, "root", "root");
    
    User.create("anonymous", "anonymous", 0);

    // Create a queue
    Properties props = new Properties();
    props.setProperty("period", "" + period);
    Queue queue = Queue.create(0, "queue", "org.objectweb.joram.mom.dest.Queue", props);
    queue.setFreeReading();
    queue.setFreeWriting();

    ctx.bind("queue", queue);
    
    AdminModule.disconnect();
    ctx.close();
  }
  
  private void setPeriod(ConnectionFactory cf, int period) throws Exception {
    Context ctx = new InitialContext();
    AdminModule.connect(cf, "root", "root");
    
    Queue queue = (Queue) ctx.lookup("queue");
    Properties props = new Properties();
    props.setProperty("period", "" + period);
    queue.setProperties(props);
    
    AdminModule.disconnect();
    ctx.close();
  }
  
  private void getPeriod(ConnectionFactory cf) throws Exception {
    Context ctx = new InitialContext();
    AdminModule.connect(cf, "root", "root");
    
    Queue queue = (Queue) ctx.lookup("queue");
    Hashtable props = queue.getStatistics();
    System.out.println(props.get("Period"));
    
    AdminModule.disconnect();
    ctx.close();
  }
}
