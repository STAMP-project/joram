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

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;

public class ConsumerContext extends SessionContext {

  private JMSConsumer consumer;
  private ConcurrentHashMap<Long, Message> messages;

  public ConsumerContext(RestClientContext clientCtx) {
    super(clientCtx);
    messages = new ConcurrentHashMap<Long, Message>();
  }

  /**
   * @return the consumer
   */
  public JMSConsumer getConsumer() {
    return consumer;
  }

  /**
   * @param consumer
   *          the consumer to set
   */
  public void setConsumer(JMSConsumer consumer) {
    this.consumer = consumer;
  }

  public long getId(Message message) throws JMSException {
    if (message == null || !messages.containsValue(message))
      return -1;
    for (Entry<Long, Message> entry : messages.entrySet()) {
      if (message.getJMSMessageID().equals(entry.getValue().getJMSMessageID())) {
        return entry.getKey();
      }
    }
    return -1;
  }

  private final void put(long id, Message msg) {
    if (msg == null)
      return;
    if (id > getLastId())
      setLastId(id);
    messages.put(id, msg);
  }

  public Message getMessage(long id) {
    getClientCtx().setLastActivity(System.currentTimeMillis());
    return messages.get(id);
  }

  synchronized Message receive(long timeout, long msgId) throws JMSException {
    Message message = null;
    if (timeout > 0)
      message = getConsumer().receive(timeout);
     else if (timeout == 0)
       message = getConsumer().receiveNoWait();
     else {
       message = getConsumer().receive();
       if (message == null)
         throw new JMSException("The consumer expire (timeout)");
     }
     
     //update activity
     getClientCtx().setLastActivity(System.currentTimeMillis());
     
     if (message != null) {
       if (getJmsContext().getSessionMode() == JMSContext.CLIENT_ACKNOWLEDGE) {
         long id = msgId;
         if (id == -1)
           id = incLastId();
         put(id, message);
       } else {
         incLastId();
       }
     }
     
     return message;
  }
  
  public Message removeMessage(long id) {
    return messages.remove(id);
  }

  public void clear() {
    messages.clear();
  }
}
