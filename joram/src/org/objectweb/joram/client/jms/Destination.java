/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2004 - Bull SA
 * Copyright (C) 2004 - ScalAgent Distributed Technologies
 * Copyright (C) 1996 - Dyade
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
 * Initial developer(s): Frederic Maistre (INRIA)
 * Contributor(s): Nicolas Tachker (ScalAgent DT)
 */
package org.objectweb.joram.client.jms;

import org.objectweb.joram.client.jms.admin.DeadMQueue;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.admin.AdministeredObject;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.AdminException;
import org.objectweb.joram.shared.admin.*;
import org.objectweb.util.monolog.api.BasicLevel;

import java.net.ConnectException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.naming.*;

/**
 * Implements the <code>javax.jms.Destination</code> interface and provides
 * JORAM specific administration and monitoring methods.
 */
public abstract class Destination
                      extends AdministeredObject
                      implements javax.jms.Destination
{
  /** Identifier of the agent destination. */
  protected String agentId;

  /** <code>true</code> if destination is a queue. */
  protected boolean isQueue = false;


  /**
   * Constructs a destination.
   *
   * @param agentId  Identifier of the agent destination.
   */ 
  public Destination(String agentId)
  {
    super(agentId);
    this.agentId = agentId;

    if (JoramTracing.dbgClient.isLoggable(BasicLevel.DEBUG))
      JoramTracing.dbgClient.log(BasicLevel.DEBUG, this + ": created.");
  }

  /**
   * Constructs an empty destination.
   */ 
  public Destination()
  {}


  /** Returns the name of the destination. */
  public String getName()
  {
    return agentId;
  }

  /**
   * Returns <code>true</code> if the parameter object is a Joram destination
   * wrapping the same agent identifier.
   */
  public boolean equals(Object obj)
  {
    if (! (obj instanceof Destination))
      return false;

    return (agentId.equals(((Destination) obj).getName()));
  }

  /**
   * Returns <code>true</code> if the destination is a queue.
   */
  public boolean isQueue() {
    return isQueue;
  }

  /**
   * Codes a <code>Destination</code> as a Hashtable for travelling through the
   * SOAP protocol.
   */
  public Hashtable code() {
    Hashtable h = super.code();
    h.put("agentId",agentId);
    return h;
  }

  /** Sets the naming reference of a destination. */
  public Reference getReference() throws NamingException
  {
    Reference ref = super.getReference();
    ref.add(new StringRefAddr("dest.name", agentId));
    return ref;
  }


  /**
   * Admin method creating or retrieving a destination with a given name on a
   * given server, and returning its identifier.
   * <p>
   * The request fails if the target server does not belong to the platform,
   * or if the destination deployement fails server side.
   *
   * @param serverId  The identifier of the server where deploying the
   *                  destination.
   * @param name      The destination name.
   * @param className Name of the MOM destination class.
   * @param prop      Properties.
   *
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public static String doCreate(int serverId, 
                                String name, 
                                String className, 
                                Properties prop)
         throws ConnectException, AdminException
  {
    CreateDestinationRequest cdr =
      new CreateDestinationRequest(serverId, name, className);
    cdr.setProperties(prop);
    CreateDestinationReply reply =
      (CreateDestinationReply) AdminModule.doRequest(cdr);
    return reply.getDestId();
  }

  /**
   * Admin method removing this destination from the platform.
   *
   * @exception AdminException    Never thrown.
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception JMSException      Never thrown.
   */
  public void delete()
         throws ConnectException, AdminException, javax.jms.JMSException
  {
    AdminModule.doRequest(new DeleteDestination(agentId));
  }

  /**
   * Admin method setting free reading access to this destination.
   * <p>
   * The request fails if this destination is deleted server side.
   *
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public void setFreeReading() throws ConnectException, AdminException
  {
    AdminModule.doRequest(new SetReader(null, agentId));
  }

  /**
   * Admin method setting free writing access to this destination.
   * <p>
   * The request fails if this destination is deleted server side.
   *
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public void setFreeWriting() throws ConnectException, AdminException 
  {
    AdminModule.doRequest(new SetWriter(null, agentId));
  }

  /**
   * Admin method unsetting free reading access to this destination.
   * <p>
   * The request fails if this destination is deleted server side.
   *
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public void unsetFreeReading() throws ConnectException, AdminException
  {
    AdminModule.doRequest(new UnsetReader(null, agentId));
  }

  /**
   * Admin method unsetting free writing access to this destination.
   * <p>
   * The request fails if this destination is deleted server side.
   *
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public void unsetFreeWriting() throws ConnectException, AdminException
  {
    AdminModule.doRequest(new UnsetWriter(null, agentId));
  }

  /**
   * Admin method setting a given user as a reader on this destination.
   * <p>
   * The request fails if this destination is deleted server side.
   *
   * @param user  User to be set as a reader.
   *
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public void setReader(User user) throws ConnectException, AdminException 
  {
    AdminModule.doRequest(new SetReader(user.getProxyId(), agentId));
  }

  /**
   * Admin method setting a given user as a writer on this destination.
   * <p>
   * The request fails if this destination is deleted server side.
   *
   * @param user  User to be set as a writer.
   *
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public void setWriter(User user) throws ConnectException, AdminException
  {
    AdminModule.doRequest(new SetWriter(user.getProxyId(), agentId));
  }

  /**
   * Admin method unsetting a given user as a reader on this destination.
   * <p>
   * The request fails if this destination is deleted server side.
   *
   * @param user  Reader to be unset.
   *
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public void unsetReader(User user) throws ConnectException, AdminException
  {
    AdminModule.doRequest(new UnsetReader(user.getProxyId(), agentId));
  }

  /**
   * Admin method unsetting a given user as a writer on this destination.
   * <p>
   * The request fails if this destination is deleted server side.
   *
   * @param user  Writer to be unset.
   *
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public void unsetWriter(User user) throws ConnectException, AdminException
  {
    AdminModule.doRequest(new UnsetWriter(user.getProxyId(), agentId));
  }


  /**
   * Admin method setting or unsetting a dead message queue for this
   * destination.
   * <p>
   * The request fails if this destination is deleted server side.
   *
   * @param dmq  The dead message queue to be set (<code>null</code> for
   *             unsetting current DMQ).
   *
   * @exception IllegalArgumentException  If the DMQ is not a valid 
   *              JORAM destination.
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public void setDMQ(DeadMQueue dmq) throws ConnectException, AdminException
  {
    if (dmq == null)
      AdminModule.doRequest(new UnsetDestinationDMQ(agentId));
    else
      AdminModule.doRequest(new SetDestinationDMQ(agentId, dmq.getName()));
  }

  /**
   * Monitoring method returning the list of all users that have a reading
   * permission on this destination, or an empty list if no specific readers
   * are set.
   * <p>
   * The request fails if the destination is deleted server side.
   *
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public List getReaders() throws ConnectException, AdminException
  {
    Monitor_GetReaders request = new Monitor_GetReaders(agentId);
    Monitor_GetUsersRep reply =
      (Monitor_GetUsersRep) AdminModule.doRequest(request);

    Vector list = new Vector();
    Hashtable users = reply.getUsers();
    String name;
    for (Enumeration names = users.keys(); names.hasMoreElements();) {
      name = (String) names.nextElement();
      list.add(new User(name, (String) users.get(name)));
    }
    return list;
  }

  /**
   * Monitoring method returning the list of all users that have a writing
   * permission on this destination, or an empty list if no specific writers
   * are set.
   * <p>
   * The request fails if the destination is deleted server side.
   *
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public List getWriters() throws ConnectException, AdminException
  {
    Monitor_GetWriters request = new Monitor_GetWriters(agentId);
    Monitor_GetUsersRep reply =
      (Monitor_GetUsersRep) AdminModule.doRequest(request);

    Vector list = new Vector();
    Hashtable users = reply.getUsers();
    String name;
    for (Enumeration names = users.keys(); names.hasMoreElements();) {
      name = (String) names.nextElement();
      list.add(new User(name, (String) users.get(name)));
    }
    return list;
  }

  /**
   * Monitoring method returning <code>true</code> if this destination
   * provides free READ access.
   * <p>
   * The request fails if the destination is deleted server side.
   *
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public boolean isFreelyReadable() throws ConnectException, AdminException
  {
    Monitor_GetFreeAccess request = new Monitor_GetFreeAccess(agentId);
    Monitor_GetFreeAccessRep reply;
    reply = (Monitor_GetFreeAccessRep) AdminModule.doRequest(request);

    return reply.getFreeReading();
  }

  /**
   * Monitoring method returning <code>true</code> if this destination
   * provides free WRITE access.
   * <p>
   * The request fails if the destination is deleted server side.
   *
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public boolean isFreelyWriteable() throws ConnectException, AdminException
  {
    Monitor_GetFreeAccess request = new Monitor_GetFreeAccess(agentId);
    Monitor_GetFreeAccessRep reply;
    reply = (Monitor_GetFreeAccessRep) AdminModule.doRequest(request);

    return reply.getFreeWriting();
  }

  /** 
   * Monitoring method returning the dead message queue of this destination,
   * null if not set.
   * <p>
   * The request fails if the destination is deleted server side.
   *
   * @exception ConnectException  If the admin connection is closed or broken.
   * @exception AdminException  If the request fails.
   */
  public DeadMQueue getDMQ() throws ConnectException, AdminException
  {
    Monitor_GetDMQSettings request = new Monitor_GetDMQSettings(agentId);
    Monitor_GetDMQSettingsRep reply;
    reply = (Monitor_GetDMQSettingsRep) AdminModule.doRequest(request);
    
    if (reply.getDMQName() == null)
      return null;
    else
      return new DeadMQueue(reply.getDMQName());
  }
}
