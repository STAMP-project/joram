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
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jms.MessageFormatException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.objectweb.joram.mom.dest.AcquisitionDaemon;
import org.objectweb.joram.mom.dest.ReliableTransmitter;
import org.objectweb.joram.shared.DestinationConstants;
import org.objectweb.joram.shared.excepts.MessageValueException;
import org.objectweb.joram.shared.messages.ConversionHelper;
import org.objectweb.joram.shared.messages.Message;
import org.objectweb.util.monolog.api.BasicLevel;
import org.objectweb.util.monolog.api.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.dyade.aaa.common.Daemon;
import fr.dyade.aaa.common.Debug;

public class RESTAcquisitionDaemon implements AcquisitionDaemon {
  private static final Logger logger = Debug.getLogger(RESTAcquisitionDaemon.class.getName());
  
  private static final String HOST_PROP = "rest.hostName";
  private static final String PORT_PROP = "rest.port";
  private static final String USER_NAME_PROP = "rest.userName";
  private static final String PASSWORD_PROP = "rest.password";
  private static final String TIMEOUT_PROP = "rest.timeout";
  private static final String NB_MAX_MSG_PROP = "rest.nbMaxMsgByPeriode";
  private static final String MEDIA_TYPE_JSON_PROP = "rest.mediaTypeJson";
  private static final String DESTINATION_NAME_PROP = "jms.destination";
  
  private ReliableTransmitter transmitter;
  private RestConsumer restConsumer;
  private ReconnectDaemon reconnectDaemon;
  
  private String hostName = "localhost";
  private int port = 8989;
  private Client client;
  private WebTarget target;
  private String userName = null;
  private String password = null;
  private long timeout = 100; //timeout = 0 => NoWait
  private boolean persistent = false;
  private int nbMaxMsg = 10;
  private boolean mediaTypeJson = true;//default true "application/json"
  
  private String destName;
  private URI uriCreateConsumer;
  private URI uriConsume;
  private URI uriCloseConsumer;
  
  private Properties properties;
  private int reconnectSleep = 1000;
  
  @Override
  public void start(Properties properties, ReliableTransmitter transmitter) {
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "Start RESTAcquisitionDaemon.");

    this.transmitter = transmitter;
    this.properties = properties;
    reconnectDaemon = new ReconnectDaemon();
    reconnectDaemon.start();
  }

  @Override
  public void stop() {
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "Stop RESTAcquisitionDaemon.");

    if (restConsumer != null && restConsumer.isRunning())
      restConsumer.stop();
    
    if (reconnectDaemon != null && reconnectDaemon.isRunning()) {
      reconnectDaemon.stop();
    } else {
      reconnectDaemon = null;
    }
  }

  public boolean init(Properties properties) {
    destName = properties.getProperty(DESTINATION_NAME_PROP);
    if (destName == null) {
      throw new IllegalArgumentException("Missing Destination JNDI name.");
    }
    if (properties.containsKey(HOST_PROP)) {
      hostName = properties.getProperty(HOST_PROP);
    }

    if (properties.containsKey(PORT_PROP)) {
      try {
        port = Integer.parseInt(properties.getProperty(PORT_PROP));
      } catch (NumberFormatException nfe) {
        logger.log(BasicLevel.ERROR, "Property " + PORT_PROP
            + " could not be parsed properly, use default value.", nfe);
      }
    }
    if (properties.containsKey(USER_NAME_PROP)) {
      userName = properties.getProperty(USER_NAME_PROP);
    }
    if (properties.containsKey(PASSWORD_PROP)) {
      password = properties.getProperty(PASSWORD_PROP);
    }
    if (properties.containsKey(TIMEOUT_PROP)) {
      try {
        timeout = Long.parseLong(properties.getProperty(TIMEOUT_PROP));
      } catch (NumberFormatException exc) { }
    }
    if (properties.containsKey(NB_MAX_MSG_PROP)) {
      try {
        nbMaxMsg = Integer.parseInt(properties.getProperty(NB_MAX_MSG_PROP));
      } catch (NumberFormatException exc) { }
    }

    uriCreateConsumer = null;
    uriCloseConsumer = null;
    // init client and target
    try {
      ClientConfig config = new ClientConfig();
      client = ClientBuilder.newClient(config);
      target = client.target(UriBuilder.fromUri("http://" + hostName + ":" + port + "/joram/").build());
    } catch (Exception e) {
      if (logger.isLoggable(BasicLevel.ERROR))
        logger.log(BasicLevel.ERROR, "Exception:: RESTAcquisitionDaemon.init " + e.getMessage());
      return false;
    }

    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "RESTAcquisitionDaemon.init target = " + target.getUri());

    // create the consumer
    if (uriCreateConsumer != null && uriCloseConsumer != null) {
      client.target(uriCloseConsumer).request().accept(MediaType.TEXT_PLAIN).delete();
      uriCreateConsumer = null;
    }
    if (properties.containsKey(MEDIA_TYPE_JSON_PROP)) {
      mediaTypeJson = Boolean.parseBoolean(properties.getProperty(MEDIA_TYPE_JSON_PROP));
    }

    if (properties.containsKey(DestinationConstants.ACQUISITION_PERSISTENT)) {
      try {
        persistent = ConversionHelper.toBoolean(properties.get(DestinationConstants.ACQUISITION_PERSISTENT));
      } catch (MessageValueException e) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, "", e);
      }
    }

    Builder builder = target.path("jndi").path(destName).request();
    Response response = builder.accept(MediaType.TEXT_PLAIN).head();
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "RESTAcquisition.createConsumer: response = " + response);
    if (201 != response.getStatus()) {
      return false;
    }

    uriCreateConsumer = response.getLink("create-consumer").getUri();
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "RESTAcquisition.createConsumer: uriCreateConsumer = " + uriCreateConsumer);

    WebTarget wTarget = client.target(uriCreateConsumer)
        .queryParam("name", "cons-" + destName)
        .queryParam("client-id", "id-" + destName);
    if (userName != null)
      wTarget = wTarget.queryParam("user", userName);
    if (password != null)
      wTarget = wTarget.queryParam("password", password);
    response = wTarget.request().accept(MediaType.TEXT_PLAIN).post(null);
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "RESTAcquisition.createConsumer: response = " + response);
    if (201 != response.getStatus()) {
      return false;
    }
    uriCloseConsumer = response.getLink("close-context").getUri();
    uriConsume = response.getLink("receive-message").getUri();
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "RESTAcquisition.createConsumer: uriCloseConsumer = " + uriCloseConsumer + ", uriConsume = " + uriConsume);

    restConsumer = new RestConsumer();
    restConsumer.start();
    return true;
  }

  private void setMessageHeader(Map jsonMessageHeader, Message message) {
    if (jsonMessageHeader.containsKey("DeliveryMode"))
      try {
        message.persistent = "PERSISTENT".equals(jsonMessageHeader.get("DeliveryMode"));
      } catch (Exception e) { 
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, "-- DeliveryMode = " + jsonMessageHeader.get("DeliveryMode"));
      }

    if (jsonMessageHeader.containsKey("Priority")) {
      try {
        message.priority = ((Double) jsonMessageHeader.get("Priority")).intValue();
      } catch (Exception e) { 
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, "-- Priority = " + jsonMessageHeader.get("Priority"));
      }
    }

    if (jsonMessageHeader.containsKey("Redelivered")) {
      try {
        message.redelivered = (boolean) jsonMessageHeader.get("Redelivered");
      } catch (Exception e) { 
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, "-- Redelivered = " + jsonMessageHeader.get("Redelivered"));
      }
    }

    if (jsonMessageHeader.containsKey("Timestamp")) {
      try {
        message.timestamp = ((Double) jsonMessageHeader.get("Timestamp")).longValue();
      } catch (Exception e) { 
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, "-- Timestamp = " + jsonMessageHeader.get("Timestamp"));
      }
    }

    if (jsonMessageHeader.containsKey("Expiration")) {
      try {
        message.expiration = ((Double) jsonMessageHeader.get("Expiration")).longValue();
      } catch (Exception e) { 
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, "-- Expiration = " + jsonMessageHeader.get("Expiration"));
      }
    }

    if (jsonMessageHeader.containsKey("CorrelationID"))
      try {
        message.correlationId = (String) jsonMessageHeader.get("CorrelationID");
      } catch (Exception e) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, "-- CorrelationID = " + jsonMessageHeader.get("CorrelationID"));
      }

    if (jsonMessageHeader.containsKey("CorrelationIDAsBytes")) {
      // TODO "CorrelationIDAsBytes"
      if (logger.isLoggable(BasicLevel.WARN))
        logger.log(BasicLevel.WARN, "-- TODO CorrelationIDAsBytes");
    }

    if (jsonMessageHeader.containsKey("Destination")) {
      try {
        Map dest = (Map) jsonMessageHeader.get("Destination");
        String id = (String) dest.get("agentId");
        String name = (String) dest.get("adminName");
        byte type = ((Double) dest.get("type")).byteValue();
        message.setDestination(id, name, type);
      } catch (Exception e) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, "-- Destination = " + jsonMessageHeader.get("Destination"));
      }
    }

    if (jsonMessageHeader.containsKey("MessageID"))
      try {
        message.id = (String) jsonMessageHeader.get("MessageID");
      } catch (Exception e) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, "-- MessageID = " + jsonMessageHeader.get("MessageID"));
      }

    if (jsonMessageHeader.containsKey("ReplyTo"))
      try {
        message.replyToId = (String) jsonMessageHeader.get("ReplyTo");
      } catch (Exception e) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, "-- ReplyTo = " + jsonMessageHeader.get("ReplyTo"));
      }

    if (jsonMessageHeader.containsKey("Type")) {
      try {
        message.jmsType = (String) jsonMessageHeader.get("Type");
      } catch (Exception e) { 
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, "-- Type = " + jsonMessageHeader.get("Type"));
      }
    }
  }
  
  private Map getMapMessage(Map<String, Object> jsonMap) throws Exception {
    if (jsonMap == null)
      return null;

    Map map = new HashMap<>();
    // parse the json map
    for (String key : jsonMap.keySet()) {
      Object value = jsonMap.get(key);
      if (value instanceof ArrayList) {
        ArrayList<String> array =(ArrayList<String>) value; 
        try {
          if (array.size() == 2) {
            String className = array.get(1);
            if (Character.class.getName().equals(className)) {
              value =  array.get(0).charAt(0);
            } else if (byte[].class.getName().equals(className)) {
             value = array.get(0).getBytes("UTF-8");
            } else {
              Constructor<?> constructor = Class.forName(className).getConstructor(String.class);
              value = constructor.newInstance(array.get(0));
            }
            map.put(key, value);
          }
        } catch (Exception e) {
          if (logger.isLoggable(BasicLevel.ERROR))
            logger.log(BasicLevel.ERROR, "getMapMessage: ignore map entry " + key + ", " + value + " : " + e.getMessage());
          continue;
        }
      }
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, "getMapMessage: " + key + ", value = " + value + ", " + value.getClass().getSimpleName());
    }
    return map;
  }

  public ArrayList<Message> receiveRestMessages() { 

    if (uriConsume == null)
      return null;

    ArrayList<Message> messages = new ArrayList<>();

    try {
    while (messages.size() < nbMaxMsg) {
      //timeout = 0 => NoWait
      Builder builder = null;
      Message message = null;
      if (!mediaTypeJson) {
        //TEXT
        builder = client.target(uriConsume)
            .queryParam("timeout", timeout)
            .request().accept(MediaType.TEXT_PLAIN);
        Response response = builder.get();
        String msg = response.readEntity(String.class);
        if (200 != response.getStatus() || msg == null || msg.isEmpty())
          break;
        try {
          message = new Message();
          message.type = Message.TEXT;
          message.setText(msg);
        } catch (Exception e) {
          if (logger.isLoggable(BasicLevel.WARN))
            logger.log(BasicLevel.WARN, "RESTAcquisitionDaemon.receiveRestMessages: msg = " + msg);
          message = null;
        }

      } else {
        //JSON
        builder = client.target(uriConsume)
            .queryParam("timeout", timeout)
            .request().accept(MediaType.APPLICATION_JSON);

        Response response = builder.get();

        //        if (logger.isLoggable(BasicLevel.DEBUG))
        //          logger.log(BasicLevel.DEBUG, "RESTAcquisition.retrieve response = " + response);
        String json = response.readEntity(String.class);

        if (200 != response.getStatus() || json == null || json.isEmpty() || "null".equals(json))
          break;

        try {
          Gson gson = new GsonBuilder().create();
          HashMap<String, Object> msg = gson.fromJson(json, HashMap.class);
          if (msg == null) {
            String text = gson.fromJson(json, String.class);
            if (text != null) {
              message = new Message();
              message.type = Message.TEXT;
              message.setText(text);
            }
          } else {

            if (logger.isLoggable(BasicLevel.DEBUG))
              logger.log(BasicLevel.DEBUG, "RESTAcquisitionDaemon.receiveRestMessages: msg = " + msg);

            message = new Message();
            //get Properties
            Map jmsProperties = (Map) msg.get("properties");
            if (jmsProperties != null && jmsProperties.size() > 0) {
              Set<Map.Entry> entrySet = jmsProperties.entrySet();
              for (Map.Entry entry : entrySet) {
                String key = (String) entry.getKey();
                ArrayList value = (ArrayList) entry.getValue();
                String propType = (String) value.get(1);
                Object propValue = null;
                switch (propType) {
                case "java.lang.Boolean":
                  propValue = Boolean.parseBoolean((String)value.get(0));
                  break;
                case "java.lang.Byte":
                  propValue = Byte.parseByte((String)value.get(0));
                  break;
                case "java.lang.Short":
                  propValue = Short.parseShort((String)value.get(0));
                  break;
                case "java.lang.Integer":
                  propValue = Integer.parseInt((String)value.get(0));
                  break;
                case "java.lang.Long":
                  propValue = Long.parseLong((String)value.get(0));
                  break;
                case "java.lang.Float":
                  propValue = Float.parseFloat((String)value.get(0));
                  break;
                case "java.lang.Double":
                  propValue = Double.parseDouble((String)value.get(0));
                  break;
                case "java.lang.String":
                  propValue = (String)value.get(0);
                  break;

                default:
                  if (logger.isLoggable(BasicLevel.WARN))
                    logger.log(BasicLevel.WARN, "RESTAcquisitionDaemon.receiveRestMessages: property type not supported: " + propType);
                  break;
                }
                if (key != null && propValue != null)
                  message.setProperty(key, propValue);
              }
            }

            //Get Header
            Map jmsHeader = (Map) msg.get("header");
            if (jmsHeader != null && jmsHeader.size() > 0) {
              setMessageHeader(jmsHeader, message);
            }

            //Get body
            String type = (String) msg.get("type");
            message.jmsType = type;
            switch (type) {
            case "TextMessage": {
              String jmsBody =  (String) msg.get("body");
              message.type = Message.TEXT;
              message.setText(jmsBody);
            } break;

            case "MapMessage": {
              Map jsonBody =  (Map) msg.get("body");
              Map map = getMapMessage(jsonBody);
              message.type = Message.MAP;
              try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(map);
                oos.flush();
                message.setBody(baos.toByteArray());
                oos.close();
                baos.close();
              } catch (IOException exc) {
                MessageFormatException jExc =
                  new MessageFormatException("The message body could not be serialized.");
                jExc.setLinkedException(exc);
                throw jExc;
              }
            } break;

            case "BytesMessage": {
              ArrayList jmsBody =  (ArrayList) msg.get("body");
              message.type = Message.BYTES;
              byte[] bytes = new byte[((ArrayList) jmsBody).size()];
              for (int i = 0; i < ((ArrayList) jmsBody).size(); i++) {
                Object value = ((ArrayList) jmsBody).get(i);
                bytes[i] = ((Number) value).byteValue();
              }
              message.body = bytes;
            } break;

            default:
              logger.log(BasicLevel.ERROR, "TODO::: receiveRestMessages.retrieve: type = " + type + " not supported.");
              break;
            }
          }
        } catch (Exception e) {
          if (logger.isLoggable(BasicLevel.WARN))
            logger.log(BasicLevel.WARN, "RESTAcquisitionDaemon.receiveRestMessages: json = " + json, e);
          message = null;
        }
      }

      if (message != null)
        messages.add(message);
    }
      
    } catch (Exception e) {
      //java.net.ConnectException: Connection refused: connect
      if (logger.isLoggable(BasicLevel.WARN))
        logger.log(BasicLevel.WARN, "EXCEPTION:: RESTAcquisitionDaemon.receiveRestMessages: " + e.getMessage());
      
      if (restConsumer != null) {
        if (logger.isLoggable(BasicLevel.INFO))
          logger.log(BasicLevel.INFO, "RESTAcquisitionDaemon.receiveRestMessages: stop...");
        restConsumer.stop();
        restConsumer = null;
      }
      
      if (reconnectDaemon == null || !reconnectDaemon.isRunning()) {
        reconnectDaemon = new ReconnectDaemon();
        reconnectDaemon.start();
      }
    }

    return messages;
  }

  /**
   * Daemon used to reconnect.
   */
  private class ReconnectDaemon extends Daemon {

    /** Constructs a <code>ReconnectDaemon</code> thread. */
    protected ReconnectDaemon() {
      super("RESTAcquisitionDaemon_ReconnectDaemon", logger);
      setDaemon(true);
      if (logmon.isLoggable(BasicLevel.DEBUG)) {
        logmon.log(BasicLevel.DEBUG, "ReconnectDaemon<init>");
      }
    }

    /** The daemon's loop. */
    public void run() {
      if (logmon.isLoggable(BasicLevel.DEBUG)) {
        logmon.log(BasicLevel.DEBUG, "ReconnectDaemon.run()");
      }

      try {
        int retry = 0;
        while (running) {

          canStop = true;
          
          try {
            retry++;
            if (logger.isLoggable(BasicLevel.DEBUG))
              logger.log(BasicLevel.DEBUG, "reconnect() retry = " + retry);

            boolean ret = init(properties);
            if (ret){
              if (logger.isLoggable(BasicLevel.DEBUG))
                logger.log(BasicLevel.DEBUG, "reconnected.");
              break;
            } else {
              try {
                Thread.sleep(reconnectSleep);
              } catch (InterruptedException e1) { }
            }
          } catch (Exception e) {
            if (logger.isLoggable(BasicLevel.DEBUG))
              logger.log(BasicLevel.DEBUG, "reconnect():: " + e.getMessage());
            try {
              Thread.sleep(reconnectSleep);
            } catch (InterruptedException e1) { }
          }

          //canStop = false;
        }
        reconnectDaemon = null;
      } finally {
        finish();
      }
    }

    @Override
    protected void close() {
      // TODO Auto-generated method stub
      
    }

    @Override
    protected void shutdown() {
      interrupt();
    } 
    
  }
  
  /**
   * Daemon used to get and transmit the message.
   */
  private class RestConsumer extends Daemon { 

    /** Constructs a <code>RestConsumer</code> thread. */
    protected RestConsumer() {
      super("RESTAcquisitionDaemon_RestConsumer", logger);
      setDaemon(true);
      if (logmon.isLoggable(BasicLevel.DEBUG)) {
        logmon.log(BasicLevel.DEBUG, "RestConsumer<init>");
      }
    }

    /** The daemon's loop. */
    public void run() {
      if (logmon.isLoggable(BasicLevel.DEBUG)) {
        logmon.log(BasicLevel.DEBUG, "run()");
      }

      try {
        while (running) {

          canStop = true;
          ArrayList<Message> messages = receiveRestMessages();
          if (logmon.isLoggable(BasicLevel.DEBUG)) {
            if (messages.size() > 0)
              logmon.log(BasicLevel.DEBUG, "RestConsumer messages = " + messages);
          }

          canStop = false;
          if (messages.size() > 0) {
            transmitter.transmit(messages, persistent);
            if (logmon.isLoggable(BasicLevel.DEBUG))
              logmon.log(BasicLevel.DEBUG, "RestConsumer transmited.");
          }
        }
      } finally {
        finish();
      }
    }

    /** Shuts the daemon down. */
    public void shutdown() {
      interrupt();
    }

    /** Releases the daemon's resources. */
    public void close() {
      if (uriCloseConsumer == null) {
        return;
      }
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, "RestConsumer:: close-consumer = " + uriCloseConsumer);
      try {
      client.target(uriCloseConsumer).request().accept(MediaType.TEXT_PLAIN).delete();
      } catch (Exception e) {
        logger.log(BasicLevel.DEBUG, "close:: " + e.getMessage());
      }
      uriCloseConsumer = null;
    }

  }
}
