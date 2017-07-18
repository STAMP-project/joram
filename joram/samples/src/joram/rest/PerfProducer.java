package rest;

import java.net.URI;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;

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

  public void run() {
    ClientConfig config = new ClientConfig();
    Client client = ClientBuilder.newClient(config);
    WebTarget target = client.target(getBaseURI());
    System.out.println(target.getUri());

    // lookup the destination
    Builder builder = target.path("jndi").path("queue").request();
    Response response = builder.accept(MediaType.TEXT_PLAIN).head();
    System.out.println("== lookup \"queue\" = " + response.getStatus());
    print(response.getLinks());

    URI uriCreateProd = client.target(response.getLink("create-producer"))
        //      .queryParam("name", "prod1")
        .getUri();

    // Create the producer
    response = client.target(uriCreateProd)
        .request()
        .accept(MediaType.TEXT_PLAIN).post(null);

    URI uriCloseProd = response.getLink("close-context").getUri();
    URI uriSendNextMsg = response.getLink("send-next-message").getUri();

    byte[] content = new byte[MsgSize];
    for (int i = 0; i< MsgSize; i++)
      content[i] = (byte) (i & 0xFF);

    long dtx = 0;
    long start = System.currentTimeMillis();

    for (int i=0; i<(Round*NbMsgPerRound); i++) {
      // Send next message
//      msg.setLongProperty("time", System.currentTimeMillis());
//      msg.setIntProperty("index", i);
      
      response = client.target(uriSendNextMsg).request()
        .accept(MediaType.TEXT_PLAIN)
        .post(Entity.entity(new String(content), MediaType.TEXT_PLAIN));
      
      uriSendNextMsg = response.getLink("send-next-message").getUri();

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
    response = client.target(uriCloseProd).request().accept(MediaType.TEXT_PLAIN).delete();
    System.out.println("== close-producer = " + response.getStatus());
    print(response.getLinks());
  }
  
  private static URI getBaseURI() {  
    return UriBuilder.fromUri("http://localhost:8989/joram/").build();  
  }

  private static void print(Set<Link> links) {
    System.out.println("  link :");
    for (Link link : links)
      System.out.println("\t" + link.getRel() + " : " + link.getUri());
  }
}
