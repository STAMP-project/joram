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

    URI uriCreateCons = client.target(response.getLink("create-consumer"))
//      .queryParam("name", "cons1")
        .queryParam("idle-timeout", "120000")
        .getUri();
    
    response = client.target(uriCreateCons)
        .request()
        .accept(MediaType.TEXT_PLAIN).post(null);

    System.out.println("== create-consumer = " + response.getStatus());
    print(response.getLinks());

    URI uriCloseCons = response.getLink("close-context").getUri();
    URI uriReceiveNextMsg = response.getLink("receive-next-message").getUri();
    
    for (int i=0; i<(Round*NbMsgPerRound); i++) {
      response = client.target(uriReceiveNextMsg)
          .queryParam("timeout", "30000")
          .request()
          .accept(MediaType.TEXT_PLAIN)
          .get();
      String msg = response.readEntity(String.class);

      last = System.currentTimeMillis();
      //      int index = msg.getIntProperty("index");
      if (i == 0) start = t1 = last;

      //      long dt = (last - msg.getLongProperty("time"));
      //      travel += dt;

      if ((i%NbMsgPerRound) == (NbMsgPerRound -1)) {
        long x = (NbMsgPerRound * 1000L) / (last - t1);
        t1 = last;
        System.out.println("#" + ((i+1)/NbMsgPerRound) + " x " + NbMsgPerRound + " msg -> " + x + " msg/s " + (travel/i));
      }
      if (response.getStatus() == Response.Status.OK.getStatusCode() && msg != null) {
        //        System.out.println("== receive-next-message = " + response.getStatus() + ", msg = " + msg);
      } else {
        System.out.println("ERROR consume msg = " + msg + ", response = " + response);
      }
    }

    long x = (Round * NbMsgPerRound * 1000L) / (last - start);
    System.out.println("Moy -> " + x + " msg/s ");
    
    response = client.target(uriCloseCons)
        .request()
        .accept(MediaType.TEXT_PLAIN)
        .delete();

    System.out.println("== close-consumer = " + response.getStatus());
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
