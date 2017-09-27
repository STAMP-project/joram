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
import org.objectweb.joram.shared.DestinationConstants;

/**
 * The <code>RestDistributionQueue</code> class allows administrators to create REST
 * distribution queues (REST bridge out).
 * <p>
 * The REST bridge destinations rely on a particular Joram service which purpose is to maintain
 * valid connections with the foreign REST Joram servers.
 * 
 * The valid properties define by this destination are:<ul>
 * <li>rest.host: hostname or IP address of remote JMS REST server, by default "localhost".</li>
 * <li>rest.port: listening port of remote JMS REST server, by default 8989.</li>
 * <li>rest.user: user name needed to connect to remote JMS REST server, by default "anonymous".</li>
 * <li>rest.pass: password needed to connect to remote JMS REST server, by default "anonymous".</li>
 * <li>distribution.async: default true.<li>
 * <li>distribution.batch: default true.<li>
 * <li>rest.idletimeout: normally each remote resource need to be explicitly closed, this parameter allows to 
 *     set the idle time in seconds in which the remote context will be closed if idle. If less than or equal
 *     to 0, the context never closes. By default 60 seconds.</li>
 * <li>period: default 1.000 (1 second).</li>
 * <li>jms.destination: the name of remote JMS destination.</li>
 * </ul>
 */
public class RestDistributionQueue {
  /**
   * Class name of handler allowing to distribute messages to a foreign REST provider.
   */
  public final static String RESTDistribution = "com.scalagent.joram.mom.dest.rest.RESTDistribution";
  
  private String host = "localhost";
  private int port = 8989;
  private String userName = "anonymous";
  private String password = "anonymous";
  private boolean batch =  true;
  private boolean async = true;
  private int period = 1000;
  private long idleTimeout; // TODO (AF): Fix a default value (used in constructor)
  
  /**
   * @return the hostName
   */
  public String getHost() {
    return host;
  }

  /**
   * @param hostName the hostName to set
   */
  public RestDistributionQueue setHost(String host) {
    this.host = host;
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
   * Gets the batch parameter.
   * @return the batch parameter
   */
  public boolean isBatch() {
    return batch;
  }

  /**
   * Sets the batch parameter.
   * @param batch the batch to set
   */
  public RestDistributionQueue setBatch(boolean batch) {
    this.batch = batch;
    return this;
  }

  /**
   * Gets the async parameter.
   * @return the async parameter
   */
  public boolean isAsync() {
    return async;
  }

  /**
   * Sets the async parameter.
   * @param async the batch to set
   */
  public RestDistributionQueue setAsync(boolean async) {
    this.async = async;
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
  
  public long getIdleTimeout() {
    return idleTimeout;
  }

  public RestDistributionQueue setIdleTimeout(long idleTimeout) {
    this.idleTimeout = idleTimeout;
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
   * In addition to properties used to configure distribution queues a set of specific properties allows to
   * configure Rest/JMS distribution destination:<ul>
   * <li>rest.host: hostname or IP address of remote JMS REST server, by default "localhost".</li>
   * <li>rest.port: listening port of remote JMS REST server, by default 8989.</li>
   * <li>rest.user: user name needed to connect to remote JMS REST server, by default "anonymous".</li>
   * <li>rest.pass: password needed to connect to remote JMS REST server, by default "anonymous".</li>
   * <li>distribution.async: default true.<li>
   * <li>distribution.batch: default true.<li>
   * <li>rest.idletimeout: normally each remote resource need to be explicitly closed, this parameter allows to 
   *     set the idle time in seconds in which the remote context will be closed if idle. If less than or equal
   *     to 0, the context never closes. By default 60 seconds.</li>
   * <li>period: default 1.000 (1 second).</li>
   * <li>jms.destination: the name of remote JMS destination.</li>
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
    if (dest == null)
      throw new AdminException("Remote destination cannot be null");

    if (props == null)
      props = new Properties();
    
    props.setProperty("distribution.className", RESTDistribution);
    if (!props.containsKey(DestinationConstants.REST_HOST_PROP))
      props.setProperty(DestinationConstants.REST_HOST_PROP, host);
    if (!props.containsKey(DestinationConstants.REST_PORT_PROP))
      props.setProperty(DestinationConstants.REST_PORT_PROP, ""+port);
    if (!props.containsKey(DestinationConstants.REST_USERNAME_PROP))
      props.setProperty(DestinationConstants.REST_USERNAME_PROP, userName);
    if (!props.containsKey(DestinationConstants.REST_PASSWORD_PROP))
      props.setProperty(DestinationConstants.REST_PASSWORD_PROP, password);
    if (!props.containsKey(DestinationConstants.ASYNC_DISTRIBUTION_OPTION))
      props.setProperty(DestinationConstants.ASYNC_DISTRIBUTION_OPTION, ""+async);
    if (!props.containsKey(DestinationConstants.BATCH_DISTRIBUTION_OPTION))
      props.setProperty(DestinationConstants.BATCH_DISTRIBUTION_OPTION, ""+batch);
    if (!props.containsKey(DestinationConstants.IDLETIMEOUT_PROP))
      props.setProperty(DestinationConstants.IDLETIMEOUT_PROP, ""+ idleTimeout);
    if (!props.containsKey("period"))
      props.setProperty("period", ""+period);

    props.setProperty(DestinationConstants.DESTINATION_NAME_PROP, dest);

    return Queue.create(serverId, name, Destination.DISTRIBUTION_QUEUE, props);
  }
}
