/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2005 - 2006 ScalAgent Distributed Technologies
 * Copyright (C) 2004 - France Telecom R&D
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
package org.objectweb.joram.mom.notifications;

import java.util.Hashtable;

public class JoinQueueCluster extends QueueClusterNot {
  
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  public Hashtable clusters;
  public Hashtable clients;
  public boolean freeReading;
  public boolean freeWriting;
  
  public JoinQueueCluster(float rateOfFlow,
                          Hashtable clusters,
                          Hashtable clients,
                          boolean freeReading,
                          boolean freeWriting) {
    super(rateOfFlow);
    this.clusters = clusters;
    this.clients = clients;
    this.freeReading = freeReading;
    this.freeWriting = freeWriting;
  }
  
  /**
   * Appends a string image for this object to the StringBuffer parameter.
   *
   * @param output
   *	buffer to fill in
   * @return
	<code>output</code> buffer is returned
   */
  public StringBuffer toString(StringBuffer output) {
    output.append('(');
    super.toString(output);
    output.append(",clusters=").append(clusters);
    output.append(",clients=").append(clients);
    output.append(",freeReading=").append(freeReading);
    output.append(",freeWriting=").append(freeWriting);
    output.append(')');

    return output;
  }
}
