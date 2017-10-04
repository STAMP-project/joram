package rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PerfConsumer implements Runnable {
  static int NbClient = 1;
  static int Round = 50;
  static int NbMsgPerRound = 1000;

  long travel = 0L;

  long start = 0L;
  long last = 0L;

  long t1 = 0L;

  public static void main(String[] args) {
    NbClient = Integer.getInteger("NbClient", NbClient).intValue();
    Round = Integer.getInteger("Round", Round).intValue();
    NbMsgPerRound = Integer.getInteger("NbMsgPerRound", NbMsgPerRound).intValue();

    System.out.println("NbMsg=" + (Round*NbMsgPerRound));
    
    for (int i=0; i<NbClient; i++) {
      new Thread(new PerfConsumer()).start();
    }
  }

  // Test with JSon message
  public void run() {
    RestConsumer cons = new RestConsumer("http://localhost:8989/joram/", "queue");

    for (int i=0; i<(Round*NbMsgPerRound); i++) {
      //      String msg = cons.receiveNextMsg();
      HashMap<String, Object> msg = cons.receiveJSonMsg();

      String type = (String) msg.get("type");
      Map header = (Map) msg.get("header");
      Map props = (Map) msg.get("properties");

      Gson gson = new GsonBuilder().create();
      if (RestConsumer.BytesMessage.equals(type)) {
        byte[] body = gson.fromJson(msg.get("body").toString(), byte[].class);
      } else if (RestConsumer.MapMessage.equals(type)) {
        Map body = (Map) msg.get("body");
      } else if (RestConsumer.TextMessage.equals(type)) {
        String body = (String) msg.get("body");
      } else {
        System.out.println("Error receiving message");
        break;
      }

      last = System.currentTimeMillis();
      int index = Integer.parseInt((String) ((ArrayList) props.get("index")).get(0));
      if (i == 0) start = t1 = last;

      long time = Long.parseLong((String) ((ArrayList) props.get("time")).get(0));
      long dt = (last - time);
      travel += dt;

      if ((i%NbMsgPerRound) == (NbMsgPerRound -1)) {
        long x = (NbMsgPerRound * 1000L) / (last - t1);
        t1 = last;
        System.out.println("#" + ((i+1)/NbMsgPerRound) + " x " + NbMsgPerRound + " msg -> " + x + " msg/s " + (travel/i));
      }
    }

    long x = (Round * NbMsgPerRound * 1000L) / (last - start);
    System.out.println("Moy -> " + x + " msg/s ");

    cons.close();
  }

  // Test with simple String message
  public void run2() {
    RestConsumer cons = new RestConsumer("http://localhost:8989/joram/", "queue");

    for (int i=0; i<(Round*NbMsgPerRound); i++) {
      String msg = cons.receiveStringMsg();

      last = System.currentTimeMillis();
//      int index = Integer.parseInt((String) ((ArrayList) props.get("index")).get(0));
      if (i == 0) start = t1 = last;

//      long time = Long.parseLong((String) ((ArrayList) props.get("time")).get(0));
//      long dt = (last - time);
//      travel += dt;

      if ((i%NbMsgPerRound) == (NbMsgPerRound -1)) {
        long x = (NbMsgPerRound * 1000L) / (last - t1);
        t1 = last;
        System.out.println("#" + ((i+1)/NbMsgPerRound) + " x " + NbMsgPerRound + " msg -> " + x + " msg/s " + (travel/i));
      }
    }

    long x = (Round * NbMsgPerRound * 1000L) / (last - start);
    System.out.println("Moy -> " + x + " msg/s ");

    cons.close();
  }
}