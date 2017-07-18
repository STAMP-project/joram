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

public class Consumer {

  public static void main(String[] args) {
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
        .queryParam("idle-timeout", "120")
        .getUri();
    
    response = client.target(uriCreateCons)
        .request()
        .accept(MediaType.TEXT_PLAIN).post(null);

    System.out.println("== create-consumer = " + response.getStatus());
    print(response.getLinks());

    URI uriCloseCons = response.getLink("close-context").getUri();
    URI uriReceiveNextMsg = response.getLink("receive-next-message").getUri();

    for (int i=0; i<10; i++) {
      response = client.target(uriReceiveNextMsg)
          .queryParam("timeout", "30000")
          .request()
          .accept(MediaType.TEXT_PLAIN)
          .get();

      String msg = response.readEntity(String.class);
      if (response.getStatus() == Response.Status.OK.getStatusCode() && msg != null) {
        System.out.println("== receive-next-message = " + response.getStatus() + ", msg = " + msg);
      } else {
        System.out.println("ERROR consume msg = " + msg + ", response = " + response);
      }
    }

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
