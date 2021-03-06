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

import java.net.URI;
import java.util.HashMap;
import java.util.Properties;
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


public class RestProducer {
  Client client = null;
  
  URI uriCloseProd = null;
  URI uriSendNextMsg = null;
  
  boolean debug = false;

  RestProducer(String uri, String dest) {
    URI base = UriBuilder.fromUri(uri).build();
    client = ClientBuilder.newClient(new ClientConfig());
    
    WebTarget target = client.target(base);
    System.out.println("Use Rest/JMS interface: " + target.getUri());

    // lookup the destination
    Builder builder = target.path("jndi").path(dest).request();
    Response response = builder.accept(MediaType.TEXT_PLAIN).head();
    
    System.out.println("Lookup \"" + dest + "\" -> " + response.getStatus());
    if (debug) print(response.getLinks());
    
    URI uriCreateProd = client.target(response.getLink("create-producer"))
//      .queryParam("name", "prod1")
        .getUri();

    System.out.println("URI -> " + uriCreateProd);
    // Create the producer
    response = client.target(uriCreateProd)
        .request()
        .accept(MediaType.TEXT_PLAIN).post(Entity.entity(null, MediaType.APPLICATION_FORM_URLENCODED));

    uriCloseProd = response.getLink("close-context").getUri();
    uriSendNextMsg = response.getLink("send-next-message").getUri();
  }
  
  void sendStringMessage(String content) {
    Response response = client.target(uriSendNextMsg).request()
        .accept(MediaType.TEXT_PLAIN)
        .post(Entity.entity(content, MediaType.TEXT_PLAIN));

      if (debug) {
        System.out.println("== send-next-message = " + response.getStatus());
        print(response.getLinks());
      }
      
      uriSendNextMsg = response.getLink("send-next-message").getUri();
  }
  
  public static final String BytesMessage = "BytesMessage";
  public static final String MapMessage = "MapMessage";
  public static final String TextMessage = "TextMessage";
  
  void sendBytesMessage(byte[] content, HashMap<String, Object> header, HashMap<String, Object> props) {
    HashMap<String, Object> msg = new HashMap<String, Object>();

    msg.put("type", BytesMessage);
    if ((header != null) && (header.size() > 0)) msg.put("header", header);
    if ((props != null) && (props.size() > 0)) msg.put("properties", props);
    msg.put("body", content);
    
    Gson gson = new GsonBuilder().create();
    String json = gson.toJson(msg);

    if (debug) System.out.println("send json = " + json);

    // Send next message
    Response response = client.target(uriSendNextMsg).request()
        .accept(MediaType.TEXT_PLAIN)
        .post(Entity.entity(json, MediaType.APPLICATION_JSON));

    if (debug) {
      System.out.println("== send-next-message = " + response.getStatus());
      print(response.getLinks());
    }
    
    uriSendNextMsg = response.getLink("send-next-message").getUri();
  }
  
  void sendTextMessage(String content, HashMap<String, Object> header, HashMap<String, Object> props) {
    HashMap<String, Object> msg = new HashMap<String, Object>();

    msg.put("type", TextMessage);
    if ((header != null) && (header.size() > 0)) msg.put("header", header);
    if ((props != null) && (props.size() > 0)) msg.put("properties", props);
    msg.put("body", content);
    
    Gson gson = new GsonBuilder().create();
    String json = gson.toJson(msg);

    if (debug) System.out.println("send json = " + json);

    // Send next message
    Response response = client.target(uriSendNextMsg).request()
        .accept(MediaType.TEXT_PLAIN)
        .post(Entity.entity(json, MediaType.APPLICATION_JSON));

    if (debug) {
      System.out.println("== send-next-message = " + response.getStatus());
      print(response.getLinks());
    }
    
    uriSendNextMsg = response.getLink("send-next-message").getUri();
  }

  void close() {
    Response response = client.target(uriCloseProd).request().accept(MediaType.TEXT_PLAIN).delete();
    
    System.out.println("Close consumer -> " + response.getStatus());
    if (debug) print(response.getLinks());
  }

  private static void print(Set<Link> links) {
    System.out.println("  link :");
    for (Link link : links)
      System.out.println("\t" + link.getRel() + " : " + link.getUri());
  }
}
