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
package org.objectweb.joram.client.jms.admin;

import java.net.ConnectException;
import java.util.Properties;

import org.objectweb.joram.client.jms.Destination;
import org.objectweb.joram.client.jms.Queue;

/**
 * The <code>RestDistributionQueue</code> class allows administrators to create REST
 * distribution queues (REST bridge out).
 * <p>
 * The REST bridge destinations rely on a particular Joram service which purpose is to maintain
 * valid connections with the foreign REST Joram servers.
 */
public class RestDistributionQueue {
  /**
   * Class name of handler allowing to distribute messages to a foreign REST provider.
   */
  public final static String RESTDistribution = "com.scalagent.joram.mom.dest.rest.RESTDistribution";
  
  private String hostName = "localhost";
  private int port = 8989;
  private String userName = "anonymous";
  private String password = "anonymous";
  private boolean batch =  true;
  private int period = 1000;
  
  /**
   * @return the hostName
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * @param hostName the hostName to set
   */
  public RestDistributionQueue setHostName(String hostName) {
    this.hostName = hostName;
    return this;
  }

  /**
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * @param port the port to set
   */
  public RestDistributionQueue setPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * @return the userName
   */
  public String getUserName() {
    return userName;
  }

  /**
   * @param userName the userName to set
   */
  public RestDistributionQueue setUserName(String userName) {
    this.userName = userName;
    return this;
  }

  /**
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param password the password to set
   */
  public RestDistributionQueue setPassword(String password) {
    this.password = password;
    return this;
  }


  /**
   * @return the batch
   */
  public boolean isBatch() {
    return batch;
  }

  /**
   * @param batch the batch to set
   */
  public RestDistributionQueue setBatch(boolean batch) {
    this.batch = batch;
    return this;
  }

  /**
   * @return the period
   */
  public int getPeriod() {
    return period;
  }

  /**
   * @param period the period to set
   */
  public RestDistributionQueue setPeriod(int period) {
    this.period = period;
    return this;
  }

  /**
   * Administration method creating and deploying a REST distribution queue on the local server.
   * <p>
   * The request fails if the destination deployment fails server side.
   * <p>
   * Be careful this method use the static AdminModule connection.
   * 
   * @param dest  The name of the foreign destination.
   * @return the created bridge destination.
   *
   * @exception ConnectException  If the administration connection is closed or broken.
   * @exception AdminException  If the request fails.
   * 
   * @see #create(int, String, String, Properties)
   */
  public Queue create(String dest) throws ConnectException, AdminException {
    return create(AdminModule.getLocalServerId(), dest);
  }

  /**
   * Administration method creating and deploying a REST distribution queue on a given server.
   * <p>
   * The request fails if the target server does not belong to the platform,
   * or if the destination deployment fails server side.
   * <p>
   * Be careful this method use the static AdminModule connection.
   *
   * @param serverId  The identifier of the server where deploying the queue.
   * @param dest      The name of the foreign destination.
   * @return the created bridge destination.
   *
   * @exception ConnectException  If the administration connection is closed or broken.
   * @exception AdminException  If the request fails.
   * 
   * @see #create(int, String, String, Properties)
   */
  public Queue create(int serverId,
      String dest) throws ConnectException, AdminException {
    return create(serverId, (String) null, dest);
  }

  /**
   * Administration method creating and deploying a REST distribution queue on a given server.
   * <p>
   * The request fails if the target server does not belong to the platform,
   * or if the destination deployment fails server side.
   * <p>
   * Be careful this method use the static AdminModule connection.
   *
   * @param serverId  The identifier of the server where deploying the queue.
   * @param name      The name of the created queue.
   * @param dest      The name of the foreign destination.
   * @return the created bridge destination.
   *
   * @exception ConnectException  If the administration connection is closed or broken.
   * @exception AdminException  If the request fails.
   * 
   * @see #create(int, String, String, Properties)
   */
  public Queue create(int serverId,
      String name,
      String dest) throws ConnectException, AdminException {
    return create(serverId, name, dest, null);
  }

  /**
   * Administration method creating and deploying a REST distribution queue on a given server.
   * <p>
   * A set of properties is used to configure the distribution destination:<ul>
   * <li>period – Tells the time to wait before another distribution attempt. Default is 0, which
   * means there won't be other attempts.</li>
   * <li>distribution.batch –  If set to true, the destination will try to distribute each time every waiting
   * message, regardless of distribution errors. This can lead to the loss of message ordering, but will
   * prevent a blocking message from blocking every following message. When set to false, the distribution
   * process will stop on the first error. Default is false.</li>
   * <li>distribution.async - If set to true, the messages are asynchronously forwarded through a daemon.</li>
   * </ul>
   * The request fails if the target server does not belong to the platform,
   * or if the destination deployment fails server side.
   * <p>
   * Be careful this method use the static AdminModule connection.
   *
   * @param serverId  The identifier of the server where deploying the queue.
   * @param name      The name of the created queue.
   * @param dest      The name of the foreign destination.
   * @param props     A Properties object containing all needed parameters.
   * @return the created bridge destination.
   *
   * @exception ConnectException  If the administration connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public Queue create(int serverId,
      String name,
      String dest,
      Properties props) throws ConnectException, AdminException {
    if (props == null)
      props = new Properties();
    props.setProperty("distribution.className", RESTDistribution);
    if (!props.containsKey("rest.hostName"))
      props.setProperty("rest.hostName", hostName);
    if (!props.containsKey("rest.port"))
      props.setProperty("rest.port", ""+port);
    if (!props.containsKey("rest.userName"))
      props.setProperty("rest.userName", userName);
    if (!props.containsKey("rest.password"))
      props.setProperty("rest.password", password);
    if (!props.containsKey("distribution.batch"))
      props.setProperty("distribution.batch", ""+batch);
    if (!props.containsKey("period"))
    props.setProperty("period", ""+period);
    
    props.setProperty("jms.destination", dest);
    
    return Queue.create(serverId, name, Destination.DISTRIBUTION_QUEUE, props);
  }
}
