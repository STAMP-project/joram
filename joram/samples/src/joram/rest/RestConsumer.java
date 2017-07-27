package rest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RestConsumer {
  Client client = null;

  URI uriCloseCons = null;
  URI uriReceiveNextMsg = null;
  URI uriAcknowledgeMsg = null;
  
  boolean debug = false;
  
  RestConsumer(String uri, String dest) {
    URI base = UriBuilder.fromUri(uri).build();
    client = ClientBuilder.newClient(new ClientConfig());
    
    WebTarget target = client.target(base);
    System.out.println("Use Rest/JMS interface: " + target.getUri());

    // lookup the destination
    Builder builder = target.path("jndi").path("queue").request();
    Response response = builder.accept(MediaType.TEXT_PLAIN).head();
    
    System.out.println("Lookup \"" + dest + "\" -> " + response.getStatus());
    if (debug) print(response.getLinks());

    URI uriCreateCons = client.target(response.getLink("create-consumer-client-ack"))
//      .queryParam("name", "cons1")
        .queryParam("idle-timeout", "120")
        .getUri();
    
    response = client.target(uriCreateCons)
        .request()
        .accept(MediaType.TEXT_PLAIN).post(null);

    System.out.println("Create consumer -> " + response.getStatus());
    if (debug) print(response.getLinks());

    uriCloseCons = response.getLink("close-context").getUri();
    uriReceiveNextMsg = response.getLink("receive-next-message").getUri();
  }
  
  String receiveStringMsg() {
    if (debug) System.out.println("== receive-next-message -> " + uriReceiveNextMsg);
    Response response = client.target(uriReceiveNextMsg)
        .queryParam("timeout", "30000")
        .request()
        .accept(MediaType.TEXT_PLAIN)
        .get();

    String msg = response.readEntity(String.class);
    if (response.getStatus() == Response.Status.OK.getStatusCode() && msg != null) {
      if (debug) System.out.println("== receive-next-message = " + response.getStatus() + ", msg = " + msg);
      uriReceiveNextMsg = response.getLink("receive-next-message").getUri();
      uriAcknowledgeMsg = response.getLink("acknowledge-message").getUri();
    } else {
      System.out.println("ERROR consume msg = " + msg + ", response = " + response);
    }
    
    return msg;
  }
  
  public static final String BytesMessage = "BytesMessage";
  public static final String MapMessage = "MapMessage";
  public static final String TextMessage = "TextMessage";

  HashMap<String, Object> receiveJSonMsg() {
    if (debug) System.out.println("== receive-next-message -> " + uriReceiveNextMsg);
    Response response = client.target(uriReceiveNextMsg)
        .queryParam("timeout", "30000")
        .request()
        .accept(MediaType.APPLICATION_JSON)
        .get();
    
    String json = response.readEntity(String.class);
    if (debug) System.out.println("Receive json = " + json);
    
    HashMap<String, Object> msg = null;
    if (response.getStatus() == Response.Status.OK.getStatusCode() && json != null) {
      Gson gson = new GsonBuilder().create();
      msg = gson.fromJson(json, HashMap.class);

      if (debug) {
        Map header = (Map) msg.get("header");
        Map props = (Map) msg.get("properties");

        System.out.println("*** header " + header);
        System.out.println("*** properties " + props);
        System.out.println("== receive-next-message = " + response.getStatus() + ", msg = " + msg);

        print(response.getLinks());
      }
      uriReceiveNextMsg = response.getLink("receive-next-message").getUri();
      uriAcknowledgeMsg = response.getLink("acknowledge-message").getUri();
    } else {
      System.out.println("ERROR receive-next-message = " + response.getStatus() + ", msg = " + json);
    }
    
    acknowledgeMessage();
    
    return msg;
  }
  
  void acknowledgeMessage() {
    if (debug) System.out.println("== acknowledge-message -> " + uriAcknowledgeMsg);
    Response response = client.target(uriAcknowledgeMsg)
        .request()
        .accept(MediaType.TEXT_PLAIN)
        .delete();
    if (debug) System.out.println("== acknowledge-message = " + response.getStatus());
  }
  
  void close() {
    Response response = client.target(uriCloseCons).request().accept(MediaType.TEXT_PLAIN).delete();

    System.out.println("Close consumer -> " + response.getStatus());
    if (debug) print(response.getLinks());
  }

  private static void print(Set<Link> links) {
    System.out.println("  link :");
    for (Link link : links)
      System.out.println("\t" + link.getRel() + " : " + link.getUri());
  }
}
