/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2004 - Bull SA
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
 * Initial developer(s): Frederic Maistre (Bull SA)
 * Contributor(s):
 */
package org.objectweb.joram.client.connector;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;


/**
 * An <code>OutboundQueueSession</code> instance wraps a JMS QueueSession
 * (XA or not) for a component involved in PTP outbound messaging.
 */
public class OutboundQueueSession extends OutboundSession
                                  implements javax.jms.QueueSession
{
  /**
   * Constructs an <code>OutboundQueueSession</code> instance.
   *
   * @param sess  The JMS session (XA or not) to wrap.
   */
  OutboundQueueSession(Session sess)
  {
    super(sess);
  }


  /**
   * Delegates the call to the wrapped JMS session.
   */
  public javax.jms.QueueSender createSender(Queue queue)
         throws JMSException
  {
    return new OutboundSender(sess.createProducer(queue));
  }

  /**
   * Delegates the call to the wrapped JMS session.
   */
  public javax.jms.QueueReceiver createReceiver(Queue queue, String selector)
         throws JMSException
  {
    return new OutboundReceiver(queue, sess.createConsumer(queue, selector));
  }

  /**
   * Delegates the call to the wrapped JMS session.
   */
  public javax.jms.QueueReceiver createReceiver(Queue queue)
         throws JMSException
  {
    return new OutboundReceiver(queue, sess.createConsumer(queue));
  }
}
