/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2017 ScalAgent Distributed Technologies
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
package com.scalagent.joram.mom.dest.rest;

import java.net.URI;
import java.util.Enumeration;
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
import org.objectweb.joram.mom.dest.DistributionHandler;
import org.objectweb.joram.shared.messages.Message;
import org.objectweb.util.monolog.api.BasicLevel;
import org.objectweb.util.monolog.api.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.dyade.aaa.common.Debug;

/**
 * Distribution handler for the REST distribution bridge.
 */
public class RESTDistribution implements DistributionHandler {

  private static final Logger logger = Debug.getLogger(RESTDistribution.class.getName());

  private static final String HOST_PROP = "rest.hostName";
  private static final String PORT_PROP = "rest.port";
  private static final String USER_NAME_PROP = "rest.userName";
  private static final String PASSWORD_PROP = "rest.password";
  private static final String DESTINATION_NAME_PROP = "jms.destination";

  private String hostName = "localhost";
  private int port = 8989;
  private Client client;
  private WebTarget target;
  private String userName = null;
  private String password = null;

  private String destName;
  private URI uriCreateProducer;
  private URI uriSendNextMsg;
  private URI uriSendMsg;
  private URI uriCloseProducer;
  
  public void init(Properties properties, boolean firstTime) {
    destName = properties.getProperty(DESTINATION_NAME_PROP);
    if (destName == null) {
      throw new IllegalArgumentException("Missing Destination JNDI name.");
    }
    if (properties.containsKey(HOST_PROP)) {
      hostName = properties.getProperty(HOST_PROP);
    }
    try {
      if (properties.containsKey(PORT_PROP)) {
        port = Integer.parseInt(properties.getProperty(PORT_PROP));
      }
    } catch (NumberFormatException nfe) {
      logger.log(BasicLevel.ERROR, "Property " + PORT_PROP
          + "could not be parsed properly, use default value.", nfe);
    }
    if (properties.containsKey(USER_NAME_PROP)) {
      userName = properties.getProperty(USER_NAME_PROP);
    }
    if (properties.containsKey(PASSWORD_PROP)) {
      password = properties.getProperty(PASSWORD_PROP);
    }
    
    ClientConfig config = new ClientConfig();
    client = ClientBuilder.newClient(config);
    target = client.target(UriBuilder.fromUri("http://" + hostName + ":" + port + "/joram/").build());
    if (logger.isLoggable(BasicLevel.DEBUG)) {
      logger.log(BasicLevel.DEBUG, "RESTDistribution.init Target : " + target.getUri());
    }
    
  }

  public void distribute(Message message) throws Exception {
//  	if (logger.isLoggable(BasicLevel.DEBUG))
//      logger.log(BasicLevel.DEBUG, "RESTDistribution.distribute(" + message + ')');
    if (uriCreateProducer == null)
      createProducer(userName, password);

    sendMessage(message);
  }

  public void close() {
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "RESTDistribution.close()");
    closeProducer();
  }
  
  private int lookup() {
    Builder builder = target.path("jndi").path(destName).request();
    Response response = builder.accept(MediaType.TEXT_PLAIN).get();
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "RESTDistribution.lookup \"" + destName +"\" = " + response.getStatus());
    print(response.getLinks());
    //TODO test status
    if (uriCreateProducer == null) {
      uriCreateProducer = response.getLink("create-producer").getUri();
    }
    return response.getStatus();
  }
  
  private int createProducer(String userName, String password) {
    if (uriCreateProducer == null) {
      WebTarget wTarget = target.path("jndi").path(destName).path("create-producer")
          .queryParam("name", "prod-" + destName)
          .queryParam("client-id", "id-" + destName);
      if (userName != null)
        wTarget = wTarget.queryParam("user", userName);
      if (password != null)
        wTarget = wTarget.queryParam("password", password);
      uriCreateProducer = wTarget.getUri();
    }

    // Create the producer prod-"destName"
    WebTarget wTarget = client.target(uriCreateProducer)
        .queryParam("name", "prod-" + destName)
        .queryParam("client-id", "id-" + destName);
    if (userName != null)
      wTarget = wTarget.queryParam("user", userName);
    if (password != null)
      wTarget = wTarget.queryParam("password", password);
    Response response  = wTarget.request().accept(MediaType.TEXT_PLAIN).post(null);
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "RESTDistribution.createProducer (" + destName + ") = " + response.getStatus());
    if (response.getStatus() > 300) {
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, "RESTDistribution.createProducer" + destName + " ERROR ==== " + response.readEntity(String.class));
      return response.getStatus();
    }
    print(response.getLinks());
    uriCloseProducer = response.getLink("close-context").getUri();
    uriSendNextMsg = response.getLink("send-next-message").getUri();
    uriSendMsg = response.getLink("send-message").getUri();
    return response.getStatus();
  }
  
  private int sendMessage(Message message) throws Exception {
    if (uriSendMsg == null) {
      return 400;
    }
    
    Response response;
    if (message.properties == null && message.type == Message.TEXT) {
      // no properties, only senda text message
      response = client.target(uriSendMsg).request().
          accept(MediaType.TEXT_PLAIN).post(Entity.entity(message.getText(), MediaType.TEXT_PLAIN));
      if (response.getStatus() != 200) {
        if (logger.isLoggable(BasicLevel.DEBUG))
          logger.log(BasicLevel.DEBUG, "RESTDistribution.sendMessage: " + response.getStatus() + ", not send: " + message);
      } else {
        if (logger.isLoggable(BasicLevel.DEBUG))
          logger.log(BasicLevel.DEBUG, "RESTDistribution.sendMessage: " + message);
      }
    } else {
      
      HashMap<String, Object> maps = new HashMap<>();
      // message body
      if (message.type == Message.TEXT) {
        message.jmsType = "TextMessage";
        maps.put("body", message.getText());
      } else if (message.type == Message.BYTES) {
        message.jmsType = "BytesMessage";
        maps.put("body", message.body);
      } else if (message.type == Message.MAP) {
        message.jmsType = "MapMessage";
        maps.put("body", message.getObject());
      } else {
        logger.log(BasicLevel.ERROR, "RESTDistribution.sendMessage: type " + message.jmsType + " not yet supported.");
        return 400;
      }
      // message type
      maps.put("type", message.jmsType);
      
      // message properties
      HashMap<String, Object> props = new HashMap<>();
      Enumeration e = message.properties.keys();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        Object value = message.properties.get(key);
        props.put(key, new String[]{value.toString(), value.getClass().getName()});
      }
      maps.put("properties", props);

      // message header
      HashMap<String, Object> header = new HashMap<>();
      if (message.correlationId != null)
        header.put("CorrelationID", new String[]{message.correlationId, String.class.getName()});
      if (message.priority > 0)
        header.put("Priority", new String[]{""+message.priority, Integer.class.getName()});
      header.put("Type", message.jmsType);
      //TODO: other headers prop
      maps.put("header", header);

      Gson gson = new GsonBuilder().create();
      String json = gson.toJson(maps);

      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, "RESTDistribution.sendMessage: json " + json);
      
      // Send next message
      response = client.target(uriSendMsg).request().accept(MediaType.TEXT_PLAIN).post( 
          Entity.entity(json, MediaType.APPLICATION_JSON));
    }
    return response.getStatus();
  }
  
  private int closeProducer() {
    // close the producer
    if (uriCloseProducer == null) {
      return 400;
    }
    Response response = client.target(uriCloseProducer).request().accept(MediaType.TEXT_PLAIN).delete();
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "RESTDistribution:: close-producer = " + response.getStatus());
    return response.getStatus();
  }

  private void print(Set<Link> links) {
    if (links.isEmpty())
      return;
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "  links :");
    for (Link link : links)
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, "\t" + link.getRel() + " : " + link.getUri());
  }
  
}
