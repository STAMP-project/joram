package rest;

import java.util.HashMap;

public class Producer {

  public static void main(String[] args) {
    RestProducer prod = new RestProducer("http://localhost:8989/joram/", "queue");
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
