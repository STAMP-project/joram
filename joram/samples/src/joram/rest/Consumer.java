package rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Consumer {

  public static void main(String[] args) {
    String queueName = System.getProperty("queue");
    if ((queueName == null) || queueName.isEmpty())
      queueName = "queue";

    RestConsumer cons = new RestConsumer("http://localhost:8989/joram/", queueName);
//  cons.debug = true;
    testJSonMessage(cons);
//    testStringMessage(cons);
    cons.close();
  }
  
  static void testStringMessage(RestConsumer cons) {
    for (int i=0; i<10; i++) {
      String msg = cons.receiveStringMsg();
      System.out.println(msg);
    }
  }

  static void testJSonMessage(RestConsumer cons) {
    for (int i=0; i<10; i++) {
      HashMap<String, Object> msg = cons.receiveJSonMsg();
      System.out.println("Receive message -> " + msg);

      String type = (String) msg.get("type");

      Gson gson = new GsonBuilder().create();
      if (RestConsumer.BytesMessage.equals(type)) {
        byte[] body = null;

        body = gson.fromJson(msg.get("body").toString(), byte[].class);
      } else if ("TextMessage".equals(type)) {
        String body = null;

        body = (String) msg.get("body");
      }

      Map header = (Map) msg.get("header");
      Map props = (Map) msg.get("properties");

      int index = Integer.parseInt((String) ((ArrayList) props.get("index")).get(0));
      long time = Long.parseLong((String) ((ArrayList) props.get("time")).get(0));

      System.out.println("Receive msg#" + index + " at " + time);
    }
  }
}
