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
 * The <code>RestAcquisitionQueue</code> class allows administrators to create REST
 * acquisition queues (Rest bridge in).
 * <p>
 * The Rest bridge destinations rely on a particular Joram service which purpose is to maintain
 * valid connections with the foreign REST Joram servers.
 */
public class RestAcquisitionQueue {
  /**
   * Class name of handler allowing to acquire messages to a foreign REST provider.
   */
  public final static String RESTAcquisition = "com.scalagent.joram.mom.dest.rest.RESTAcquisitionDaemon";
  
  private String hostName = "localhost";
  private int port = 8989;
  private String userName = "anonymous";
  private String password = "anonymous";
  private int nbMaxMsgByPeriode = 100;
  private boolean mediaTypeJson = true;;
  private long timeout = 500;
  private int acquisitionPeriod = 100;
  
  
  /**
   * @return the hostName
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * @param hostName the hostName to set
   */
  public RestAcquisitionQueue setHostName(String hostName) {
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
  public RestAcquisitionQueue setPort(int port) {
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
  public RestAcquisitionQueue setUserName(String userName) {
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
  public RestAcquisitionQueue setPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * @return the nbMaxMsgByPeriode
   */
  public int getNbMaxMsgByPeriode() {
    return nbMaxMsgByPeriode;
  }

  /**
   * @param nbMaxMsgByPeriode the nbMaxMsgByPeriode to set
   */
  public RestAcquisitionQueue setNbMaxMsgByPeriode(int nbMaxMsgByPeriode) {
    this.nbMaxMsgByPeriode = nbMaxMsgByPeriode;
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
   * A set of properties is used to configure the distribution destination:<ul>
   * <li>period – .</li>
   * <li>acquisition.period - The period between two acquisitions, default is 0 (no periodic acquisition).</li>
   * <li>persistent - Tells if produced messages will be persistent, default is true (JMS default).</li>
   * <li>expiration - Tells the life expectancy of produced messages, default is 0 (JMS default time to live).</li>
   * <li>priority - Tells the JMS priority of produced messages, default is 4 (JMS default).</li>
   * <li>acquisition.max_msg - The maximum number of messages between the last message acquired by the handler
   * and the message correctly handled by the acquisition destination, default is 20. When the number of messages
   * waiting to be handled is greater the acquisition handler is temporarily stopped. A value lesser or equal to
   * 0 disables the mechanism.</li>
   * <li>acquisition.min_msg - The minimum number of message to restart the acquisition, default is 10.</li>
   * <li>acquisition.max_pnd - The maximum number of pending messages on the acquisition destination, default is 20.
   * When the number of waiting messages is greater the acquisition handler is temporarily stopped. A value lesser
   * or equal to 0 disables the mechanism.</li>
   * <li>acquisition.min_pnd - The minimum number of pending messages to restart the acquisition, default is 10.</li>
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
    
    props.setProperty("acquisition.className", RESTAcquisition);
    if (!props.containsKey("rest.hostName"))
      props.setProperty("rest.hostName", hostName);
    if (!props.containsKey("rest.port"))
      props.setProperty("rest.port", ""+port);
    if (!props.containsKey("rest.userName"))
      props.setProperty("rest.userName", userName);
    if (!props.containsKey("rest.password"))
      props.setProperty("rest.password", password);
    if (!props.containsKey("rest.nbMaxMsgByPeriode"))
      props.setProperty("rest.nbMaxMsgByPeriode", ""+nbMaxMsgByPeriode);
    if (!props.containsKey("rest.mediaTypeJson"))
      props.setProperty("rest.mediaTypeJson", ""+mediaTypeJson);
    if (!props.containsKey("rest.timeout"))
      props.setProperty("rest.timeout", ""+timeout);
    if (!props.containsKey("acquisition.period"))
      props.setProperty("acquisition.period", ""+acquisitionPeriod);
    
    props.setProperty("jms.destination", dest);
    
    return Queue.create(serverId, name, Destination.ACQUISITION_QUEUE, props);
  }
}
