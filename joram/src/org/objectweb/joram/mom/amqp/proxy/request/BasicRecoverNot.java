/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2009 ScalAgent Distributed Technologies
 * Copyright (C) 2009 CNES
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
package org.objectweb.joram.mom.amqp.proxy.request;

import org.objectweb.joram.mom.amqp.marshalling.AMQP;

import fr.dyade.aaa.agent.AgentId;
import fr.dyade.aaa.agent.SyncNotification;

public class BasicRecoverNot extends SyncNotification {

  /** define serialVersionUID for interoperability */
  private static final long serialVersionUID = 1L;

  private int channelId;
  private boolean requeue;
  
  /**
   * @param channelId
   * @param ticket
   * @param queue
   * @param noAck
   * @param consumerTag
   * @param callback
   */
  public BasicRecoverNot(int channelId, boolean requeue) {
    super();
    this.requeue = requeue;
    this.channelId = channelId;
  }
  
  public boolean isRequeue() {
    return requeue;
  }
  
  public int getChannelId() {
    return channelId;
  }

  public AMQP.Basic.CancelOk basicCancel(AgentId proxyId) throws Exception {
    Object[] res = invoke(proxyId);
    return (AMQP.Basic.CancelOk) res[0];
  }
  
  public void Return() {
    Return(null);
  }

}
