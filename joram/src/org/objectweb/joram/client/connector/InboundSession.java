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
import javax.jms.Session;
import javax.jms.XAConnection;
import javax.jms.XASession;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;


/**
 * An <code>InboundSession</code> instance is responsible for processing
 * delivered messages within a <code>javax.resource.spi.Work</code> instance,
 * and passing them to a set of application server endpoints.
 */
class InboundSession implements javax.jms.ServerSession,
                                javax.resource.spi.work.Work,
                                javax.jms.MessageListener
{
  /** Application server's <code>WorkManager</code> instance. */
  private WorkManager workManager;
  /** Application's endpoints factory. */
  private MessageEndpointFactory endpointFactory; 

  /**
   * <code>javax.jms.Session</code> instance dedicated to processing
   * the delivered messages.
   */
  private Session session;
  /** <code>XAResource</code> instance, if any. */
  private XAResource xaResource = null;


  /**
   * Constructs an <code>InboundSession</code> instance.
   *
   * @param workManager      Application server's <code>WorkManager</code>
   *                         instance.
   * @param endpointFactory  Application's endpoints factory.
   * @param cnx              Connection to the underlying JORAM server.
   * @param transacted       <code>true</code> if deliveries occur within a 
   *                         XA transaction.
   */
  InboundSession(WorkManager workManager,
                 MessageEndpointFactory endpointFactory,
                 XAConnection cnx,
                 boolean transacted)
  {
    this.workManager = workManager;
    this.endpointFactory = endpointFactory;

    try {
      if (transacted) {
        session = cnx.createXASession();
        xaResource = ((XASession) session).getXAResource();
      }
      else
        session = cnx.createSession(false, Session.AUTO_ACKNOWLEDGE);

      session.setMessageListener(this);
    }
    catch (JMSException exc) {}
  }


  /**
   * Provides the wrapped <code>javax.jms.Session</code> instance for
   * processing delivered messages.
   *
   * @exception JMSException  Never thrown.
   */
  public Session getSession() throws JMSException
  {
    return session;
  }

  /**
   * Notifies that the messages are ready to be processed.
   *
   * @exception JMSException  If submitting the processing work fails.
   */
  public void start() throws JMSException
  {
    try {
      workManager.scheduleWork(this);
    }
    catch (Exception exc) {
      throw new JMSException("Can't start the adapter session for processing "
                             + "the delivered messages: " + exc);
    }
  }

  /** <code>javax.resource.spi.Work</code> method, not effective. */
  public void release()
  {}

  /** Runs the wrapped session for processing the messages. */
  public void run()
  {
    session.run();
  }

  /** Forwards a processed message to an endpoint. */
  public void onMessage(javax.jms.Message message)
  {
    try {
      MessageEndpoint endpoint = endpointFactory.createEndpoint(xaResource);
      ((javax.jms.MessageListener) endpoint).onMessage(message);
      endpoint.release();
    }
    catch (Exception exc) {
      throw new java.lang.IllegalStateException("Could not get endpoint "
                                                + "instance: " + exc);
    }
  }
}
