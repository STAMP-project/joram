/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2001 - 2010 ScalAgent Distributed Technologies
 * Copyright (C) 1996 - 2000 Dyade
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
 * Contributor(s): ScalAgent Distributed Technologies
 */
package org.objectweb.joram.mom.dest;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.management.MBeanAttributeInfo;
import javax.management.ObjectName;

import org.objectweb.joram.mom.notifications.AdminReply;
import org.objectweb.joram.mom.notifications.ClientMessages;
import org.objectweb.joram.mom.notifications.DestinationAdminRequestNot;
import org.objectweb.joram.mom.notifications.Monit_FreeAccess;
import org.objectweb.joram.mom.notifications.Monit_FreeAccessRep;
import org.objectweb.joram.mom.notifications.Monit_GetDMQSettings;
import org.objectweb.joram.mom.notifications.Monit_GetDMQSettingsRep;
import org.objectweb.joram.mom.notifications.Monit_GetReaders;
import org.objectweb.joram.mom.notifications.Monit_GetStat;
import org.objectweb.joram.mom.notifications.Monit_GetStatRep;
import org.objectweb.joram.mom.notifications.Monit_GetUsersRep;
import org.objectweb.joram.mom.notifications.Monit_GetWriters;
import org.objectweb.joram.mom.notifications.RequestGroupNot;
import org.objectweb.joram.mom.notifications.SetDMQRequest;
import org.objectweb.joram.mom.notifications.SetRightRequest;
import org.objectweb.joram.mom.notifications.SpecialAdminRequest;
import org.objectweb.joram.mom.notifications.WakeUpNot;
import org.objectweb.joram.mom.proxies.SendRepliesNot;
import org.objectweb.joram.mom.proxies.SendReplyNot;
import org.objectweb.joram.mom.util.DMQManager;
import org.objectweb.joram.shared.MessageErrorConstants;
import org.objectweb.joram.shared.excepts.AccessException;
import org.objectweb.joram.shared.excepts.MessageValueException;
import org.objectweb.joram.shared.excepts.RequestException;
import org.objectweb.joram.shared.messages.ConversionHelper;
import org.objectweb.joram.shared.messages.Message;
import org.objectweb.util.monolog.api.BasicLevel;
import org.objectweb.util.monolog.api.Logger;

import fr.dyade.aaa.agent.AgentId;
import fr.dyade.aaa.agent.Channel;
import fr.dyade.aaa.agent.DeleteNot;
import fr.dyade.aaa.agent.Notification;
import fr.dyade.aaa.agent.UnknownAgent;
import fr.dyade.aaa.common.Debug;
import fr.dyade.aaa.util.management.MXWrapper;

/**
 * The <code>DestinationImpl</code> class implements the common behavior of
 * MOM destinations.
 */
public abstract class DestinationImpl implements java.io.Serializable, DestinationImplMBean {  
  public static Logger logger = Debug.getLogger(DestinationImpl.class.getName());
  
  public static final String WAKEUP_PERIOD = "period";
  
  /**
   * <code>true</code> if the destination successfully processed a deletion
   * request.
   */
  private boolean deletable = false;

  /** period to run task at regular interval: cleaning, load-balancing, etc. */
  private long period = -1L;
  
  /**
   * Identifier of the destination's administrator.
   * In any case the local administration topic is authorized to handle the
   * destination, this mechanism allows an other agent to get the same rights.
   */
  private AgentId adminId;

  /**
   * Reference to the agent hosting the destination.
   */
  protected transient Destination agent;
  
  /** Identifier of the agent hosting the destination. */
  public final AgentId getId() {
  	return (agent == null)?AgentId.nullId:agent.getId();
  }

  /** <code>true</code> if the READ access is granted to everybody. */
  protected boolean freeReading = false;
  /** <code>true</code> if the WRITE access is granted to everybody. */
  protected boolean freeWriting = false;
  /** Table of the destination readers and writers. */
  protected Hashtable clients;

  /** READ access value. */
  public static int READ = 1;
  /** WRITE access value. */
  public static int WRITE = 2;
  /** READ and WRITE access value. */
  public static int READWRITE = 3;

  /**
   * Identifier of the dead message queue this destination must send its
   * dead messages to, if any.
   */
  protected AgentId dmqId = null;

  /**
   * Transient <code>StringBuffer</code> used to build message, this buffer
   * is created during agent initialization, then reused during the destination
   * life.
   */
  transient StringBuffer strbuf;

  /**
   * date of creation.
   */
  public long creationDate = -1;

  protected long nbMsgsReceiveSinceCreation = 0;
  protected long nbMsgsDeliverSinceCreation = 0;
  protected long nbMsgsSentToDMQSinceCreation = 0;

  /**
   * Constructs a <code>DestinationImpl</code>.
   *
   * @param adminId  Identifier of the administrator of the destination.
   * @param prop     The initial set of properties.
   */ 
  public DestinationImpl(AgentId adminId, Properties prop) {
    this.adminId = adminId;
    clients = new Hashtable();
    strbuf = new StringBuffer();

    if (creationDate == -1)
      creationDate = System.currentTimeMillis();

    try {
      if (prop != null)
        period = ConversionHelper.toLong(prop.get(WAKEUP_PERIOD));
    } catch (MessageValueException e) {
      period = -1L;
    } catch (NumberFormatException e) {
      period = -1L;
    }
    
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, this + ", created.");
  }

  void setNoSave() {
    if (agent != null) {
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, this + ".setNoSave().");
      agent.setNoSave();
    }
  }

  void setSave() {
    if (agent != null) {
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, this + ".setSave().");
      agent.setSave();
    }
  }

	/**
	 * Initializes the destination.
	 * 
   * @param firstTime		true when first called by the factory
	 */
	public abstract void initialize(boolean firstTime);

  public void setAgent(Destination agent) {
    // state change, so save.
    setSave();
    this.agent = agent;
  }

  protected boolean isLocal(AgentId id) {
    return (getId().getTo() == id.getTo());
  }

  /** Returns <code>true</code> if the destination might be deleted. */
  public boolean canBeDeleted() {
    return deletable;
  }

  /**
   * Returns  the period value of this destination, -1 if not set.
   *
   * @return the period value of this destination; -1 if not set.
   */
  public long getPeriod() {
    return period;
  }

  /**
   * Sets or unsets the period for this destination.
   *
   * @param period The period value to be set or -1 for unsetting previous
   *               value (ignore 0).
   */
  public void setPeriod(long period) {
    if (this.period != period) {
      // Schedule the task.
      WakeUpNot not = new WakeUpNot();
      not.update = true;
      forward(getId(), not);
      this.period = period;
    }
  }
  
  public abstract void wakeUpNot(WakeUpNot not);
    
  /**
   * Method implementing the reaction to a <code>SetRightRequest</code>
   * notification requesting rights to be set for a user.
   *
   * @exception AccessException  If the requester is not the administrator.
   */
  public void setRightRequest(AgentId from, SetRightRequest not) throws AccessException {
    if (! isAdministrator(from))
      throw new AccessException("ADMIN right not granted");

    AgentId user = not.getClient();
    int right = not.getRight();
    String info;

    try {
      processSetRight(user,right);
      doRightRequest(not);
      info = strbuf.append("Request [")
        .append(not.getClass().getName())
        .append("], sent to Destination [")
        .append(getId())
        .append("], successful [true]: user [")
        .append(user)
        .append("] set with right [" + right +"]").toString();
      strbuf.setLength(0);
      forward(from, new AdminReply(not, true, info)); 
    } catch (RequestException exc) {
      info = strbuf.append("Request [")
        .append(not.getClass().getName())
        .append("], sent to Destination [")
        .append(getId())
        .append("], successful [false]: ")
        .append(exc.getMessage()).toString();
      strbuf.setLength(0);
      forward(from, new AdminReply(not, false, info));
    }

    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, info);
  }

  /** set user right. */
  protected void processSetRight(AgentId user, int right) 
    throws RequestException {
    // state change, so save.
    setSave();

    // Setting "all" users rights:
    if (user == null) {
      if (right == READ)
        freeReading = true;
      else if (right == WRITE)
        freeWriting = true;
      else if (right == -READ)
        freeReading = false;
      else if (right == -WRITE)
        freeWriting = false;
      else
        throw new RequestException("Invalid right value: " + right);
    } else {
      // Setting a specific user right:
      Integer currentRight = (Integer) clients.get(user);
      if (right == READ) {
        if (currentRight != null && currentRight.intValue() == WRITE)
          clients.put(user, new Integer(READWRITE));
        else
          clients.put(user, new Integer(READ));
      } else if (right == WRITE) {
        if (currentRight != null && currentRight.intValue() == READ)
          clients.put(user, new Integer(READWRITE));
        else
          clients.put(user, new Integer(WRITE));
      } else if (right == -READ) {
        if (currentRight != null && currentRight.intValue() == READWRITE)
          clients.put(user, new Integer(WRITE));
        else if (currentRight != null && currentRight.intValue() == READ)
          clients.remove(user);
      } else if (right == -WRITE) {
        if (currentRight != null && currentRight.intValue() == READWRITE)
          clients.put(user, new Integer(READ));
        else if (currentRight != null && currentRight.intValue() == WRITE)
          clients.remove(user);
      } else
        throw new RequestException("Invalid right value: " + right);
    }
  }
    
  /**
   * Method implementing the reaction to a <code>SetDMQRequest</code>
   * notification setting the dead message queue identifier for this
   * destination.
   *
   * @exception AccessException  If the requester is not the administrator.
   */
  public void setDMQRequest(AgentId from, SetDMQRequest not) throws AccessException {
    if (! isAdministrator(from))
      throw new AccessException("ADMIN right not granted");

    // state change, so save.
    setSave();

    dmqId = not.getDmqId();
    
    String info = strbuf.append("Request [")
      .append(not.getClass().getName())
      .append("], sent to Destination [")
      .append(getId())
      .append("], successful [true]: dmq [")
      .append(dmqId)
      .append("] set").toString();
    strbuf.setLength(0);
    forward(from, new AdminReply(not, true, info));
    
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, info);
  }

  /**
   * Method implementing the reaction to a <code>Monit_GetReaders</code>
   * notification requesting the identifiers of the destination's readers.
   *
   * @exception AccessException  If the requester is not the administrator.
   */
  public void monitGetReaders(AgentId from, Monit_GetReaders not) throws AccessException {
    if (! isAdministrator(from))
      throw new AccessException("ADMIN right not granted");

    AgentId key;
    int right;
    Vector readers = new Vector();
    for (Enumeration keys = clients.keys(); keys.hasMoreElements();) {
      key = (AgentId) keys.nextElement();
      right = ((Integer) clients.get(key)).intValue();

      if (right == READ || right == READWRITE)
        readers.add(key);
    }
    forward(from, new Monit_GetUsersRep(not, readers));
  }

  /**
   * Method implementing the reaction to a <code>Monit_GetWriters</code>
   * notification requesting the identifiers of the destination's writers.
   *
   * @exception AccessException  If the requester is not the administrator.
   */
  public void monitGetWriters(AgentId from, Monit_GetWriters not) throws AccessException {
    if (! isAdministrator(from))
      throw new AccessException("ADMIN right not granted");

    AgentId key;
    int right;
    Vector writers = new Vector();
    for (Enumeration keys = clients.keys(); keys.hasMoreElements();) {
      key = (AgentId) keys.nextElement();
      right = ((Integer) clients.get(key)).intValue();

      if (right == WRITE || right == READWRITE)
        writers.add(key);
    }
    forward(from, new Monit_GetUsersRep(not, writers));
  }

  public static String[] _rights = {":R;", ";W;", ":RW;"};

  /**
   * Returns a string representation of the rights set on this destination.
   *
   * @return the rights set on this destination.
   */
  public String[] getRights() {
    String rigths[] = new String[clients.size()];

    AgentId key;
    int right;

    int i=0;
    for (Enumeration keys = clients.keys(); keys.hasMoreElements();) {
      key = (AgentId) keys.nextElement();
      right = ((Integer) clients.get(key)).intValue();
      rigths[i++] = key.toString() + _rights[right -1];
    }

    return rigths;
  }

  /**
   * Returns a string representation of rights set on this destination for a
   * particular user. The user is pointed out by its unique identifier.
   *
   * @param userid The user's unique identifier.
   * @return the rights set on this destination.
   */
  public String getRight(String userid) {
    AgentId key = AgentId.fromString(userid);
    if (key == null) return userid + ":bad user;";
    Integer right = (Integer) clients.get(key);
    if (right == null) return userid + ":unknown;";

    return userid + _rights[right.intValue() -1];
  }

//   public void setRight(String userid, String right) {
//     AgentId key = AgentId.fromString(userid);

//     // To be continued
//   }

  /**
   * Method implementing the reaction to a <code>Monit_FreeAccess</code>
   * notification requesting the free access status of this destination.
   *
   * @exception AccessException  If the requester is not the administrator.
   */
  public void monitFreeAccess(AgentId from, Monit_FreeAccess not) throws AccessException {
    if (! isAdministrator(from))
      throw new AccessException("ADMIN right not granted");

    forward(from, new Monit_FreeAccessRep(not, freeReading, freeWriting));
  }

  /**
   * Method implementing the reaction to a <code>Monit_GetDMQSettings</code>
   * notification requesting the destination's DMQ settings.
   *
   * @exception AccessException  If the requester is not the administrator.
   */
  public void monitGetDMQSettings(AgentId from, Monit_GetDMQSettings not) throws AccessException {
    if (! isAdministrator(from))
      throw new AccessException("ADMIN right not granted");

    String id = null;
    if (dmqId != null)
      id = dmqId.toString();

    forward(from, new Monit_GetDMQSettingsRep(not, id, null));
  }

  /**
   * Method implementing the reaction to a <code>Monit_GetStat</code>
   * notification requesting to get statistic of this destination.
   *
   * @exception AccessException  If the requester is not the administrator.
   */
  public final void monitGetStat(AgentId from, Monit_GetStat not) throws AccessException {
    if (! isAdministrator(from))
      throw new AccessException("ADMIN right not granted");
    forward(from, new Monit_GetStatRep(not, getJMXStatistics()));
  }

  /**
   * This method allows to exclude some JMX attribute of getJMXStatistics method.
   * It must be overloaded in subclass.
   * 
   * @param attrName name of attribute to test.
   * @return true if the attribute is a valid one.
   */
  protected boolean isValidJMXAttribute(String attrName) {
    if (attrName == null)
      return false;
    return true;
  }
  
  /**
   * Returns values of all valid JMX attributes about the destination.
   * 
   * @return a Hashtable containing the values of all valid JMX attributes about the destination.
   *         The keys are the name of corresponding attributes.
   */
  protected final Hashtable getJMXStatistics() {
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "DestinationImpl.getJMXStatistics()");

    Hashtable stats = null;

    try {
      ObjectName mbeanName = new ObjectName(agent.getMBeanName());

      MBeanAttributeInfo[] attributes = MXWrapper.getAttributes(mbeanName);
      if (attributes != null) {
        stats = new Hashtable(attributes.length);
        for (int k=0; k<attributes.length; k++) {
          String name = attributes[k].getName();
          if (isValidJMXAttribute(name)) {
            Object value = MXWrapper.getAttribute(mbeanName, name);
            if ((value != null) && ((value instanceof String) || (value instanceof Number)))
              stats.put(name, value);
          }
        }
      }
    } catch (Exception exc) {
      logger.log(BasicLevel.ERROR, " getAttributes  on " + agent.getMBeanName() + " error.", exc);
    }

    return stats;
  }

  /**
   * Method implementing the reaction to a <code>ClientMessages</code>
   * notification holding messages sent by a client.
   * <p>
   * If the sender is not a writer on the destination the messages are
   * sent to the DMQ and an exception is thrown. Otherwise, the processing of
   * the received messages is performed in subclasses.
   *
   * @exception AccessException  If the sender is not a WRITER on the
   *              destination.
   */
  protected void clientMessages(AgentId from, ClientMessages not) throws AccessException {
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG,
                 "DestinationImpl.clientMessages(" + from + ',' + not + ')');

    // If sender is not a writer, sending the messages to the DMQ, and
    // throwing an exception:
    if (!isWriter(from)) {
      DMQManager dmqManager = new DMQManager(not.getDMQId(), dmqId, getId());
      Message msg;
      for (Iterator msgs = not.getMessages().iterator(); msgs.hasNext();) {
        msg = (Message) msgs.next();
        nbMsgsSentToDMQSinceCreation++;
        dmqManager.addDeadMessage(msg, MessageErrorConstants.NOT_WRITEABLE);
        handleDeniedMessage(msg.id, AgentId.fromString(msg.replyToId));
      }
      dmqManager.sendToDMQ();
      throw new AccessException("WRITE right not granted");
    }

    doClientMessages(from, not);

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // for topic performance : must send reply after process ClientMessage
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    if (!not.isPersistent() && !not.getAsyncSend()) {
      forward(from, new SendReplyNot(not.getClientContext(), not.getRequestId()));
    }
  }

  /**
   * Method used to do specific actions when a message is denied because of a
   * lack of rights.
   */
  protected void handleDeniedMessage(String msgId, AgentId replyTo) {
    // Nothing to do, useful in admin topic
  }

  /**
   * Method implementing the reaction to an <code>UnknownAgent</code>
   * notification.
   * <p>
   * If the unknown agent is the DMQ, its identifier is set to null. If it
   * is a client of the destination, it is removed. Specific processing is
   * also done in subclasses.
   */
  public void unknownAgent(AgentId from, UnknownAgent not) {
    if (isAdministrator(not.agent)) {
      if (logger.isLoggable(BasicLevel.ERROR))
            logger.log(BasicLevel.ERROR,
                       "Admin of dest " + getId() + " does not exist anymore.");
    } else if (not.agent.equals(dmqId)) {
      // state change, so save.
      setSave();
      dmqId = null;
    } else {
      // state change, so save.
      setSave();
      clients.remove(from);
      doUnknownAgent(not);
    }
  }

  /**
   * Method implementing the reaction to a <code>DeleteNot</code>
   * notification requesting the deletion of the destination.
   * <p>
   * The processing is done in subclasses if the sender is an administrator.
   */
  public void deleteNot(AgentId from, DeleteNot not) {
    if (! isAdministrator(from)) {
      if (logger.isLoggable(BasicLevel.WARN))
        logger.log(BasicLevel.WARN,
                   "Unauthorized deletion request from " + from);
    } else {
      doDeleteNot(not);
      // state change, so save.
      setSave();
      deletable = true;
    }
  }

  /**
   * Method implementing the reaction to a <code>SpecialAdminRequest</code>
   * notification requesting the special administration of the destination.
   * <p>
   */
  public void specialAdminRequest(AgentId from, SpecialAdminRequest not) {
    String info;
    Object obj = null;

    // state change, so save.
    setSave();

    try {
      if (!isAdministrator(from)) {
        if (logger.isLoggable(BasicLevel.WARN))
          logger.log(BasicLevel.WARN, 
                     "Unauthorized SpecialAdminRequest request from " + from);
        throw new RequestException("ADMIN right not granted");
      }
      obj = specialAdminProcess(not);
      info = strbuf.append("Request [")
        .append(not.getClass().getName())
        .append("], sent to Destination [")
        .append(getId())
        .append("], successful [true] ").toString();
      strbuf.setLength(0);
      forward(from, new AdminReply(not, true, info, obj)); 
    } catch (RequestException exc) {
      info = strbuf.append("Request [")
        .append(not.getClass().getName())
        .append("], sent to Destination [")
        .append(getId())
        .append("], successful [false]: ")
        .append(exc.getMessage()).toString();
      strbuf.setLength(0);
      forward(from, new AdminReply(not, false, info, obj));
    }
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, info);
  }
  
  public void requestGroupNot(AgentId from, RequestGroupNot not) {
    Enumeration en = not.getClientMessages();
    ClientMessages theCM = (ClientMessages) en.nextElement();
    Vector replies = new Vector();
    replies.addElement(new SendReplyNot(
        theCM.getClientContext(), 
        theCM.getRequestId()));
    while (en.hasMoreElements()) {
      ClientMessages cm = (ClientMessages) en.nextElement();
      List msgs = cm.getMessages();
      for (int i = 0; i < msgs.size(); i++) {
        theCM.addMessage((Message) msgs.get(i));
      }
      if (! cm.getAsyncSend()) {
        replies.addElement(new SendReplyNot(
            cm.getClientContext(), 
            cm.getRequestId()));
      }
    }
    
    doClientMessages(from, theCM);

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // for topic performance : must send reply after process ClientMessage
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    if (!not.isPersistent() && replies.size() > 0) {
      forward(from, new SendRepliesNot(replies));
    }
  }
  
  protected Object specialAdminProcess(SpecialAdminRequest not) 
    throws RequestException {
    return null;
  }

  /**
   * Checks the reading permission of a given client agent.
   *
   * @param client  AgentId of the client requesting a reading permission.
   */
  protected boolean isReader(AgentId client) {
    if (isAdministrator(client) || freeReading)
      return true;

    Integer clientRight = (Integer) clients.get(client);
    if (clientRight == null)
      return false;
    else
      return ((clientRight.intValue() == READ) ||
              (clientRight.intValue() == READWRITE));
  }

  /**
   * Checks the writing permission of a given client agent.
   *
   * @param client  AgentId of the client requesting a writing permission.
   */
  protected boolean isWriter(AgentId client) {
    if (isAdministrator(client) || freeWriting)
      return true;

    Integer clientRight = (Integer) clients.get(client);
    if (clientRight == null)
      return false;
    else
      return ((clientRight.intValue() == WRITE) ||
              (clientRight.intValue() == READWRITE));
  }

  /**
   * Checks the administering permission of a given client agent.
   *
   * @param client  AgentId of the client requesting an admin permission.
   */
  protected boolean isAdministrator(AgentId client) {
    return client.equals(adminId) || client.equals(AdminTopic.getDefault());
  }

  abstract protected void doRightRequest(SetRightRequest not);
  abstract protected void doClientMessages(AgentId from, ClientMessages not);
  abstract protected void doUnknownAgent(UnknownAgent not);
  abstract protected void doDeleteNot(DeleteNot not);
  
  public SetRightRequest preProcess(SetRightRequest req) {
    // nothing to do
    return req;
  }
  
  public void postProcess(SetRightRequest req) {
    // nothing to do
  }
  
  public ClientMessages preProcess(AgentId from, ClientMessages msgs) {
    // nothing to do.
    return msgs;
  }
  
  public void postProcess(ClientMessages msgs) {
    // nothing to do.
  }
  
  private void writeObject(java.io.ObjectOutputStream out)
    throws IOException {
    out.writeBoolean(deletable);
    out.writeObject(adminId);
    out.writeBoolean(freeReading);
    out.writeBoolean(freeWriting);
    out.writeObject(clients);    
    out.writeObject(dmqId);
    out.writeLong(creationDate);
    out.writeLong(nbMsgsReceiveSinceCreation);
    out.writeLong(nbMsgsDeliverSinceCreation);
    out.writeLong(nbMsgsSentToDMQSinceCreation);
    out.writeLong(period);
  }

  private void readObject(java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {
    deletable = in.readBoolean();
    adminId = (AgentId)in.readObject();
    freeReading = in.readBoolean();
    freeWriting = in.readBoolean();
    clients = (Hashtable)in.readObject();
    dmqId = (AgentId)in.readObject();
    strbuf = new StringBuffer();
    creationDate = in.readLong();
    nbMsgsReceiveSinceCreation = in.readLong();
    nbMsgsDeliverSinceCreation = in.readLong();
    nbMsgsSentToDMQSinceCreation = in.readLong();
    period = in.readLong();
  }

  // DestinationMBean interface

  /**
   * Returns the unique identifier of the destination.
   *
   * @return the unique identifier of the destination.
   */
  public final String getDestinationId() {
    return getId().toString();
  }

  /**
   * Tests if this destination is free for reading.
   *
   * @return true if anyone can receive messages from this destination;
   * 	     false otherwise.
   */
  public boolean isFreeReading() {
    return freeReading;
  }

  /**
   * Sets the <code>FreeReading</code> attribute for this destination.
   *
   * @param on	if true anyone can receive message from this destination.
   */
  public void setFreeReading(boolean on) {
    // state change, so save.
    setSave();
    freeReading = on;
  }

  /**
   * Tests if this destination is free for writing.
   *
   * @return true if anyone can send messages to this destination;
   * 	     false otherwise.
   */
  public boolean isFreeWriting() {
    return freeWriting;
  }

  /**
   * Sets the <code>FreeWriting</code> attribute for this destination.
   *
   * @param on	if true anyone can send message to this destination.
   */
  public void setFreeWriting(boolean on) {
    // state change, so save.
    setSave();
    freeWriting = on;
  }

  /**
   * Return the unique identifier of DMQ set for this destination if any.
   *
   * @return the unique identifier of DMQ set for this destination if any;
   *	     null otherwise.
   */
  public String getDMQId() {
    if (dmqId != null) 
      return dmqId.toString();
    return null;
  }

  public AgentId getDMQAgentId() {
    return dmqId;
  }

  /**
   * Returns this destination creation time as a long.
   *
   * @return the destination creation time as UTC milliseconds from the epoch.
   */
  public long getCreationTimeInMillis() {
    return creationDate;
  }

  /**
   * Returns this destination creation time through a <code>String</code> of
   * the form: <code>dow mon dd hh:mm:ss zzz yyyy</code>.
   *
   * @return the destination creation time.
   */
  public String getCreationDate() {
    return new Date(creationDate).toString();
  }

  /**
   * Returns the number of messages received since creation time of this
   * destination.
   *
   * @return the number of messages received since creation time.
   */
  abstract public long getNbMsgsReceiveSinceCreation();
  
  /**
   * Returns the number of messages delivered since creation time of this
   * destination. It includes messages all delivered messages to a consumer,
   * already acknowledged or not.
   *
   * @return the number of messages delivered since creation time.
   */
  public long getNbMsgsDeliverSinceCreation() {
    return nbMsgsDeliverSinceCreation;
  }

  /**
   * Returns the number of erroneous messages forwarded to the DMQ since
   * creation time of this destination..
   *
   * @return the number of erroneous messages forwarded to the DMQ.
   */
  public long getNbMsgsSentToDMQSinceCreation() {
    return nbMsgsSentToDMQSinceCreation;
  }

  protected void replyToTopic(org.objectweb.joram.shared.admin.AdminReply reply,
                              AgentId replyTo,
                              String requestMsgId,
                              String replyMsgId) {
    Message message = new Message();
    message.correlationId = requestMsgId;
    message.timestamp = System.currentTimeMillis();
    message.setDestination(replyTo.toString(), message.TOPIC_TYPE);
    message.id = replyMsgId;
    try {
      message.setAdminMessage(reply);
      ClientMessages clientMessages = new ClientMessages(-1, -1, message);
      forward(replyTo, clientMessages);
    } catch (Exception exc) {
      if (logger.isLoggable(BasicLevel.ERROR))
        logger.log(BasicLevel.ERROR, "", exc);
      throw new Error(exc.getMessage());
    }
  }
  
  public final void forward(AgentId to, Notification not) {
    Channel.sendTo(to, not);
  }
  
  public abstract void destinationAdminRequestNot(AgentId from, DestinationAdminRequestNot not);
  
}
