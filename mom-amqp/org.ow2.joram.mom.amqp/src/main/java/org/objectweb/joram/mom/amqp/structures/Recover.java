/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2009 ScalAgent Distributed Technologies
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
package org.objectweb.joram.mom.amqp.structures;

import java.io.Serializable;
import java.util.List;

public class Recover implements Serializable {

  private static final long serialVersionUID = 1L;

  private String queueName;

  private List<Long> idsToRecover;

  public Recover(String queueName, List<Long> idsToRecover) {
    this.queueName = queueName;
    this.idsToRecover = idsToRecover;
  }

  public String getQueueName() {
    return queueName;
  }

  public List<Long> getIdsToRecover() {
    return idsToRecover;
  }

}
