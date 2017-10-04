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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.MessageFormatException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.objectweb.joram.mom.dest.DistributionHandler;
import org.objectweb.joram.shared.DestinationConstants;
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

  private String hostName = "localhost";
  private int port = 8989;

  private int connectTimeout = 10000;
  private int readTimeout = 10000;
  
  private String userName = "anonymous";
  private String password = "anonymous";

  private String destName = null;
  
  private String idleTimeout = "60";

  private String prodName = null;
  private String clientId = null;

  private Client client;
  
  private URI uriSendNextMsg = null;
  private URI uriCloseProducer = null;
  
  public void init(Properties properties, boolean firstTime) {
    if (properties.containsKey(DestinationConstants.REST_HOST_PROP)) {
      hostName = properties.getProperty(DestinationConstants.REST_HOST_PROP);
    } else {
      logger.log(BasicLevel.WARN,
          "Missing property " + DestinationConstants.REST_HOST_PROP + ", use default value: " + hostName);
    }
    if (properties.containsKey(DestinationConstants.REST_PORT_PROP)) {
      try {
        port = Integer.parseInt(properties.getProperty(DestinationConstants.REST_PORT_PROP));
      } catch (NumberFormatException exc) {
        logger.log(BasicLevel.ERROR,
            "Property " + DestinationConstants.REST_PORT_PROP + " could not be parsed properly, use default value: " + port, exc);
      }
    } else {
      logger.log(BasicLevel.WARN,
          "Missing property " + DestinationConstants.REST_PORT_PROP + ", use default value: " + port);
    }
    
    if (properties.containsKey(DestinationConstants.REST_USERNAME_PROP)) {
      userName = properties.getProperty(DestinationConstants.REST_USERNAME_PROP);
    } else {
      logger.log(BasicLevel.WARN,
          "Missing property " + DestinationConstants.REST_USERNAME_PROP + ", use default value: " + userName);
    }
    if (properties.containsKey(DestinationConstants.REST_PASSWORD_PROP)) {
      password = properties.getProperty(DestinationConstants.REST_PASSWORD_PROP);
    } else {
      logger.log(BasicLevel.WARN,
          "Missing property " + DestinationConstants.REST_PASSWORD_PROP + ", use default value: " + password);
    }

    if (properties.containsKey(DestinationConstants.IDLETIMEOUT_PROP)) {
      try {
        idleTimeout = properties.getProperty(DestinationConstants.IDLETIMEOUT_PROP);
      } catch (NumberFormatException exc) { }
    }

    destName = properties.getProperty(DestinationConstants.DESTINATION_NAME_PROP);
    if (destName == null) {
      logger.log(BasicLevel.ERROR,
          "Missing Destination JNDI name, should fixed property " + DestinationConstants.DESTINATION_NAME_PROP);
    }
  }
  
  private void createProducer(String userName, String password) throws Exception {
    if (uriCloseProducer != null)
      return;

    URI base = UriBuilder.fromUri("http://" + hostName + ":" + port + "/joram/").build();
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG,
          "RestDistribution.createProducer(), use base URI " + base);

    // Initializes URI
    uriSendNextMsg = null;

    // Initializes Rest client and target
    try {
      // It seems that there is no exceptions thrown by these methods.
      client = ClientBuilder.newClient(new ClientConfig() // TODO (AF): Fix these properties with configuration values
          .property(ClientProperties.CONNECT_TIMEOUT, connectTimeout)
          .property(ClientProperties.READ_TIMEOUT, readTimeout));
    } catch (Exception exc) {
      if (logger.isLoggable(BasicLevel.ERROR))
        logger.log(BasicLevel.ERROR,
            "RestDistribution.createProducer(): cannot initialize Rest client", exc);
      throw exc;
    }
    
    // Get the destination
    Response response = null;
    try {
      response = client.target(base).path("jndi").path(destName)
          .request().accept(MediaType.TEXT_PLAIN).head();
    } catch (Exception exc) {
      if (logger.isLoggable(BasicLevel.ERROR))
        logger.log(BasicLevel.ERROR,
            "RestDistribution.createProducer(): cannot get destination " + destName, exc);
      throw exc;
    }
    
    if (response.getStatus() == 201) {
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG,
            "RestDistribution.createProducer(): get destination -> " + response.getStatusInfo());
    } else {
      if (logger.isLoggable(BasicLevel.ERROR))
        logger.log(BasicLevel.ERROR,
            "RestDistribution.createProducer(): cannot get destination " + destName + " -> " + response.getStatusInfo());
      throw new Exception("Cannot get destination " + destName);
    }

    try {
      URI uriCreateProducer = response.getLink("create-producer").getUri();
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG,
            "RestDistribution.createProducer(): create-producer = " + uriCreateProducer);

      // TODO (AF): We should fix name and client-id to be unique.
      WebTarget target = client.target(uriCreateProducer);
      if (prodName != null)  target = target.queryParam("name", prodName);
      if (clientId != null)  target = target.queryParam("client-id", clientId);
      if (userName != null) target = target.queryParam("user", userName);
      if (password != null) target = target.queryParam("password", password);
      if (idleTimeout != null)  target = target.queryParam("idle-timeout", idleTimeout);

      response  = target.request().accept(MediaType.TEXT_PLAIN).post(null);
    } catch (Exception exc) {
      if (logger.isLoggable(BasicLevel.ERROR))
        logger.log(BasicLevel.ERROR,
            "RestDistribution.createProducer(): cannot create producer", exc);
      throw exc;
    }
      
    if (response.getStatus() == 201) {
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG,
            "RestDistribution.createProducer(): create producer -> " + response.getStatusInfo());

      uriCloseProducer = response.getLink("close-context").getUri();
      uriSendNextMsg = response.getLink("send-next-message").getUri();
    } else {
      if (logger.isLoggable(BasicLevel.ERROR))
        logger.log(BasicLevel.ERROR,
            "RestDistribution.createProducer(): cannot create producer -> " + response.getStatusInfo());
      throw new Exception("Cannot create producer");
    }
  }

  public void distribute(Message message) throws Exception {
  	if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "RESTDistribution.distribute(" + message + ')');
    
  	if (destName == null) {
  	  logger.log(BasicLevel.ERROR,
  	      "Missing Destination JNDI name, should fixed property " + DestinationConstants.DESTINATION_NAME_PROP);
  	  throw new Exception("Missing Destination JNDI name, should fixed property " + DestinationConstants.DESTINATION_NAME_PROP);
  	}

  	try {
  	  createProducer(userName, password);
    } catch (Exception exc) {
      if (logger.isLoggable(BasicLevel.ERROR))
        logger.log(BasicLevel.ERROR, "RestDistribution.distribute(): Cannot create producer", exc);
      throw exc;
    }
  	
  	try {
      sendNextMessage(message);
    } catch (Exception e) {
      closeProducer();
      throw e;
    }
  }

  public void close() {
    if (logger.isLoggable(BasicLevel.WARN))
      logger.log(BasicLevel.WARN, "RestDistribution.close()");
    
    closeProducer();
  }
  
  private Map getMapMessageToJsonBodyMap(Message message) throws JMSException {
    Map msgMap = null;
    ByteArrayInputStream bais = null;
    ObjectInputStream ois = null;
    try {
      bais = new ByteArrayInputStream(message.getBody());
      ois = new ObjectInputStream(bais);
      msgMap = (Map) ois.readObject();
    } catch (Exception exc) {
      MessageFormatException jE =
        new MessageFormatException("Error while getting the body.");
      jE.setLinkedException(exc);
      throw jE;
    } finally {
      try {
        ois.close();
      } catch (IOException exc) {}
      try {
        bais.close();
      } catch (IOException exc) {}
    }
    if (msgMap == null)
      return null;
    HashMap<String, Object> jsonBodyMap = new HashMap<>();
    Iterator<Map.Entry> entries = msgMap.entrySet().iterator();
    while (entries.hasNext()) {
      Entry entry = entries.next();
      String key = (String) entry.getKey();
      Object v = entry.getValue();
      if (v != null) {
        String[] value = new String[2];
        if (v instanceof byte[]) {
          try {
            value[0] = new String((byte[])v, "UTF-8");
          } catch (UnsupportedEncodingException e) {
            if (logger.isLoggable(BasicLevel.WARN))
              logger.log(BasicLevel.WARN, "", e);
            MessageFormatException jE =
                new MessageFormatException("Error while encode the bytes to string.");
            jE.setLinkedException(e);
            throw jE;
          }
        } else {
          value[0] = ""+v;
        }
        value[1] = v.getClass().getName();
        jsonBodyMap.put(key, value);
      }
    }
    return jsonBodyMap;
  }
  
  public static final String BytesMessage = "BytesMessage";
  public static final String MapMessage = "MapMessage";
  public static final String TextMessage = "TextMessage";

  private void sendNextMessage(Message message) throws Exception {
    Response response = null;

    if (uriSendNextMsg == null) {
      logger.log(BasicLevel.ERROR, 
          "RestDistribution.sendNextMessage(): URI not initialized.");
      throw new Exception("URI not initialized");
    }
    
    if (message.properties == null && message.type == Message.TEXT) {
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG,
            "RestDistribution.sendNextMessage: Send simple message");

      // If the message contains only text and defines no properties, we can send an
      // optimized message without using JSON.
      
      WebTarget target = client.target(uriSendNextMsg)
          .queryParam("delivery-mode", (message.persistent?Message.PERSISTENT:Message.NON_PERSISTENT))
          .queryParam("priority", message.priority);
      if (message.deliveryTime > 0)
        target = target.queryParam("delivery-time", message.deliveryTime);
      if (message.expiration > 0)
        target = target.queryParam("time-to-live", (message.expiration - System.currentTimeMillis()));
      if (message.correlationId != null && !message.correlationId.isEmpty())
        target = target.queryParam("correlation-id", message.correlationId);
      
      response =  target.request()
          .accept(MediaType.TEXT_PLAIN).post(Entity.entity(message.getText(), MediaType.TEXT_PLAIN));
      
      if (response.getStatus() != 200) {
        if (logger.isLoggable(BasicLevel.ERROR))
          logger.log(BasicLevel.ERROR,
              "RestDistribution.sendNextMessage: cannot send message -> " + response.getStatus());
        throw new Exception("Cannot send message: " + response.getStatus());
      } else {
        if (logger.isLoggable(BasicLevel.DEBUG))
          logger.log(BasicLevel.DEBUG,
              "RestDistribution.sendNextMessage: message sent -> " + response.getStatus());
      }
    } else {
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG,
            "RestDistribution.sendNextMessage: Send JSON message");
      
      // Use JSON to encode the message
      HashMap<String, Object> maps = new HashMap<>();
      
      // Encode message body
      if (message.type == Message.TEXT) {
        maps.put("type", TextMessage);
        maps.put("body", message.getText());
      } else if (message.type == Message.BYTES) {
        maps.put("type", BytesMessage);
        maps.put("body", message.body);
      } else if (message.type == Message.MAP) {
        maps.put("type", MapMessage);
        maps.put("body", getMapMessageToJsonBodyMap(message));
      } else {
        logger.log(BasicLevel.ERROR,
            "RestDistribution.sendNextMessage: type " + message.type + " not yet supported.");
        throw new Exception("Message type " + message.type + " not yet supported.");
      }
      
      // Transform JMS message properties
      if (message.properties != null) {
        HashMap<String, Object> props = new HashMap<>();
        Enumeration e = message.properties.keys();
        while (e.hasMoreElements()) {
          String key = (String) e.nextElement();
          Object value = message.properties.get(key);
          props.put(key, new String[]{value.toString(), value.getClass().getName()});
        }
        maps.put("properties", props);
      }

      // message header
      HashMap<String, Object> header = new HashMap<>();
      if (message.correlationId != null)
        header.put("CorrelationID", new String[]{message.correlationId, String.class.getName()});
      if (message.priority > 0)
        header.put("Priority", new String[]{""+message.priority, Integer.class.getName()});
      // TODO (AF): not correct?
//      header.put("Type", message.jmsType);
          
      //TODO (AF): other headers prop
      maps.put("header", header);

      Gson gson = new GsonBuilder().create();
      String json = gson.toJson(maps);

      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, "RESTDistribution.sendMessage: json " + json);
      
      // Send next message
      response = client.target(uriSendNextMsg).request().accept(MediaType.TEXT_PLAIN).post( 
          Entity.entity(json, MediaType.APPLICATION_JSON));
      
      if (response.getStatus() != 200) {
        if (logger.isLoggable(BasicLevel.ERROR))
          logger.log(BasicLevel.ERROR,
              "RestDistribution.sendNextMessage: cannot send message -> " + response.getStatus());
        throw new Exception("Cannot send message: " + response.getStatus());
      } else {
        if (logger.isLoggable(BasicLevel.DEBUG))
          logger.log(BasicLevel.DEBUG,
              "RestDistribution.sendNextMessage: message sent -> " + response.getStatus());
      }
    }
    
    uriSendNextMsg = response.getLink("send-next-message").getUri();
  }
  
  /**
   * Close the producer.
   */
  private void closeProducer() {
    if (uriCloseProducer != null) {
      Response response = client.target(uriCloseProducer).request().accept(MediaType.TEXT_PLAIN).delete();
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, "RestDistribution.closeProducer(): -> " + response.getStatus());
      uriCloseProducer = null;
    }
  }
}
