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

public class Producer {

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
    
    URI uriCreateProd = client.target(response.getLink("create-producer"))
//      .queryParam("name", "prod1")
        .getUri();

    // Create the producer
    response = client.target(uriCreateProd)
        .request()
        .accept(MediaType.TEXT_PLAIN).post(null);

    URI uriCloseProd = response.getLink("close-context").getUri();
    URI uriSendNextMsg = response.getLink("send-next-message").getUri();

    for (int i=0; i<10; i++) {
      // Send next message
      response = client.target(uriSendNextMsg).request()
        .accept(MediaType.TEXT_PLAIN)
        .post(Entity.entity("Test message#" + i, MediaType.TEXT_PLAIN));

      System.out.println("== send-next-message = " + response.getStatus());
      print(response.getLinks());
      uriSendNextMsg = response.getLink("send-next-message").getUri();
    }
    
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
