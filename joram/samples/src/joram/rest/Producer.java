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

public class Producer {

  public static void main(String[] args) {
    String queueName = System.getProperty("queue");
    if ((queueName == null) || queueName.isEmpty())
      queueName = "queue";
    
    RestProducer prod = new RestProducer("http://localhost:8989/joram/", queueName);
//    prod.debug = true;
    testJSonMessage(prod);
//    testStringMessage(prod);
    // close the producer
    prod.close();
  }

  static void testStringMessage(RestProducer prod) {
    for (int i=0; i<10; i++) {
      // Send next message
      byte[] content = ("Test message#" + i).getBytes();
//      prod.sendNextMessage("Test message#" + i);
      HashMap<String, Object> header = new HashMap<String, Object>();
      header.put("CorrelationID", "0123456789");
      HashMap<String, Object> props = new HashMap<String, Object>();
      props.put("time", new String[]{"" + System.currentTimeMillis(), Long.class.getName()});
      props.put("index", new String[]{"" + i, Integer.class.getName()});
      
      //      prod.sendNextMessage(new String(content));
//      prod.sendBytesMessage(content, header, props);
      prod.sendTextMessage("Test message#" + i, header, props);

      System.out.println("Send message -> " + "Test message#" + i);
    }
  }

  static void testJSonMessage(RestProducer prod) {
    for (int i=0; i<10; i++) {
      // Send next message
      byte[] content = ("Test message#" + i).getBytes();
//      prod.sendNextMessage("Test message#" + i);
      HashMap<String, Object> header = new HashMap<String, Object>();
      header.put("CorrelationID", "0123456789");
      HashMap<String, Object> props = new HashMap<String, Object>();
      props.put("time", new String[]{"" + System.currentTimeMillis(), Long.class.getName()});
      props.put("index", new String[]{"" + i, Integer.class.getName()});
      
      //      prod.sendNextMessage(new String(content));
//      prod.sendBytesMessage(content, header, props);
      prod.sendTextMessage("Test message#" + i, header, props);

      System.out.println("Send message -> " + "Test message#" + i);
    }
  }
}
