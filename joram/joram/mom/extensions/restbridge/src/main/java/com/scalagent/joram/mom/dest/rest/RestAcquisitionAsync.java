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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.objectweb.joram.mom.dest.AcquisitionDaemon;
import org.objectweb.joram.mom.dest.ReliableTransmitter;
import org.objectweb.joram.shared.DestinationConstants;
import org.objectweb.joram.shared.messages.Message;
import org.objectweb.util.monolog.api.BasicLevel;
import org.objectweb.util.monolog.api.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.dyade.aaa.common.Daemon;
import fr.dyade.aaa.common.Debug;

/**
 * Asynchronous acquisition handler implementing the AcquisitionDaemon interface.
 */
public class RestAcquisitionAsync implements AcquisitionDaemon {
  private static final Logger logger = Debug.getLogger(RestAcquisitionAsync.class.getName());
  
  private Properties properties = null;
  private ReliableTransmitter transmitter = null;

  private String hostName = "localhost";
  private int port = 8989;

  private final int connectTimeout = 5000; // Value for jersey.config.client.connectTimeout.
  private final int readTimeout = 5000;    // Base value for jersey.config.client.readTimeout (timeout is added).

  private String userName = "anonymous";
  private String password = "anonymous";
  
  private String destName = null;
  
  private String consName = null;
  private String clientId = null;
  // Normally each consumer resource need to be explicitly closed, this parameter allows to set the idle time
  // in seconds in which the consumer context will be closed if idle.
  private String idleTimeout = "60";
  // Timeout for waiting for a message.
  private String timeout = "10000";
  
  private boolean mediaTypeJson = true; //default true, use "application/json"
  private boolean persistent = true;

  private XDaemon daemon = null;

  @Override
  public void start(Properties properties, ReliableTransmitter transmitter) {
    this.properties = properties;
    this.transmitter = transmitter;
    
    initFromProperties();
    
    if (destName == null) {
      logger.log(BasicLevel.ERROR,
          "Missing Destination JNDI name, should fixed property " + DestinationConstants.DESTINATION_NAME_PROP);
      return;
    }

    daemon = new XDaemon("RestAcquisitionAsync.Daemon-" + destName, logger);
    daemon.start();
  }

  /**
   * Initializes fields from properties.
   */
  private void initFromProperties() {
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

    destName = properties.getProperty(DestinationConstants.DESTINATION_NAME_PROP);
    if (destName == null) {
      logger.log(BasicLevel.ERROR,
          "Missing Destination JNDI name, should fixed property " + DestinationConstants.DESTINATION_NAME_PROP);
    }

    if (properties.containsKey(DestinationConstants.MEDIA_TYPE_JSON_PROP)) {
      mediaTypeJson = Boolean.parseBoolean(properties.getProperty(DestinationConstants.MEDIA_TYPE_JSON_PROP));
    } else {
      logger.log(BasicLevel.WARN,
          "Missing property " + DestinationConstants.MEDIA_TYPE_JSON_PROP + ", use default value: " + mediaTypeJson);
    }
    
    if (properties.containsKey(DestinationConstants.ACQUISITION_PERSISTENT)) {
      persistent = Boolean.parseBoolean(properties.getProperty(DestinationConstants.ACQUISITION_PERSISTENT));
    } else {
      logger.log(BasicLevel.WARN,
          "Missing property " + DestinationConstants.ACQUISITION_PERSISTENT + ", use default value: " + persistent);
    }
    
    if (properties.containsKey(DestinationConstants.TIMEOUT_PROP)) {
      try {
        timeout = properties.getProperty(DestinationConstants.TIMEOUT_PROP);
      } catch (NumberFormatException exc) { }
    }

    if (properties.containsKey(DestinationConstants.IDLETIMEOUT_PROP)) {
      try {
        idleTimeout = properties.getProperty(DestinationConstants.IDLETIMEOUT_PROP);
      } catch (NumberFormatException exc) { }
    }
  }
  
  private URI uriCloseConsumer = null;
  private URI uriReceiveNextMsg = null;
  private URI uriAcknowledgeMsg = null;
  
  Client client = null;

  /**
   * Gets destination and initializes the Rest/JMS consumer.
   */
  private void createConsumer() throws Exception {
    if (uriCloseConsumer != null)
      return;

    URI base = UriBuilder.fromUri("http://" + hostName + ":" + port + "/joram/").build();
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG,
          "RestAcquisitionAsync.createConsumer(), use base URI " + base);
        
    // Initializes URI
    uriAcknowledgeMsg = null;
    uriReceiveNextMsg = null;
    
    // Initializes Rest client and target
    try {
      // It seems that there is no exceptions thrown by these methods.
      client = ClientBuilder.newClient(
          new ClientConfig() // TODO (AF): Fix these properties with configuration values
          .property(ClientProperties.CONNECT_TIMEOUT, connectTimeout)
          .property(ClientProperties.READ_TIMEOUT, Integer.parseInt(timeout) + readTimeout));
    } catch (Exception exc) {
      if (logger.isLoggable(BasicLevel.ERROR))
        logger.log(BasicLevel.ERROR,
            "RestAcquisitionAsync.createConsumer(): cannot initialize Rest client", exc);
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
            "RestAcquisitionAsync.createConsumer(): cannot get destination " + destName, exc);
      throw exc;
    }
    
    if (response.getStatus() == 201) {
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG,
            "RestAcquisitionAsync.createConsumer(): get destination -> " + response.getStatusInfo());
    } else {
      if (logger.isLoggable(BasicLevel.ERROR))
        logger.log(BasicLevel.ERROR,
            "RestAcquisitionAsync.createConsumer(): cannot get destination " + destName + " -> " + response.getStatusInfo());
      throw new Exception("Cannot get destination " + destName);
    }
    
    try {
      // Get URI to create a consumer in ClientAcknowledge mode.
      URI uriCreateConsumer = response.getLink("create-consumer-client-ack").getUri();
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, "RestAcquisitionAsync.createConsumer(): create-consumer = " + uriCreateConsumer);

      // TODO (AF): We should fix name and client-id to be unique.
      WebTarget target = client.target(uriCreateConsumer);
      if (consName != null)  target = target.queryParam("name", consName);
      if (clientId != null)  target = target.queryParam("client-id", clientId);
      // Normally each consumer resource need to be explicitly closed, this parameter allows to set the idle time
      // in seconds in which the consumer context will be closed if idle.
      if (idleTimeout != null)  target = target.queryParam("idle-timeout", idleTimeout);
      if (userName != null) target = target.queryParam("user", userName);
      if (password != null) target = target.queryParam("password", password);

      response = target.request().accept(MediaType.TEXT_PLAIN).post(null);
    } catch (Exception exc) {
      if (logger.isLoggable(BasicLevel.ERROR))
        logger.log(BasicLevel.ERROR,
            "RestAcquisitionAsync.createConsumer(): cannot create consumer", exc);
      throw exc;
    }

    if (response.getStatus() == 201) {
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG,
            "RestAcquisitionAsync.createConsumer(): create consumer -> " + response.getStatusInfo());

      uriCloseConsumer = response.getLink("close-context").getUri();
      uriAcknowledgeMsg = null;
      uriReceiveNextMsg = response.getLink("receive-next-message").getUri();
    } else {
      if (logger.isLoggable(BasicLevel.ERROR))
        logger.log(BasicLevel.ERROR,
            "RestAcquisitionAsync.createConsumer(): cannot create consumer -> " + response.getStatusInfo());
      throw new Exception("Cannot create consumer");
    }
  }

  private Message recvNextMessage() throws Exception {
    Response response = null;
    Message msg = null;
    
    if (uriReceiveNextMsg == null) {
      logger.log(BasicLevel.ERROR, 
          "RestAcquisitionAsync.recvNextMessage(): URI not initialized.");
      throw new Exception("URI not initialized");
    }

    if (mediaTypeJson) {
      response = client.target(uriReceiveNextMsg)
          .queryParam("timeout", timeout)
          .request()
          .accept(MediaType.APPLICATION_JSON)
          .get();
      if (response.getStatus() != 200) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, 
              "RestAcquisitionAsync.receiveNextMessage: cannot receive next message ->" + response.getStatusInfo());
        
        return null;
      }

      String json = response.readEntity(String.class);
      if ((json == null) || json.isEmpty() || "null".equals(json)){
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, 
              "RestAcquisitionAsync.receiveNextMessage: receive empty message.");
        
        return null;
      }
      
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, 
            "RestAcquisitionAsync.receiveNextMessage: receive " + json);
      
      Gson gson = new GsonBuilder().create();
      HashMap<String, Object> map = gson.fromJson(json, HashMap.class);
      if (map != null) {
        try {
          msg = createJSonMessage(map);
        } catch (Exception exc) {
          if (logger.isLoggable(BasicLevel.ERROR))
            logger.log(BasicLevel.ERROR, 
                "RestAcquisitionAsync.receiveNextMessage: cannot create message", exc);

          return null;
        }
      } else {
        String text = gson.fromJson(json, String.class);
        if (text != null) {
          if (logger.isLoggable(BasicLevel.WARN))
            logger.log(BasicLevel.WARN, 
                "RestAcquisitionAsync.receiveNextMessage: not receive a JSon message -> " + json);
          
          try {
            msg = createTextMessage(text);
          } catch (Exception exc) {
            if (logger.isLoggable(BasicLevel.ERROR))
              logger.log(BasicLevel.ERROR, 
                  "RestAcquisitionAsync.receiveNextMessage: cannot create message", exc);

            return null;
          }
        } else {
          if (logger.isLoggable(BasicLevel.WARN))
            logger.log(BasicLevel.WARN, 
                "RestAcquisitionAsync.receiveNextMessage: receive bad message -> " + json);
          
          return null;
        }
      }
    } else {
      response = client.target(uriReceiveNextMsg)
          .queryParam("timeout", timeout)
          .request()
          .accept(MediaType.TEXT_PLAIN)
          .get();
      if (response.getStatus() != 200) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, 
              "RestAcquisitionAsync.receiveNextMessage: cannot receive next message ->" + response.getStatusInfo());
        
        return null;
      }

      String text = response.readEntity(String.class);
      if ((text == null) || text.isEmpty()) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, 
              "RestAcquisitionAsync.receiveNextMessage: receive empty message.");
        
        return null;
      }
      
      try {
        msg = createTextMessage(text);
      } catch (Exception exc) {
        if (logger.isLoggable(BasicLevel.ERROR))
          logger.log(BasicLevel.ERROR, 
              "RestAcquisitionAsync.receiveNextMessage: cannot create message", exc);

        return null;
      }
    }
    
    uriReceiveNextMsg = response.getLink("receive-next-message").getUri();
    uriAcknowledgeMsg = response.getLink("acknowledge-message").getUri();

    return msg;
  }
  
  private Object getPropertyValue(String type, String value) {
    switch (type) {
    case "java.lang.Boolean":
      return Boolean.valueOf(value);
    case "java.lang.Byte":
      return Byte.valueOf(value);
    case "java.lang.Short":
      return Short.valueOf(value);
    case "java.lang.Integer":
      return Integer.valueOf(value);
    case "java.lang.Long":
      return Long.valueOf(value);
    case "java.lang.Float":
      return Float.valueOf(value);
    case "java.lang.Double":
      return Double.valueOf(value);
    case "java.lang.String":
      return value;
    default:
      if (logger.isLoggable(BasicLevel.WARN))
        logger.log(BasicLevel.WARN,
            "RESTAcquisitionAsync.getPropertyValue: unknown property type: " + type);
    }
    return null;
  }
  
  private void setJMSMessageHeader(Message msg, Map header) {
    if (header == null || (header.size() == 0))  {
      if (logger.isLoggable(BasicLevel.WARN))
        logger.log(BasicLevel.WARN,
            "RESTAcquisitionAsync.setJMSMessageHeader: empty JMS header map");
      return;
    }

    if (header.containsKey("DeliveryMode")) {
      try {
        msg.persistent = "PERSISTENT".equals(header.get("DeliveryMode"));
      } catch (Exception e) { 
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN,
              "RESTAcquisitionAsync.setJMSMessageHeader -- DeliveryMode = " + header.get("DeliveryMode"));
      }
    }

    if (header.containsKey("Priority")) {
      try {
        msg.priority = ((Double) header.get("Priority")).intValue();
      } catch (Exception e) { 
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN,
              "RESTAcquisitionAsync.setJMSMessageHeader -- Priority = " + header.get("Priority"));
      }
    }

    if (header.containsKey("Redelivered")) {
      try {
        msg.redelivered = (boolean) header.get("Redelivered");
      } catch (Exception e) { 
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN,
              "RESTAcquisitionAsync.setJMSMessageHeader-- Redelivered = " + header.get("Redelivered"));
      }
    }

    if (header.containsKey("Timestamp")) {
      try {
        msg.timestamp = ((Double) header.get("Timestamp")).longValue();
      } catch (Exception e) { 
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN,
              "RESTAcquisitionAsync.setJMSMessageHeader-- Timestamp = " + header.get("Timestamp"));
      }
    }

    if (header.containsKey("Expiration")) {
      try {
        msg.expiration = ((Double) header.get("Expiration")).longValue();
      } catch (Exception e) { 
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN,
              "RESTAcquisitionAsync.setJMSMessageHeader -- Expiration = " + header.get("Expiration"));
      }
    }

    if (header.containsKey("CorrelationID")) {
      try {
        msg.correlationId = (String) header.get("CorrelationID");
      } catch (Exception e) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN,
              "RESTAcquisitionAsync.setJMSMessageHeader -- CorrelationID = " + header.get("CorrelationID"));
      }
    }

    if (header.containsKey("CorrelationIDAsBytes")) {
      msg.setJMSCorrelationIDAsBytes((byte[]) header.get("CorrelationIDAsBytes"));
      if (logger.isLoggable(BasicLevel.WARN))
        logger.log(BasicLevel.DEBUG, "-- CorrelationIDAsBytes = " + header.get("CorrelationIDAsBytes"));
    }

    // TODO (AF): Is it correct? The destination should correspond to the current destination.
    if (header.containsKey("Destination")) {
      try {
        Map dest = (Map) header.get("Destination");
        String id = (String) dest.get("agentId");
        String name = (String) dest.get("adminName");
        byte type = ((Double) dest.get("type")).byteValue();
        msg.setDestination(id, name, type);
      } catch (Exception e) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, "-- Destination = " + header.get("Destination"));
      }
    }

    if (header.containsKey("MessageID")) {
      try {
        msg.id = (String) header.get("MessageID");
      } catch (Exception e) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN,
              "RESTAcquisitionAsync.setJMSMessageHeader -- MessageID = " + header.get("MessageID"));
      }
    }

    if (header.containsKey("ReplyTo")) {
      try {
        msg.replyToId = (String) header.get("ReplyTo");
      } catch (Exception e) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN,
              "RESTAcquisitionAsync.setJMSMessageHeader -- ReplyTo = " + header.get("ReplyTo"));
      }
    }

    if (header.containsKey("Type")) {
      try {
        msg.jmsType = (String) header.get("Type");
      } catch (Exception e) { 
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN,
              "RESTAcquisitionAsync.setJMSMessageHeader -- Type = " + header.get("Type"));
      }
    }
  }

  private void setJMSProperties(Message msg, Map props) {
    if (props != null && props.size() > 0) {
      Set<Map.Entry> entrySet = props.entrySet();
      for (Map.Entry entry : entrySet) {
        String key = (String) entry.getKey();
        ArrayList pair = (ArrayList) entry.getValue();
        Object value = getPropertyValue((String) pair.get(1), (String)pair.get(0));

        if (key != null && value != null) {
          msg.setProperty(key, value);
        } else {
          if (logger.isLoggable(BasicLevel.WARN))
            logger.log(BasicLevel.WARN,
                "RESTAcquisitionAsync.setProperties: bad property <" + key + ", " + value + ">");
        }
      }
    }    
  }
  
  // TODO (AF): To verify
  private Map<String, Object> getMapMessage(Map<String, Object> jsonMap) {
    Map<String, Object> map = new HashMap<String, Object>();

    // parse the json map
    for (String key : jsonMap.keySet()) {
      ArrayList<String> array = null;
      try {
        array = (ArrayList<String>) jsonMap.get(key);
      } catch (Exception exc) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN,
              "RESTAcquisitionAsync.getMapMessage: bad element, ignore map entry " + key, exc);
        continue;
      }

      if (array.size() != 2) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN,
              "RESTAcquisitionAsync.getMapMessage: bad element, ignore map entry " + key);
        continue;
      }
      
      String classname = array.get(1);
      try {
        Object value = null;
        if (Character.class.getName().equals(classname)) {
          value =  array.get(0).charAt(0);
        } else if (byte[].class.getName().equals(classname)) {
          value = array.get(0).getBytes("UTF-8");
        } else {
          Constructor<?> constructor = Class.forName(classname).getConstructor(String.class);
          value = constructor.newInstance(array.get(0));
        }
        map.put(key, value);
      } catch (Exception exc) {
        if (logger.isLoggable(BasicLevel.ERROR))
          logger.log(BasicLevel.ERROR,
              "RESTAcquisitionAsync.getMapMessage: ignore map entry " + key + ", " + array.get(0) + " / " + classname, exc);
        continue;
      }


      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG,
            "RESTAcquisitionAsync.getMapMessage: get key=" + key + ", value = " + array.get(0) + " / " + classname);
    }
    
    return map;
  }

  private Message createJSonMessage(HashMap<String, Object> map) throws Exception {
    Message msg = new Message();

    // Get JMS header
    setJMSMessageHeader(msg, (Map) map.get("header"));

    // Get JMS properties
    setJMSProperties(msg, (Map) map.get("properties"));

    if (msg.id == null) {
      if (logger.isLoggable(BasicLevel.WARN))
        logger.log(BasicLevel.WARN,
            "RESTAcquisitionAsync.createJSonMessage: message unique identifier not set");
      // TODO (AF): Should set a unique message identifier
      msg.id = uriReceiveNextMsg.getPath();
    }
    
    // Get message body
    String type = (String) map.get("type");
    Object body = map.get("body");
    if (body == null ) {
      if (logger.isLoggable(BasicLevel.ERROR))
        logger.log(BasicLevel.ERROR,
            "RESTAcquisitionAsync.createJSonMessage: no body definition for the message");
      throw new Exception("Cannot convert message");
    }
    
    switch (type) {
    case "TextMessage": {
      msg.type = Message.TEXT;
      if (body instanceof String) {
        try {
          msg.setText((String) body);
          return msg;
        } catch (Exception exc) {
          if (logger.isLoggable(BasicLevel.ERROR))
            logger.log(BasicLevel.ERROR,
                "RESTAcquisitionAsync.createJSonMessage: error setting message body", exc);
          throw new Exception("Cannot convert message", exc);
        }
      } else {
        if (logger.isLoggable(BasicLevel.ERROR))
          logger.log(BasicLevel.ERROR,
              "RESTAcquisitionAsync.createJSonMessage: body of TextMessage should not be " + body.getClass());
        throw new Exception("Cannot convert message");
      }
    }
    case "MapMessage": {
      if (body instanceof Map) {
        msg.type = Message.MAP;
        try {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(baos);
          oos.writeObject(getMapMessage((Map) body));
          oos.flush();
          msg.setBody(baos.toByteArray());
          oos.close();
          baos.close();
          return msg;
        } catch (Exception exc) {
          if (logger.isLoggable(BasicLevel.ERROR))
            logger.log(BasicLevel.ERROR,
                "RESTAcquisitionAsync.createJSonMessage: error serializing message body", exc);
          throw new Exception("Cannot convert message", exc);
        }
      } else {
        if (logger.isLoggable(BasicLevel.ERROR))
          logger.log(BasicLevel.ERROR,
              "RESTAcquisitionAsync.createJSonMessage: body of MapMessage should not be " + body.getClass());
        throw new Exception("Cannot convert message");
      }
    }
    case "BytesMessage": {
      if (body instanceof ArrayList) {
        ArrayList jmsBody =  (ArrayList) body;
        msg.type = Message.BYTES;
        byte[] bytes = new byte[jmsBody.size()];
        try {
          for (int i = 0; i < bytes.length; i++) {
            bytes[i] = ((Number) jmsBody.get(i)).byteValue();
          }
        } catch (Exception exc) {
          if (logger.isLoggable(BasicLevel.ERROR))
            logger.log(BasicLevel.ERROR,
                "RESTAcquisitionAsync.createJSonMessage: error converting BytesMessage", exc);
          throw new Exception("Cannot convert message", exc);
        }
        msg.body = bytes;
        return msg;
      } else {
        if (logger.isLoggable(BasicLevel.ERROR))
          logger.log(BasicLevel.ERROR,
              "RESTAcquisitionAsync.createJSonMessage: body of BytesMessage should not be " + body.getClass());
        throw new Exception("Cannot convert message");
      }
    }
    default:
      logger.log(BasicLevel.ERROR,
          "RESTAcquisitionAsync.createJSonMessage: type " + type + " not supported.");
      throw new Exception("Cannot convert message");
    }
  }

  private Message createTextMessage(String text) throws Exception {
    Message msg = new Message();
    msg.type = Message.TEXT;
    msg.id = uriReceiveNextMsg.getPath();
    msg.persistent = persistent;
    msg.setText(text);
    return msg;
  }
  
  void acknowledgeMessage() throws Exception {
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, 
          "RestAcquisitionAsync.acknowledgeMessage: " + uriAcknowledgeMsg);
    
    if (uriAcknowledgeMsg != null) {
      Response response = client.target(uriAcknowledgeMsg)
          .request()
          .accept(MediaType.TEXT_PLAIN)
          .delete();

      if (response.getStatus() != 200) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, 
              "RestAcquisitionAsync.acknowledgeMessage: error during acknowledge ->" + response.getStatusInfo());
        
        throw new Exception("Error during acknowledge: "+ response.getStatusInfo());
      }
    } else {
      if (logger.isLoggable(BasicLevel.WARN))
        logger.log(BasicLevel.WARN, 
            "RestAcquisitionAsync.acknowledgeMessage: Cannot acknowledge message, empty URI.");
      
      throw new Exception("Error during acknowledge: empty URI");
    }
    uriAcknowledgeMsg = null;
  }

  private void closeConsumer() {
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "RestAcquisitionAsync.closeConsumer():");
    
    if (uriCloseConsumer != null) {
      try {
        Response response = client.target(uriCloseConsumer).request().accept(MediaType.TEXT_PLAIN).delete();
        if (logger.isLoggable(BasicLevel.DEBUG))
          logger.log(BasicLevel.DEBUG, "RestAcquisitionAsync.closeConsumer(): -> " + response.getStatus());
      } catch (Exception exc) {
        logger.log(BasicLevel.WARN, "RestAcquisitionAsync.closeConsumer()", exc);
        return;
      } finally {
        uriCloseConsumer = null;
      }
    }    
  }
  
  @Override
  public void stop() {
    if ((daemon != null) && daemon.isRunning())
      daemon.stop();
    daemon = null;
  }

  private class XDaemon extends Daemon {

    protected XDaemon(String name, Logger logmon) {
      super(name, logmon);
      setDaemon(true);
    }

    @Override
    public void run() {
      if (logger.isLoggable(BasicLevel.INFO))
        logger.log(BasicLevel.INFO, "RestAcquisitionAsync.Daemon.run(): starting");
      
      try {
        while (running) {
          canStop = false;
          try {
            createConsumer();
          } catch (Exception exc) {
            if (logger.isLoggable(BasicLevel.ERROR))
              logger.log(BasicLevel.ERROR, "RestAcquisitionAsync.Daemon.run()", exc);

            try {
              canStop = true;
              // TODO (AF): Handles this pause smartly.
              Thread.sleep(1000L);
            } catch (InterruptedException ie) {}
            canStop = false;
            continue;
          }

          Message msg = null;
          try {
            if (logger.isLoggable(BasicLevel.DEBUG))
              logger.log(BasicLevel.DEBUG, "RestAcquisitionAsync.Daemon.run(): waits for new message.");

            msg = recvNextMessage();
          } catch (Exception exc) {
            if (logger.isLoggable(BasicLevel.ERROR))
              logger.log(BasicLevel.ERROR, "RestAcquisitionAsync.Daemon.run()", exc);

            closeConsumer();
            continue;
          }

          if (msg == null) continue;

          if (logger.isLoggable(BasicLevel.DEBUG))
            logger.log(BasicLevel.DEBUG, "RestAcquisitionAsync.Daemon.run(): receives " + msg.id);

          try {
            transmitter.transmit(msg, msg.id);

            if (logger.isLoggable(BasicLevel.DEBUG))
              logger.log(BasicLevel.DEBUG, "RestAcquisitionAsync.Daemon.run(): " + msg.id + " transmitted");

          } catch (Exception exc) {
            // TODO (AF): Error during transmit ??
            if (logger.isLoggable(BasicLevel.ERROR))
              logger.log(BasicLevel.ERROR, "RestAcquisitionAsync.Daemon.run()", exc);

            closeConsumer();
            continue;
          }

          try {
            if (logger.isLoggable(BasicLevel.DEBUG))
              logger.log(BasicLevel.DEBUG, "RestAcquisitionAsync.Daemon.run(): acknowledges message.");

            acknowledgeMessage();
          } catch (Exception exc) {
            if (logger.isLoggable(BasicLevel.ERROR))
              logger.log(BasicLevel.ERROR, "RestAcquisitionAsync.Daemon.run()", exc);

            closeConsumer();
            continue;
          }
        }
      } catch (Throwable exc) {
        if (logger.isLoggable(BasicLevel.ERROR))
          logger.log(BasicLevel.ERROR, "RestAcquisitionAsync.Daemon.run()", exc);
      } finally {
        if (logger.isLoggable(BasicLevel.INFO))
          logger.log(BasicLevel.INFO, "RestAcquisitionAsync.Daemon.run(): finishing", new Exception());
        finish();
      }
    }
    
    @Override
    public synchronized void stop() {
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, "RestAcquisitionAsync.Daemon.stop()", new Exception());
      super.stop();
    }

    @Override
    protected void close() {
      closeConsumer();
    }

    @Override
    protected void shutdown() {
      // TODO (AF): force jersey to close
      close();
    }
  }
}
