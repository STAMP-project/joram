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

import javax.jms.BytesMessage;
import javax.jms.TextMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.InitialContext;

import org.objectweb.joram.client.jms.Destination;

/**
 * MessageProducer sending messages on queue or topic for performance statistics.
 */
public class PerfProducer implements Runnable {
  static int NbClient = 1;
  static int Round = 50;
  static int NbMsgPerRound = 1000;
  static int MsgSize = 1000;
  static int mps = 10000;

  static Destination dest = null;
  static ConnectionFactory cf = null;

  static boolean MsgTransient = true;
  static boolean SwapAllowed = false;
  static boolean transacted = true;
  static boolean asyncSend = false;

  public static boolean getBoolean(String key, boolean def) {
    String value = System.getProperty(key, Boolean.toString(def));
    return Boolean.parseBoolean(value);
  }

  public static void main (String args[]) throws Exception {
    NbClient = Integer.getInteger("NbClient", NbClient).intValue();
    Round = Integer.getInteger("Round", Round).intValue();
    NbMsgPerRound = Integer.getInteger("NbMsgPerRound", NbMsgPerRound).intValue();
    MsgSize = Integer.getInteger("MsgSize", MsgSize).intValue();
    mps = Integer.getInteger("mps", mps).intValue();

    MsgTransient = getBoolean("MsgTransient", MsgTransient);
    SwapAllowed = getBoolean("SwapAllowed", SwapAllowed);
    transacted = getBoolean("Transacted", transacted);
    asyncSend = getBoolean("asyncSend", asyncSend);

    String queueName = System.getProperty("queue");
        
    Properties jndiProps = new Properties();
    jndiProps.setProperty("java.naming.factory.initial", "fr.dyade.aaa.jndi2.client.NamingContextFactory");
    jndiProps.setProperty("java.naming.factory.host", "localhost");
    jndiProps.setProperty("java.naming.factory.port", "16401");
 
    javax.naming.Context jndiCtx = new javax.naming.InitialContext(jndiProps);
    dest = (Destination) jndiCtx.lookup(queueName);
    cf = (ConnectionFactory) jndiCtx.lookup("bridgeCF");
    jndiCtx.close();

    System.out.println("Destination: " + (dest.isQueue()?"Queue":"Topic"));
    System.out.println("Message: MsgTransient=" + MsgTransient);
    System.out.println("Message: SwapAllowed=" + SwapAllowed);
    System.out.println("Transacted=" + transacted);
    System.out.println("asyncSend=" + asyncSend);
    System.out.println("NbMsg=" + (Round*NbMsgPerRound) + ", MsgSize=" + MsgSize);
    
    ((org.objectweb.joram.client.jms.ConnectionFactory) cf).getParameters().asyncSend = asyncSend;
    
    for (int i=0; i<NbClient; i++) {
      new Thread(new PerfProducer()).start();
    }
  }

  public void run() {
    try {
      Connection cnx = cf.createConnection();
      Session session = cnx.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
      MessageProducer producer = session.createProducer(dest);
      if (MsgTransient) {
        producer.setDeliveryMode(javax.jms.DeliveryMode.NON_PERSISTENT);
      }
      
      StringBuffer strbuf = new StringBuffer();
      for (int i = 0; i< MsgSize; i++)
        strbuf.append('0');
      String content = strbuf.toString();

      long dtx = 0;
      long start = System.currentTimeMillis();
      for (int i=0; i<(Round*NbMsgPerRound); i++) {
        TextMessage msg = session.createTextMessage();
        //BytesMessage msg = session.createBytesMessage();
        if (SwapAllowed) {
          msg.setBooleanProperty("JMS_JORAM_SWAPALLOWED", true);
        }
        msg.setText(content);
        //msg.writeBytes(content);
        msg.setLongProperty("time", System.currentTimeMillis());
        msg.setIntProperty("index", i);
        
        producer.send(msg);

        if (transacted && ((i%10) == 9)) session.commit();

        if ((i%NbMsgPerRound) == (NbMsgPerRound-1)) {
          long dtx1 = (i * 1000L) / mps;
          long dtx2 = System.currentTimeMillis() - start;
          if (dtx1 > (dtx2 + 20)) {
            dtx += (dtx1 - dtx2);
            try {
              Thread.sleep(dtx1 - dtx2);
            } catch (InterruptedException exc) { }
          }
          if (dtx2 > 0)
            System.out.println("sent=" + i + ", mps=" + ((((long) i) * 1000L)/dtx2));
          else
            System.out.println("sent=" + i);
        }
      }
      long end = System.currentTimeMillis();
      long dt = end - start;

      System.out.println("----------------------------------------------------");
      System.out.println("| sender dt=" +  ((dt *1000L)/(Round*NbMsgPerRound)) + "us -> " +
          ((1000L * (Round*NbMsgPerRound)) / (dt)) + "msg/s");
      System.out.println("| sender wait=" + dtx + "ms");

      cnx.close();
    } catch (Exception exc) {
      exc.printStackTrace();
    }
  }
}
