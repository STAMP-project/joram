/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2018 - 2019 ScalAgent Distributed Technologies
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
package rest;

import java.util.HashMap;

/**
 * MessageProducer sending messages on queue or topic for performance statistics.
 */
public class PerfProducer implements Runnable {
  static int NbClient = 1;
  static int Round = 50;
  static int NbMsgPerRound = 1000;
  static int MsgSize = 1000;
  static int mps = 10000;

  static boolean MsgTransient = true;
  static boolean SwapAllowed = false;

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

    System.out.println("NbMsg=" + (Round*NbMsgPerRound) + ", MsgSize=" + MsgSize);
    
    for (int i=0; i<NbClient; i++) {
      new Thread(new PerfProducer()).start();
    }
  }

  // Test with JSon message
  public void run() {
    RestProducer prod = new RestProducer("http://localhost:8989/joram/", "queue");

//    byte[] content = new byte[MsgSize];
//    for (int i = 0; i< MsgSize; i++)
//      content[i] = (byte) (i & 0xFF);

    StringBuffer strbuf = new StringBuffer();
    for (int i = 0; i< MsgSize; i++)
      strbuf.append('0');
    String content = strbuf.toString();
    
    long dtx = 0;
    long start = System.currentTimeMillis();

    for (int i=0; i<(Round*NbMsgPerRound); i++) {
      // Send next message
      HashMap<String, Object> header = new HashMap<String, Object>();
      header.put("CorrelationID", "0123456789");
      HashMap<String, Object> props = new HashMap<String, Object>();
      props.put("time", new String[]{"" + System.currentTimeMillis(), Long.class.getName()});
      props.put("index", new String[]{"" + i, Integer.class.getName()});
      
//      prod.sendBytesMessage(content, header, props);
      prod.sendTextMessage(content, header, props);
      
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

    // close the producer
    prod.close();
  }

  // Test with simple String message
  public void run2() {
    RestProducer prod = new RestProducer("http://localhost:8989/joram/", "queue");

    StringBuffer strbuf = new StringBuffer();
    for (int i = 0; i< MsgSize; i++)
      strbuf.append('0');
    String content = strbuf.toString();
    
    long dtx = 0;
    long start = System.currentTimeMillis();

    for (int i=0; i<(Round*NbMsgPerRound); i++) {
      prod.sendStringMessage(content);
      
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

    // close the producer
    prod.close();
  }
}
