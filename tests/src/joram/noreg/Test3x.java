/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2003 - 2007 ScalAgent Distributed Technologies
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
 * Initial developer(s): Freyssinet Andre (ScalAgent D.T.)
 * Contributor(s): Badolle Fabien (ScalAgent D.T.)
 */
package joram.noreg;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.objectweb.joram.client.jms.Destination;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.User;

import fr.dyade.aaa.agent.AgentServer;

/**
 * Test transfer with big messages
 */
public class Test3x extends BaseTest {
  static int MsgSize = 1*1024*1024;
  static int NbRound = 10;
  static int NbMsg = 100;
  static int MsgPerCommit = 10;

  static Destination dest = null;
  static ConnectionFactory cf = null;

  static String host = "localhost";
  static int port = 16010;

  public static void main(String[] args) throws Exception {
    new Test3x().run(args);
  }

  public void run(String[] args) {
    try{
      if (! Boolean.getBoolean("ServerOutside"))
        startServer();
      writeIntoFile("===================== start test 3 =====================");
      MsgSize = Integer.getInteger("MsgSize", MsgSize/1024).intValue() *1024;
      NbRound = Integer.getInteger("NbRound", NbRound).intValue();
      NbMsg = Integer.getInteger("NbMsg", NbMsg).intValue();
      MsgPerCommit = Integer.getInteger("MsgPerCommit", MsgPerCommit).intValue(); 
      String destc = System.getProperty("Destination",
      "org.objectweb.joram.client.jms.Queue");

      host = System.getProperty("hostname", host);
      port = Integer.getInteger("port", port).intValue();

      writeIntoFile("----------------------------------------------------");
      writeIntoFile("Destination: " + destc);
      writeIntoFile("MsgSize: " + MsgSize);
      writeIntoFile("MsgPerCommit: " + MsgPerCommit);
      writeIntoFile("NbMsg: " + NbMsg);
      writeIntoFile("----------------------------------------------------");

      cf = TcpBaseTest.createConnectionFactory();
      AdminModule.connect(cf);
      dest = createDestination(destc);
      User user = User.create("anonymous", "anonymous", 0);
      dest.setFreeReading();
      dest.setFreeWriting();
      org.objectweb.joram.client.jms.admin.AdminModule.disconnect();

      MsgListener listener = new MsgListener(cf, dest);
      
      Connection cnx = cf.createConnection();
      Session sess = cnx.createSession(false, Session.AUTO_ACKNOWLEDGE);
      MessageProducer prod = sess.createProducer(dest);
      cnx.start();

      for (int round=1; round<=NbRound; round++) {
        byte[] content = new byte[round * MsgSize];
        for (int i = 0; i< (round * MsgSize); i++)
          content[i] = (byte) (i & 0xFF);

        listener.reset();
        
        BytesMessage msg1 = sess.createBytesMessage();
        msg1.writeBytes(content);
        for (int nb=0; nb<NbMsg; nb++) {
          msg1.setBooleanProperty("JMS_JORAM_SWAPALLOWED", true);
          msg1.setLongProperty("time", System.nanoTime());
          prod.send(msg1);
          System.out.print("S");
//          if ((nb % MsgPerCommit) == 0) {
//            System.out.print("C");
//            //writeIntoFile(MsgPerCommit+" message sent ;message :" + nb);
//            sess.commit();
//          }
          System.out.flush();
          System.out.checkError();
          Thread.sleep(10L);
        }
        
        long avlat = listener.check(NbMsg)/NbMsg;
        System.out.println("\naverage latency: " + (avlat/1000L) + "us - " + ((round*MsgSize)/(1024*1024)) + "Mb.");
      }
    }catch(Throwable exc){
      exc.printStackTrace();
      error(exc);
    }finally{
      AgentServer.stop();
      endTest();
    }

    System.exit(0);
  }
}

class MsgListener implements MessageListener {
  long latency = 0;
  
  public MsgListener(ConnectionFactory cf, Destination dest) throws JMSException {
    Connection cnx = cf.createConnection();
    Session sess = cnx.createSession(false, Session.AUTO_ACKNOWLEDGE);
    MessageConsumer cons = sess.createConsumer(dest);
    cons.setMessageListener(this);
    cnx.start();
  }

  public void onMessage(Message msg) {
    try {
//      System.err.println("receive" + cpt);
      inc(System.nanoTime() - msg.getLongProperty("time"));
    } catch (JMSException e) {}
    
    System.out.print("R");
    System.out.flush();
  }
  
  int cpt = 0;
  
  public synchronized void reset() {
    cpt = 0;
    latency = 0;
  }

  public synchronized void inc(long l) {
    cpt += 1;
//    System.err.println("inc " + cpt);
    latency += l;
    notify();
  }
  
  public synchronized long check(int limit) {
    while (cpt < limit) {
//      System.err.println("check " + cpt + "/" + limit);
      try {
        wait();
      } catch (InterruptedException e) {}
    }
    return latency;
  }
}

