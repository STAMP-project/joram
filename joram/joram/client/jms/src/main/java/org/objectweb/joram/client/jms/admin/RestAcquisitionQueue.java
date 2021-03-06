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
 * The <code>RestAcquisitionQueue</code> class allows administrators to create REST
 * acquisition queues (Rest bridge in).
 * <p>
 * The Rest bridge destinations rely on a particular Joram service which purpose is to maintain
 * valid connections with the foreign REST Joram servers.
 * 
 * The valid properties define by this destination are:<ul>
 * <li>rest.host: hostname or IP address of remote JMS REST server, by default "localhost".</li>
 * <li>rest.port: listening port of remote JMS REST server, by default 8989.</li>
 * <li>rest.user: user name needed to connect to remote JMS REST server, by default "anonymous".</li>
 * <li>rest.pass: password needed to connect to remote JMS REST server, by default "anonymous".</li>
 * <li>rest.mediaTypeJson: if true the acquisition queue accepts JSON message, default true.</li>
 * <li>rest.timeout: Timeout for waiting for a message, by default 10.000 (10 seconds).</li>
 * <li>rest.idletimeout: normally each remote resource need to be explicitly closed, this parameter allows to 
 *     set the idle time in seconds in which the remote context will be closed if idle. If less than or equal
 *     to 0, the context never closes. By default 60 seconds.</li>
 * <li>acquisition.period: delay between to attempts to receive a message if the acquisition
 *     queue doesn't use the asyncrhonous mode, by default 100 milliseconds.</li>
 * <li>jms.destination: the name of remote JMS destination.</li>
 * </ul>
 */
public class RestAcquisitionQueue {
  /**
   * Class name of handler allowing to acquire messages to a foreign REST provider.
   */
//  public final static String RESTAcquisition = "com.scalagent.joram.mom.dest.rest.RESTAcquisitionDaemon";
  public final static String RESTAcquisition = "com.scalagent.joram.mom.dest.rest.RestAcquisitionAsync";
  
  private String host = "localhost";
  private int port = 8989;
  private String username = "anonymous";
  private String password = "anonymous";
  private boolean mediaTypeJson = true;
  private long timeout = 10000;
  // This attribute below is only needed with timed acquisition.
  private int acquisitionPeriod = -1;
  // Normally each consumer resource need to be explicitly closed, this parameter allows to set the idle time
  // in seconds in which the consumer context will be closed if idle.
  private long idleTimeout = 60;

  /**
   * @return the hostName
   */
  public String getHost() {
    return host;
  }

  /**
   * @param hostName the hostName to set
   */
  public RestAcquisitionQueue setHost(String host) {
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
  public RestAcquisitionQueue setPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * @return the userName
   */
  public String getUsername() {
    return username;
  }

  /**
   * @param userName the userName to set
   */
  public RestAcquisitionQueue setUsername(String userName) {
    this.username = userName;
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
  public RestAcquisitionQueue setPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * @return the mediaTypeJson
   */
  public boolean isMediaTypeJson() {
    return mediaTypeJson;
  }

  /**
   * @param mediaTypeJson the mediaTypeJson to set
   */
  public RestAcquisitionQueue setMediaTypeJson(boolean mediaTypeJson) {
    this.mediaTypeJson = mediaTypeJson;
    return this;
  }

  /**
   * @return the timeout
   */
  public long getTimeout() {
    return timeout;
  }

  /**
   * @param timeout the timeout to set
   */
  public RestAcquisitionQueue setTimeout(long timeout) {
    this.timeout = timeout;
    return this;
  }
  
  /**
   * @return the acquisitionPeriod
   */
  public long getAcquisitionPeriod() {
    return acquisitionPeriod;
  }

  /**
   * @param acquisitionPeriod the acquisitionPeriod to set
   */
  public RestAcquisitionQueue setAcquisitionPeriod(int acquisitionPeriod) {
    this.acquisitionPeriod = acquisitionPeriod;
    return this;
  }
  
  public long getIdleTimeout() {
    return idleTimeout;
  }

  public RestAcquisitionQueue setIdleTimeout(long idleTimeout) {
    this.idleTimeout = idleTimeout;
    return this;
  }

  /**
   * Administration method creating and deploying a REST acquisition queue on the local server.
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
   * Administration method creating and deploying a REST acquisition queue on a given server.
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
   * Administration method creating and deploying a REST acquisition queue on a given server.
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
   * Administration method creating and deploying a REST acquisition queue on a given server.
   * <p>
   * In addition to properties used to configure acquisition queues a set of specific properties allows to
   * configure Rest/JMS acquisition destination:<ul>
   * <li>rest.host: hostname or IP address of remote JMS REST server, by default "localhost".</li>
   * <li>rest.port: listening port of remote JMS REST server, by default 8989.</li>
   * <li>rest.user: user name needed to connect to remote JMS REST server, by default "anonymous".</li>
   * <li>rest.pass: password needed to connect to remote JMS REST server, by default "anonymous".</li>
   * <li>rest.mediaTypeJson: if true the acquisition queue accepts JSON message, default true.</li>
   * <li>rest.timeout: Timeout for waiting for a message, by default 10.000 (10 seconds).</li>
   * <li>rest.idletimeout: normally each remote resource need to be explicitly closed, this parameter allows to 
   *     set the idle time in seconds in which the remote context will be closed if idle. If less than or equal
   *     to 0, the context never closes. By default 60 seconds.</li>
   * <li>acquisition.period: delay between to attempts to receive a message if the acquisition
   *     queue doesn't use the asyncrhonous mode, by default 100 milliseconds.</li>
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
    
    props.setProperty(DestinationConstants.ACQUISITION_CLASS_NAME, RESTAcquisition);
    if (!props.containsKey(DestinationConstants.REST_HOST_PROP))
      props.setProperty(DestinationConstants.REST_HOST_PROP, host);
    if (!props.containsKey(DestinationConstants.REST_PORT_PROP))
      props.setProperty(DestinationConstants.REST_PORT_PROP, "" + port);
    if (!props.containsKey(DestinationConstants.REST_USERNAME_PROP))
      props.setProperty(DestinationConstants.REST_USERNAME_PROP, username);
    if (!props.containsKey(DestinationConstants.REST_PASSWORD_PROP))
      props.setProperty(DestinationConstants.REST_PASSWORD_PROP, password);
    if (!props.containsKey(DestinationConstants.MEDIA_TYPE_JSON_PROP))
      props.setProperty(DestinationConstants.MEDIA_TYPE_JSON_PROP, "" + mediaTypeJson);
    if (!props.containsKey(DestinationConstants.TIMEOUT_PROP))
      props.setProperty(DestinationConstants.TIMEOUT_PROP, "" + timeout);
    if (!props.containsKey(DestinationConstants.IDLETIMEOUT_PROP))
      props.setProperty(DestinationConstants.IDLETIMEOUT_PROP, "" + idleTimeout);
    if (!props.containsKey(DestinationConstants.ACQUISITION_PERIOD))
      props.setProperty(DestinationConstants.ACQUISITION_PERIOD, "" + acquisitionPeriod);
    
    props.setProperty(DestinationConstants.DESTINATION_NAME_PROP, dest);
    
    return Queue.create(serverId, name, Destination.ACQUISITION_QUEUE, props);
  }
}
