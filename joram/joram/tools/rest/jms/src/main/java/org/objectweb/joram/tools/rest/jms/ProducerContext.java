/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2016 ScalAgent Distributed Technologies
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
package org.objectweb.joram.tools.rest.jms;

import java.util.ArrayList;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import org.objectweb.util.monolog.api.BasicLevel;
import org.objectweb.util.monolog.api.Logger;

import fr.dyade.aaa.common.Debug;

public class ProducerContext extends SessionContext {
  public static Logger logger = Debug.getLogger(ProducerContext.class.getName());
  
  private JMSProducer producer;

  public ProducerContext(RestClientContext clientCtx) {
    super(clientCtx);
  }

  /**
   * @return the producer
   */
  public JMSProducer getProducer() {
    return producer;
  }

  /**
   * @param producer
   *          the producer to set
   */
  public void setProducer(JMSProducer producer) {
    this.producer = producer;
  }

  private int dfltDeliveryMode;
  
  public void setDefaultDeliveryMode(int deliveryMode) {
    dfltDeliveryMode = deliveryMode;
  }

  private String dfltCorrelationID = null;
  
  public void setDefaultJMSCorrelationID(String correlationID) {
    dfltCorrelationID = correlationID;
  }

  private int dfltPriority;
  
  public void setDefaultPriority(int priority) {
    dfltPriority = priority;
  }

  private long dfltTimeToLive;
  
  public void setDefaultTimeToLive(long timeToLive) {
    dfltTimeToLive = timeToLive;
  }

  private long dfltDeliveryDelay;
  
  public void setDefaultDeliveryDelay(long deliveryDelay) {
    dfltDeliveryDelay = deliveryDelay;
  }
  
  final Message createMessage(String type, Object jmsBody) throws Exception {
    Message msg = null;
    
    // Build the JMS Message
    if (type.equals(TextMessage.class.getSimpleName())) {
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, "send text message = " + jmsBody);
      
      // create the text message
      msg = getJmsContext().createTextMessage((String) jmsBody);
    } else if(type.equals(BytesMessage.class.getSimpleName())) {
      // create the byte message
      if (jmsBody instanceof ArrayList) {
        if (logger.isLoggable(BasicLevel.DEBUG))
          logger.log(BasicLevel.DEBUG, "send bytes message");
        
        msg = getJmsContext().createBytesMessage();
        byte[] bytes = new byte[((ArrayList) jmsBody).size()];
        for (int i = 0; i < ((ArrayList) jmsBody).size(); i++) {
          Object value = ((ArrayList) jmsBody).get(i);
          bytes[i] = ((Number) value).byteValue();
        }
        ((BytesMessage) msg).writeBytes(bytes);
        ((BytesMessage) msg).reset();
      } else {
        throw new Exception("BytesMessage: invalid jmsBody = " + jmsBody.getClass().getName());
      }
    } else if(type.equals(MapMessage.class.getSimpleName())) {
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, "send map message");

      // create the map message
      if (jmsBody instanceof Map) {
        msg = getJmsContext().createMapMessage();
        Helper.setMapMessage((Map) jmsBody, (MapMessage) msg);
      } else {
        throw new Exception("MapMessage: invalid jmsBody = " + jmsBody.getClass().getName());
      }
    } else if(type.equals(ObjectMessage.class.getSimpleName())) {
      throw new Exception("type: " + type + ", not yet implemented");
    } else if(type.equals(StreamMessage.class.getSimpleName())) {
      throw new Exception("type: " + type + ", not yet implemented");
    } else {
      throw new Exception("Unknown message type: " + type); 
    }

    return msg;
  }
  
  final void setJMSProperties(Message msg, Map<String, Object> jmsProps) throws JMSException {
    if (jmsProps != null) {
      // Properties
      for (String key : jmsProps.keySet()) {
        Object value = null; 
        try {
          value = Helper.getValue(jmsProps, key);
        } catch (Exception e) {
          if (logger.isLoggable(BasicLevel.ERROR))
            logger.log(BasicLevel.ERROR, "ignore set jms properties(" + key + ", " + value + ") : " + e.getMessage());
          continue;
        }
        if (value == null)
          continue;
        if (logger.isLoggable(BasicLevel.DEBUG))
          logger.log(BasicLevel.DEBUG, "set jms properties: " + key + ", value = " + value + ", " + value.getClass().getSimpleName());
        
        switch (value.getClass().getSimpleName()) {
        case "String":
          msg.setStringProperty(key, (String) value);
          break;
        case "Boolean":
          msg.setBooleanProperty(key, (Boolean)value);
          break;
        case "Integer":
          msg.setIntProperty(key, (Integer)value);
          break;
        case "Long":
          msg.setLongProperty(key, (Long)value);
          break;
        case "Double":
          msg.setDoubleProperty(key, (Double)value);
          break;
        case "Float":
          msg.setFloatProperty(key, (Float)value);
          break;
        case "Short":
          msg.setShortProperty(key, (Short)value);
          break;
        case "Byte":
          msg.setByteProperty(key, (Byte)value);
          break;

        default:
          try {
          msg.setObjectProperty(key, value);
          } catch (Exception e) {
            if (logger.isLoggable(BasicLevel.ERROR))
              logger.log(BasicLevel.ERROR, "ignore jms setObjectProperties(" + key + ", " + value + ") : " + e.getMessage());
          }
          break;
        }
      }
    }
  }
  
  synchronized long send(
      String type, 
      Map<String, Object> jmsHeaders, 
      Map<String, Object> jmsProps, 
      Object jmsBody, 
      int deliveryMode,    // -1 if not set in Rest call
      long deliveryDelay,  // -1 if not set in Rest call
      int priority,        // -1 if not set in Rest call
      long timeToLive,     // -1 if not set in Rest call
      String correlationID) throws Exception {
    // creates the JMS Message
    Message msg = createMessage(type, jmsBody);
    // Sets JMS Message properties
    setJMSProperties(msg, jmsProps);
    
    // Determines the JMS Message QoS, first from jmsHeaders map, second from Rest call
    // parameters (priority), and last using default value from producer.
    if (jmsHeaders != null) {
      if (deliveryMode == -1) {
        try {
          Integer value = (Integer) Helper.getValue(jmsHeaders, "DeliveryMode");
          if (value != null)
            deliveryMode = value.intValue();
        } catch (Exception exc) {
          logger.log(BasicLevel.WARN, "ProducerContext.send(); bad DeliveryMode value", exc);
        }
      }
      if (deliveryDelay == -1) {
        try {
          Long value = (Long) Helper.getValue(jmsHeaders, "DeliveryTime");
          if (value != null)
            deliveryDelay = value.longValue();
        } catch (Exception exc) {
          logger.log(BasicLevel.WARN, "ProducerContext.send(); bad DeliveryTime value", exc);
        }
      }
      if (priority == -1) {
        try {
          Integer value = (Integer) Helper.getValue(jmsHeaders, "Priority");
          if (value != null)
            priority = value.intValue();
        } catch (Exception exc) {
          logger.log(BasicLevel.WARN, "ProducerContext.send(); bad Priority value", exc);
        }
      }
      if (timeToLive == -1) {
        try {
          Long value = (Long) Helper.getValue(jmsHeaders, "Expiration");
          if (value != null)
            timeToLive = value.longValue();
        } catch (Exception exc) {
          logger.log(BasicLevel.WARN, "ProducerContext.send(); bad Expiration value", exc);
        }
      }
      if (correlationID == null) {
        try {
          String value = (String) Helper.getValue(jmsHeaders, "CorrelationID");
          if (value != null)
            correlationID = value;
        } catch (Exception exc) {
          logger.log(BasicLevel.WARN, "ProducerContext.send(); bad CorrelationID value", exc);
        }
      }
    }
    
    if (correlationID == null)
      correlationID = dfltCorrelationID;
    if (correlationID != null)
      msg.setJMSCorrelationID(correlationID);
    
    if (deliveryMode == -1)
      deliveryMode = dfltDeliveryMode;
    if (deliveryDelay == -1)
      deliveryDelay = dfltDeliveryDelay;
    if (priority == -1)
      priority = dfltPriority;
    if (timeToLive == -1)
      timeToLive = dfltTimeToLive;
    
    // send the message
    getProducer().setDeliveryMode(deliveryMode).setPriority(priority).setDeliveryDelay(deliveryDelay).setTimeToLive(timeToLive).send(getDest(), msg);
    // Increment the last id
    incLastId();
    //update activity
    getClientCtx().setLastActivity(System.currentTimeMillis());
    
    return getLastId();
  }

}
