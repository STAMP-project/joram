/*
 * Copyright (C) 2002 - ScalAgent Distributed Technologies
 * Copyright (C) 1996 - 2000 BULL
 * Copyright (C) 1996 - 2000 INRIA
 *
 * The contents of this file are subject to the Joram Public License,
 * as defined by the file JORAM_LICENSE.TXT 
 * 
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License on the Objectweb web site
 * (www.objectweb.org). 
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific terms governing rights and limitations under the License. 
 * 
 * The Original Code is Joram, including the java packages fr.dyade.aaa.agent,
 * fr.dyade.aaa.ip, fr.dyade.aaa.joram, fr.dyade.aaa.mom, and
 * fr.dyade.aaa.util, released May 24, 2000.
 * 
 * The Initial Developer of the Original Code is Dyade. The Original Code and
 * portions created by Dyade are Copyright Bull and Copyright INRIA.
 * All Rights Reserved.
 *
 * The present code contributor is ScalAgent Distributed Technologies.
 */
package fr.dyade.aaa.mom.messages;

import java.io.*;
import java.util.Hashtable;

/** 
 * The <code>Message</code> class actually provides the transport facility
 * for the data exchanged during MOM operations.
 * <p>
 * A message may either carry a String, or a serializable object, or an
 * hashtable, or bytes, even nothing. It is charaterized by properties and
 * "header" fields.
 */
public class Message implements Cloneable, Serializable
{
  /** The message identifier. */
  private String id = "";
  /** The message priority (from 0 to 9, 9 being the highest). */
  private int priority = 4;
  /**
   * The name of the destination the message is destinated to, or comes from.
   */
  private String destination = null;
  /** <code>true</code> if the message is used in a PTP exchange. */
  private boolean ptp;
  /** The message expiration time (0 for infinite time-to-live). */
  private long expiration = 0;
  /** The message time stamp. */
  private long timestamp = 0;
  /** The correlation identifier field. */
  private String correlationId = null;
  /**
   * The name of the destination to which a reply should be sent.
   */
  private String replyTo = null;
  /** <code>true</code> if the reply should be sent to a queue. */
  private boolean replyToQueue;

  /** Integer field for user specific needs. */
  public int userIntHeader = 0;
  /** String field for user specific needs. */
  public String userStringHeader = null;
  /** Bytes field for user specific needs. */
  public byte[] userBytesHeader = null;

  /** The message body. */
  private byte[] body = null;

  /** The message type (SIMPLE, TEXT, OBJECT, MAP, STREAM, BYTES). */
  public int type;
  /**
   * The message properties table.
   * <p>
   * <b>Key:</b> property name<br>
   * <b>Object:</b> property (native objects)
   */
  public Hashtable properties = null;
  /**
   * <code>true</code> if the message has been denied at least once by a
   * consumer.
   */
  public boolean denied = false;

  /** The identifier of the consumer, used by queues. */
  transient public String consId = null;
  /**
   * The number of acknowledgements a message still expects from its 
   * subscribers before having been fully consumed (field used by JMS proxies).
   */
  transient public int acksCounter = 0;



  /**
   * Constructs a <code>Message</code> instance.
   */
  public Message()
  {
    this.type = MessageType.SIMPLE;
  }

  
  /** Sets the message identifier. */ 
  public void setIdentifier(String id)
  {
    this.id = id;
  }

  /** Sets the message priority (0 for the lowest, 9 for the highest. */ 
  public void setPriority(int priority)
  {
    if (priority >= 0 && priority <= 9)
      this.priority = priority;
  }

  /**
   * Sets the message destination.
   *
   * @param name  The destination name.
   * @param isQueue  <code>true</code> if the destination is a queue.
   */
  public void setDestination(String name, boolean isQueue)
  {
    destination = name;
    ptp = isQueue;
  }

  /** Sets the message expiration. */
  public void setExpiration(long expiration)
  {
    if (expiration >= 0)
      this.expiration = expiration;
  }

  /** Sets the message time stamp. */
  public void setTimestamp(long timestamp)
  {
    this.timestamp = timestamp;
  }

  /** Sets the message correlation identifier. */
  public void setCorrelationId(String correlationId)
  {
    this.correlationId = correlationId;
  }

  /**
   * Sets the destination to which the reply should be sent.
   *
   * @param name  The destination name.
   * @param isQueue  <code>true</code> if the destination is a queue.
   */
  public void setReplyTo(String name, boolean isQueue)
  {
    replyTo = name;
    replyToQueue = isQueue;
  }

  /** Returns the message identifier. */
  public String getIdentifier()
  {
    return id;
  }

  /** Returns the message priority. */
  public int getPriority()
  {
    return priority;
  }
  
  /** Returns the message destination. */
  public String getDestination()
  {
    return destination;
  }

  /** Returns <code>true</code> if the destination is a queue. */
  public boolean getPTP()
  {
    return ptp;
  }

  /** Returns the message expiration time. */
  public long getExpiration()
  {
    return expiration;
  }

  /** Returns the message time stamp. */
  public long getTimestamp()
  {
    return timestamp;
  }

  /** Returns the message correlation identifier. */
  public String getCorrelationId()
  {
    return correlationId;
  }

  /** Returns the name of the destination the reply should be sent to. */
  public String getReplyTo()
  {
    return replyTo;
  }

  /** Returns <code>true</code> if the reply should be sent to a queue. */
  public boolean replyToQueue()
  {
    return replyToQueue;
  }
 
  /** Returns <code>true</code> if the message is valid. */
  public boolean isValid()
  {
    if (expiration == 0)
      return true;

    return ((expiration - System.currentTimeMillis()) > 0);
  }


  /**
   * Sets an object as the body of the message. 
   *
   * @exception IOException  In case of an error while setting the object.
   */
  public void setObject(Object object) throws IOException
  {
    if (object == null)
      body = null;
    else {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(object);
      oos.flush();
      setBody(baos.toByteArray());
      oos.close();
      baos.close();
    }
    type = MessageType.OBJECT;
  }

  /**
   * Sets a map as the body of the message.
   *
   * @exception IOException  In case of an error while setting the map.
   */
  public void setMap(Hashtable map) throws IOException
  {
    if (map == null)
      body = null;
    else {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(map);
      oos.flush();
      setBody(baos.toByteArray());
      oos.close();
      baos.close();
    }
    type = MessageType.MAP;
  }

  /** Sets a String as the body of the message. */ 
  public void setText(String text)
  {
    if (text == null)
      body = null;
    else
      body = text.getBytes();
    type = MessageType.TEXT;
  }

  /** Sets the message body as a stream of bytes. */
  public void setStream(ByteArrayOutputStream bos)
  {
    setBody(bos.toByteArray());
    type = MessageType.STREAM;
  }

  /** Sets the message body as an array of bytes. */
  public void setBytes(byte[] bytes)
  {
    setBody(bytes);
    type = MessageType.BYTES;
  } 
  
  /**
   * Returns the object body of the message.
   *
   * @exception IOException  In case of an error while getting the object.
   * @exception ClassNotFoundException  If the object class is unknown.
   */
  public Object getObject() throws Exception
  {
    if (body == null || type != MessageType.OBJECT)
      return null;
 
    ByteArrayInputStream bais = new ByteArrayInputStream(body);
    ObjectInputStream ois = new ObjectInputStream(bais);
    return ois.readObject();
  }

  /**
   * Returns the map body of the message.
   *
   * @exception IOException  In case of an error while getting the map.
   * @exception ClassNotFoundException  If the map is invalid.
   */ 
  public Hashtable getMap() throws Exception
  {
    if (body == null || type != MessageType.MAP)
      return null;

    ByteArrayInputStream bais = new ByteArrayInputStream(body);
    ObjectInputStream ois = new ObjectInputStream(bais);
    return (Hashtable) ois.readObject();
  }

  /** Gets the String body of the message. */
  public String getText()
  {
    if (body == null || type != MessageType.TEXT)
      return null;

    return new String(body);
  }

  /** Returns the stream of bytes body of the message. */
  public ByteArrayInputStream getStream()
  {
    if (type != MessageType.STREAM)
      return null;

    return new ByteArrayInputStream(body);
  }

  /** Returns the array of bytes body of the message. */
  public byte[] getBytes()
  {
    if (type != MessageType.BYTES)
      return null;

    return body;
  }


  /**
   * Returns the value corresponding to a given header field or property
   * name.
   * <p>
   * Method called by the <code>fr.dyade.aaa.mom.selectors.Filter</code>
   * class.
   */
  public Object getField(String name)
  {
    Object res = null;

    if (name.equals("JMSMessageID"))
      res = id;
    else if (name.equals("JMSPriority"))
      res = new Integer(priority);
    else if (name.equals("JMSTimestamp"))
      res = new Long(timestamp);
    else if (name.equals("JMSCorrelationID"))
      res = correlationId;
    else if (name.equals("JMSDeliveryMode"))
      res = new Integer(userIntHeader);
    else if (name.equals("JMSType"))
      res = userStringHeader;
    else if (properties == null)
      res = null;
    else
      res = properties.get(name);

    if (res instanceof Number)
      return new Double(((Number) res).doubleValue());

    return res;
  }


  /**
   * Method actually setting the message body.
   *
   * @param body  Array of bytes to set as the message body.
   */
  private void setBody(byte[] body)
  {
    this.body = new byte[body.length];
    for (int i = 0; i < body.length; i++) {
      this.body[i] = body[i];
    }
  }

  /** Clones this object. */
  public Object clone()
  {
    try {
      return super.clone();
    }
    catch (CloneNotSupportedException cE) {
      return null;
    }
  }
}
