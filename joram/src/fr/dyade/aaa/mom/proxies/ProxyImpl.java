/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2001 - 2004 ScalAgent Distributed Technologies
 * Copyright (C) 1996 - 2003 Bull SA
 * Copyright (C) 1996 - 2001 Dyade
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
 * Contributor(s):       Andre Freyssinet (ScalAgent D.T.)
 */
package fr.dyade.aaa.mom.proxies;

import fr.dyade.aaa.agent.AgentId;
import fr.dyade.aaa.agent.AgentServer;
import fr.dyade.aaa.agent.DeleteNot;
import fr.dyade.aaa.agent.Notification;
import fr.dyade.aaa.agent.UnknownAgent;
import fr.dyade.aaa.agent.UnknownNotificationException;
import fr.dyade.aaa.mom.MomTracing;
import fr.dyade.aaa.mom.admin.DeleteUser;
import fr.dyade.aaa.mom.admin.UpdateUser;
import fr.dyade.aaa.mom.comm.*;
import fr.dyade.aaa.mom.dest.*;
import fr.dyade.aaa.mom.excepts.*;
import fr.dyade.aaa.mom.jms.*;
import fr.dyade.aaa.mom.messages.*;

import org.objectweb.util.monolog.api.BasicLevel;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 * The <code>ProxyImpl</code> class implements the MOM proxy behaviour,
 * basically forwarding client requests to MOM destinations and MOM
 * destinations replies to clients.
 */ 
public class ProxyImpl implements ProxyImplMBean, java.io.Serializable
{
  /** <code>true</code> if this proxy is an administrator's proxy. */
  private boolean admin = false;
  /** <code>true</code> if the administrator did not connect yet. */
  private boolean firstAdminConnection = true;
  /** Administrator's initial name. */
  private String initialAdminName;
  /** Administrator's initial password. */
  private String initialAdminPass;

  // Flow control objects and parameters
  private static Object lock = new Object();
  /**
   * Flow control duration (in ms) between two message sendings
   * (-1 for no flow control).
   */
  private static int inFlow = -1;
  private static long flowControl = 0;
  private static long start = 0L;
  private static long end = 0L;
  private static int nbmsg = 0;

  /**
   * Identifier of this proxy dead message queue, <code>null</code> for DMQ
   * not set.
   */
  private AgentId dmqId = null;
  /**
   * Threshold value, 0 or negative for no threshold, <code>null</code> for
   * value not set.
   */
  private Integer threshold = null; 

  /**
   * Table of the proxy's <code>ClientContext</code> instances.
   * <p>
   * <b>Key:</b> context identifier<br>
   * <b>Value:</b> context
   */
  private Hashtable contexts;
  /**
   * Table holding the <code>ClientSubscription</code> instances.
   * <p>
   * <b>Key:</b> subsription name<br>
   * <b>Value:</b> client subscription
   */
  private Hashtable subsTable;
  /** The module used by the proxy's subscriptions for persisting messages. */
  private PersistenceModule msgsPersistenceModule;
  /** Counter of message arrivals from topics. */
  private long arrivalsCounter = 0;

  /** The reference of the agent hosting the proxy. */
  private transient ProxyAgentItf proxyAgent;
  /** 
   * Table holding the <code>TopicSubscription</code> instances.
   * <p>
   * <b>Key:</b> topic identifier<br>
   * <b>Value:</b> topic subscription
   */
  private transient Hashtable topicsTable;
  /**
   * Table holding the subsriptions' messages.
   * <p>
   * <b>Key:</b> message identifier<br>
   * <b>Value:</b> message
   */
  private transient Hashtable messagesTable;

  /** Identifier of the active context. */
  private transient int activeCtxId = 0;
  /** Reference to the active <code>ClientContext</code> instance. */
  private transient ClientContext activeCtx = null;

  
  /**
   * Constructs a <code>ProxyImpl</code> instance.
   */
  public ProxyImpl()
  {
    contexts = new Hashtable();
    subsTable = new Hashtable();

    inFlow = fr.dyade.aaa.mom.proxies.tcp.ConnectionFactory.inFlow;

    if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
      MomTracing.dbgProxy.log(BasicLevel.DEBUG, this + ": created.");
  }

  /**
   * Constructs a <code>ProxyImpl</code> instance for an administrator.
   *
   * @param name  Name of the administrator.
   * @param pass  Password of the administrator.
   */
  public ProxyImpl(String name, String pass)
  {
    admin = true;
    initialAdminName = name;
    initialAdminPass = pass;
    contexts = new Hashtable();
    subsTable = new Hashtable();

    if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
      MomTracing.dbgProxy.log(BasicLevel.DEBUG, this + ": created.");
  }


  public String toString()
  {
    if (proxyAgent == null)
      return "ProxyImpl:";
    else
      return "ProxyImpl:" + proxyAgent.getAgentId();
  }


  /**
   * (Re)initializes the proxy.
   *
   * @exception Exception  If the proxy state could not be fully retrieved,
   *              leading to an inconsistent state.
   */
  public void initialize(boolean firstTime, ProxyAgentItf proxyAgent)
              throws Exception
  {
    if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
      MomTracing.dbgProxy.log(BasicLevel.DEBUG,
                              "--- " + this + " (re)initializing...");
 
    this.proxyAgent = proxyAgent;

    topicsTable = new Hashtable();
    messagesTable = new Hashtable();

    // End of first initialization.
    if (firstTime) {
      msgsPersistenceModule = new PersistenceModule(proxyAgent.getAgentId()); 
      return;
    }

    // Re-initializing after a crash or a server stop.

    // Browsing the pre-crash contexts:
    ClientContext activeCtx;
    AgentId destId;
    for (Enumeration ctxIds = contexts.keys(); ctxIds.hasMoreElements();) {
      activeCtx = (ClientContext) contexts.remove(ctxIds.nextElement());

      // Denying the non acknowledged messages:
      for (Enumeration queueIds = activeCtx.getDeliveringQueues();
           queueIds.hasMoreElements();) {
        destId = (AgentId) queueIds.nextElement();
        proxyAgent.sendNot(destId, new DenyRequest(activeCtx.getId()));

        if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
          MomTracing.dbgProxy.log(BasicLevel.DEBUG,
                                  "Denies messages on queue "
                                  + destId.toString());
      }
      // Deleting the temporary destinations:
      for (Enumeration tempDests = activeCtx.getTempDestinations();
           tempDests.hasMoreElements();) {
        destId = (AgentId) tempDests.nextElement();
        proxyAgent.sendNot(destId, new DeleteNot());
  
        if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
          MomTracing.dbgProxy.log(BasicLevel.DEBUG,
                                  "Deletes temporary destination "
                                  + destId.toString());
      }
    }

    // Retrieving the subscriptions' messages.
    Vector messages = msgsPersistenceModule.loadAll();
    msgsPersistenceModule.deleteAll();
    
    // Browsing the pre-crash subscriptions:
    String subName;
    ClientSubscription cSub;
    Vector topics = new Vector();
    TopicSubscription tSub;
    for (Enumeration subNames = subsTable.keys();
         subNames.hasMoreElements();) {
      subName = (String) subNames.nextElement();
      cSub = (ClientSubscription) subsTable.get(subName);
      destId = cSub.getTopicId();
      if (! topics.contains(destId))
        topics.add(destId);
      // Deleting the non durable subscriptions.
      if (! cSub.getDurable())
        subsTable.remove(subName);
      // Reinitializing the durable ones.
      else {
        cSub.reinitialize(msgsPersistenceModule, messagesTable, messages);
        tSub = (TopicSubscription) topicsTable.get(destId);
        if (tSub == null) {
          tSub = new TopicSubscription();
          topicsTable.put(destId, tSub);
        }
        tSub.putSubscription(subName, cSub.getSelector());
      }
    }
    // Browsing the topics and updating their subscriptions.
    for (Enumeration topicIds = topics.elements();
         topicIds.hasMoreElements();)
      updateSubscriptionToTopic((AgentId) topicIds.nextElement(), -1, -1);

    // Commiting the persistence requests.
    msgsPersistenceModule.commit();
  }


  /**
   * Method processing clients requests.
   * <p>
   * Some of the client requests are directly forwarded, some others are
   * sent to the proxy so that their processing occurs in a transaction.
   * <p>
   * A <code>MomExceptionReply</code> wrapping a <code>RequestException</code>
   * might be sent back if a target destination can't be identified.
   */
  public void reactToClientRequest(int key, AbstractJmsRequest request)
  {
    try {
      if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG)) {
        MomTracing.dbgProxy.log(BasicLevel.DEBUG,
                                "--- " + this
                                + " got " + request.getClass().getName()
                                + " with id: " + request.getRequestId()
                                + " through activeCtx: " + key);
      }
      // Requests directly processed:
      if (request instanceof ProducerMessages)
        reactToClientRequest(key, (ProducerMessages) request);
      else if (request instanceof ConsumerReceiveRequest)
        reactToClientRequest(key, (ConsumerReceiveRequest) request);
      else if (request instanceof ConsumerSetListRequest)
        reactToClientRequest(key, (ConsumerSetListRequest) request);
      else if (request instanceof QBrowseRequest)
        reactToClientRequest(key, (QBrowseRequest) request);
      // Other requests are forwarded to the proxy:
      else
        proxyAgent.sendNot(proxyAgent.getAgentId(),
                           new RequestNotification(key, request));
    }
    // Catching an exception due to an invalid agent identifier to
    // forward the request to:
    catch (IllegalArgumentException iE) {
      DestinationException dE =
        new DestinationException("Proxy could not forward the request to"
                                 + " incorrectly identified destination: "
                                 + iE);

      doReply(key, new MomExceptionReply(request.getRequestId(), dE));
    }
  }

  /**
   * Forwards the messages sent by the client in a
   * <code>ProducerMessages</code> request as a <code>ClientMessages</code>
   * MOM request directly to a destination, and acknowledges them by sending
   * a <code>ServerReply</code> back.
   */
  private void reactToClientRequest(int key, ProducerMessages req)
  {
    ClientMessages not = new ClientMessages(key,
                                            req.getRequestId(),
                                            req.getMessages());

    // Setting the producer's DMQ identifier field: 
    if (dmqId != null)
      not.setDMQId(dmqId);
    else
      not.setDMQId(DeadMQueueImpl.getId());

    proxyAgent.sendNot(AgentId.fromString(req.getTarget()), not);
    doReply(key, new ServerReply(req));

    if (inFlow != -1) {
      synchronized (lock) {
        if (start == 0L) start = System.currentTimeMillis();
        nbmsg += 1;
        if (nbmsg == inFlow) {
          end = System.currentTimeMillis();
          flowControl = 1000L - (end - start);
          if (flowControl > 0) {
            try {
              Thread.sleep(flowControl);
            } catch (InterruptedException exc) {}
            start = System.currentTimeMillis();
          } else {
            start = end;
          }
          nbmsg = 0;
        }
      }
    }
  }

  /**
   * Either forwards the <code>ConsumerReceiveRequest</code> request as a
   * <code>ReceiveRequest</code> directly to the target queue, or wraps it
   * and sends it to the proxy if destinated to a subscription.
   */
  private void reactToClientRequest(int key, ConsumerReceiveRequest req)
  {
    if (req.getQueueMode()) {
      AgentId to = AgentId.fromString(req.getTarget());
      proxyAgent.sendNot(to,
                         new ReceiveRequest(key,
                                            req.getRequestId(),
                                            req.getSelector(),
                                            req.getTimeToLive(),
                                            false));
    }
    else
      proxyAgent.sendNot(proxyAgent.getAgentId(),
                         new RequestNotification(key, req));
  }

  /**
   * Either forwards the <code>ConsumerSetListRequest</code> request as a
   * <code>ReceiveRequest</code> directly to the target queue, or wraps it
   * and sends it to the proxy if destinated to a subscription.
   */
  private void reactToClientRequest(int key, ConsumerSetListRequest req)
  {
    if (req.getQueueMode()) {
      AgentId to = AgentId.fromString(req.getTarget());
      proxyAgent.sendNot(to, new ReceiveRequest(key,
                                                req.getRequestId(),
                                                req.getSelector(),
                                                0,
                                                false));
    }
    else
     proxyAgent.sendNot(proxyAgent.getAgentId(),
                        new RequestNotification(key, req));
  }

  /**
   * Forwards the client's <code>QBrowseRequest</code> request as
   * a <code>BrowseRequest</code> MOM request directly to a destination.
   */
  private void reactToClientRequest(int key, QBrowseRequest req)
  {
    proxyAgent.sendNot(AgentId.fromString(req.getTarget()),
                       new BrowseRequest(key,
                                         req.getRequestId(),
                                         req.getSelector()));
  }


  /**
   * Distributes the received notifications to the appropriate reactions.
   * <p>
   * A JMS proxy reacts to:
   * <ul>
   * <li><code>RequestNotification</code> notifications,</li>
   * <li><code>SyncReply</code> proxy synchronizing notification,</li>
   * <li><code>SetDMQRequest</code> admin notification,</li>
   * <li><code>SetThreshRequest</code> admin notification,</li>
   * <li><code>Monit_GetDMQSettings</code> monitoring notification,</li>
   * <li><code>AbstractReply</code> destination replies,</li>
   * <li><code>AdminReply</code> administration replies,</li>
   * <li><code>fr.dyade.aaa.agent.UnknownAgent</code>.</li>
   * </ul>
   * @exception UnknownNotificationException  If the notification is not
   *              expected.
   */ 
  public void react(AgentId from, Notification not)
              throws UnknownNotificationException
  {
    if (not instanceof RequestNotification) {
      int key = ((RequestNotification) not).id;
      AbstractJmsRequest request = ((RequestNotification) not).request;

      doReact(key, request);
    }
    // Administration and monitoring requests:
    else if (not instanceof SetDMQRequest)
      doReact(from, (SetDMQRequest) not);
    else if (not instanceof SetThreshRequest)
      doReact(from, (SetThreshRequest) not);
    else if (not instanceof Monit_GetDMQSettings)
      doReact(from, (Monit_GetDMQSettings) not);
    // Synchronization notification:
    else if (not instanceof SyncReply)
      doReact((SyncReply) not);
    // Notifications sent by a destination:
    else if (not instanceof AbstractReply) 
      doFwd(from, (AbstractReply) not);
    else if (not instanceof AdminReply)
      doReact((AdminReply) not);
    // Platform notifications:
    else if (not instanceof UnknownAgent)
      doReact((UnknownAgent) not);
    else
      throw new UnknownNotificationException("Unexpected notification: " 
                                             + not.getClass().getName());

    // Commiting the messages persistence requests.
    msgsPersistenceModule.commit();
  }

  
  /**
   * Distributes the client requests to the appropriate reactions.
   * <p>
   * The proxy accepts the following requests:
   * <ul>
   * <li><code>GetAdminTopicRequest</code></li>
   * <li><code>CnxConnectRequest</code></li>
   * <li><code>CnxStartRequest</code></li>
   * <li><code>CnxStopRequest</code></li>
   * <li><code>SessCreateTQRequest</code></li>
   * <li><code>SessCreateTTRequest</code></li>
   * <li><code>ConsumerSubRequest</code></li>
   * <li><code>ConsumerUnsubRequest</code></li>
   * <li><code>ConsumerCloseSubRequest</code></li>
   * <li><code>ConsumerSetListRequest</code></li>
   * <li><code>ConsumerUnsetListRequest</code></li>
   * <li><code>ConsumerReceiveRequest</code></li>
   * <li><code>ConsumerAckRequest</code></li>
   * <li><code>ConsumerDenyRequest</code></li>
   * <li><code>SessAckRequest</code></li>
   * <li><code>SessDenyRequest</code></li>
   * <li><code>TempDestDeleteRequest</code></li>
   * <li><code>XASessPrepare</code></li>
   * <li><code>XASessCommit</code></li>
   * <li><code>XASessRollback</code></li>
   * </ul>
   * <p>
   * A <code>JmsExceptReply</code> is sent back to the client when an
   * exception is thrown by the reaction.
   */ 
  private void doReact(int key, AbstractJmsRequest request)
  {
    try {
      // Updating the active context if the request is not a new context
      // request!
      if (! (request instanceof CnxConnectRequest))
        setCtx(key);

      if (request instanceof GetAdminTopicRequest)
        doReact(key, (GetAdminTopicRequest) request);
      else if (request instanceof CnxConnectRequest)
        doReact(key, (CnxConnectRequest) request);
      else if (request instanceof CnxStartRequest)
        doReact((CnxStartRequest) request);
      else if (request instanceof CnxStopRequest)
        doReact((CnxStopRequest) request);
      else if (request instanceof SessCreateTQRequest)
        doReact((SessCreateTQRequest) request);
      else if (request instanceof SessCreateTTRequest)
        doReact((SessCreateTTRequest) request);
      else if (request instanceof ConsumerSubRequest)
        doReact((ConsumerSubRequest) request);
      else if (request instanceof ConsumerUnsubRequest)
        doReact((ConsumerUnsubRequest) request);
      else if (request instanceof ConsumerCloseSubRequest)
        doReact((ConsumerCloseSubRequest) request);
      else if (request instanceof ConsumerSetListRequest)
        doReact((ConsumerSetListRequest) request);
      else if (request instanceof ConsumerUnsetListRequest)
        doReact((ConsumerUnsetListRequest) request);
      else if (request instanceof ConsumerReceiveRequest)
        doReact((ConsumerReceiveRequest) request);
      else if (request instanceof ConsumerAckRequest)
        doReact((ConsumerAckRequest) request);
      else if (request instanceof ConsumerDenyRequest)
        doReact((ConsumerDenyRequest) request);
      else if (request instanceof SessAckRequest)
        doReact((SessAckRequest) request);
      else if (request instanceof SessDenyRequest)
        doReact((SessDenyRequest) request);
      else if (request instanceof TempDestDeleteRequest)
        doReact((TempDestDeleteRequest) request);
      else if (request instanceof XASessPrepare)
        doReact((XASessPrepare) request);
      else if (request instanceof XASessCommit)
        doReact((XASessCommit) request);
      else if (request instanceof XASessRollback)
        doReact((XASessRollback) request);
    }
    catch (MomException mE) {
      if (MomTracing.dbgProxy.isLoggable(BasicLevel.WARN))
        MomTracing.dbgProxy.log(BasicLevel.WARN, mE);

      // If the context has not been lost, sending the exception to
      // the client:
      if (! (mE instanceof ProxyException))
        doReply(new MomExceptionReply(request.getRequestId(), mE));
    }
  }

  /**
   * Method implementing the reaction to a <code>GetAdminTopicRequest</code>
   * requesting the identifier of the local admin topic.
   * <p>
   * It simply sends back a <code>GetAdminTopicReply</code> holding the 
   * admin topic identifier.
   * 
   * @exception AccessException  If the requester is not an administrator.
   */
  private void doReact(int key, GetAdminTopicRequest req)
               throws AccessException
  {
    if (! admin)
      throw new AccessException("Request forbidden to a non administrator.");

    doReply(key,
            new GetAdminTopicReply(req,
                                   AdminTopicImpl.ref.getId().toString()));
  }

  /**
   * Method implementing the reaction to a <code>CnxConnectRequest</code>
   * requesting the key of the active context.
   * <p>
   * It simply sends back a <code>ConnectReply</code> holding the active
   * context's key.
   *
   * @exception DestinationException  In case of a first administrator 
   *              context, if the local administration topic reference
   *              is not available.
   */
  private void doReact(int key, CnxConnectRequest req)
               throws DestinationException
  {
    activeCtxId = key;
    activeCtx = new ClientContext(proxyAgent.getAgentId(), key);
    contexts.put(new Integer(key), activeCtx);
    
    if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
      MomTracing.dbgProxy.log(BasicLevel.DEBUG, "Connection " + key
                              + " opened.");

    // If this is a first administrator context, registering the proxy
    // to the administration topic if it is available.
    if (admin && firstAdminConnection) {
      if (AdminTopicImpl.ref == null) {
        throw new DestinationException("Can't access the local AdminTopic"
                                       + " on server "
                                       + AgentServer.getServerId());
      }
      AdminNotification adminNot =
        new AdminNotification(proxyAgent.getAgentId(),
                              initialAdminName,
                              initialAdminPass);
      proxyAgent.sendNot(AdminTopicImpl.ref.getId(), adminNot);
      firstAdminConnection = false;
    }
    doReply(new CnxConnectReply(req, key, proxyAgent.getAgentId().toString()));
  }

  /**
   * Method implementing the proxy reaction to a <code>CnxStartRequest</code>
   * requesting to start a context.
   * <p>
   * This method sends the pending <code>ConsumerMessages</code> replies,
   * if any.
   */
  private void doReact(CnxStartRequest req)
  {
    activeCtx.setActivated(true);

    // Delivering the pending deliveries, if any:
    for (Enumeration deliveries = activeCtx.getPendingDeliveries();
         deliveries.hasMoreElements();)
      doReply((ConsumerMessages) deliveries.nextElement());

    // Clearing the pending deliveries.
    activeCtx.clearPendingDeliveries();
  }

  /**
   * Method implementing the JMS proxy reaction to a
   * <code>CnxStopRequest</code> requesting to stop a context.
   * <p>
   * This method sends a <code>ServerReply</code> back.
   */
  private void doReact(CnxStopRequest req)
  {
    activeCtx.setActivated(false);
    doReply(new ServerReply(req));
  }

  /**
   * Method implementing the JMS proxy reaction to a
   * <code>SessCreateTQRequest</code> requesting the creation of a temporary
   * queue.
   * <p>
   * Creates the queue, sends it a <code>SetRightRequest</code> for granting
   * WRITE access to all, and wraps a <code>SessCreateTDReply</code> in a
   * <code>SyncReply</code> notification it sends to itself. This latest
   * action's purpose is to preserve causality.
   *
   * @exception RequestException  If the queue could not be deployed.
   */
  private void doReact(SessCreateTQRequest req) throws RequestException
  {
    try {
      Queue queue = new Queue(proxyAgent.getAgentId());
      AgentId qId = queue.getId();

      queue.deploy();

      // Setting free WRITE right on the queue:
      proxyAgent.sendNot(qId, new SetRightRequest(null, null, 2));

      activeCtx.addTemporaryDestination(qId);

      SessCreateTDReply reply = new SessCreateTDReply(req, qId.toString());
      proxyAgent.sendNot(proxyAgent.getAgentId(),
                         new SyncReply(activeCtxId, reply));

      if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
        MomTracing.dbgProxy.log(BasicLevel.DEBUG, "Temporary queue "
                                + qId + " created.");
    }
    catch (java.io.IOException iE) {
      throw new RequestException("Could not create temporary queue: " + iE);
    } 
  }

  /**
   * Method implementing the JMS proxy reaction to a
   * <code>SessCreateTTRequest</code> requesting the creation of a temporary
   * topic.
   * <p>
   * Creates the topic, sends it a <code>SetRightRequest</code> for granting
   * WRITE access to all, and wraps a <code>SessCreateTDReply</code> in a
   * <code>SyncReply</code> notification it sends to itself. This latest
   * action's purpose is to preserve causality.
   *
   * @exception RequestException  If the topic could not be deployed.
   */
  private void doReact(SessCreateTTRequest req) throws RequestException
  {
    Topic topic = new Topic(proxyAgent.getAgentId());
    AgentId tId = topic.getId();

    try {
      topic.deploy();

      // Setting free WRITE right on the topic:
      proxyAgent.sendNot(tId, new SetRightRequest(null, null, 2));

      activeCtx.addTemporaryDestination(tId);

      SessCreateTDReply reply = new SessCreateTDReply(req, tId.toString());
      proxyAgent.sendNot(proxyAgent.getAgentId(),
                         new SyncReply(activeCtxId, reply));

      if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
        MomTracing.dbgProxy.log(BasicLevel.DEBUG, "Temporary topic"
                                + tId + " created.");
    }
    catch (java.io.IOException iE) {
      topic = null;
      throw new RequestException("Could not deploy temporary topic "
                                 + tId + ": " + iE);
    } 
  }

  /**
   * Method implementing the JMS proxy reaction to a
   * <code>ConsumerSubRequest</code> requesting to subscribe to a topic.
   *
   * @exception RequestException  If activating an already active durable
   *              subscription.
   */
  private void doReact(ConsumerSubRequest req) throws RequestException
  {
    AgentId topicId = AgentId.fromString(req.getTarget());
    String subName = req.getSubName();
    
    boolean newTopic = ! topicsTable.containsKey(topicId);
    boolean newSub = ! subsTable.containsKey(subName);

    TopicSubscription tSub;
    ClientSubscription cSub;

    // true if a SubscribeRequest has been sent to the topic. 
    boolean sent = false;

    // New topic...
    if (newTopic) {
      tSub = new TopicSubscription();
      topicsTable.put(topicId, tSub);
    }
    // Known topic...
    else
      tSub = (TopicSubscription) topicsTable.get(topicId);

    // New subscription...
    if (newSub) {
      cSub = new ClientSubscription(proxyAgent.getAgentId(),
                                    activeCtxId,
                                    req.getRequestId(),
                                    req.getDurable(),
                                    topicId,
                                    req.getSubName(),
                                    req.getSelector(),
                                    req.getNoLocal(),
                                    dmqId,
                                    threshold,
                                    msgsPersistenceModule,
                                    messagesTable);
     
      if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
        MomTracing.dbgProxy.log(BasicLevel.DEBUG,
                                "Subscription " + subName + " created.");

      subsTable.put(subName, cSub);
      tSub.putSubscription(subName, req.getSelector());
      sent =
        updateSubscriptionToTopic(topicId, activeCtxId, req.getRequestId());
    }
    // Existing durable subscription...
    else {
      cSub = (ClientSubscription) subsTable.get(subName);

      if (cSub.getActive())
        throw new RequestException("The durable subscription "
                                   + subName 
                                   + " has already been activated.");

      // Updated topic: updating the subscription to the previous topic.
      boolean updatedTopic = ! topicId.equals(cSub.getTopicId());
      if (updatedTopic) {
        TopicSubscription oldTSub =
          (TopicSubscription) topicsTable.get(cSub.getTopicId());
        oldTSub.removeSubscription(subName);
        updateSubscriptionToTopic(cSub.getTopicId(), -1, -1);
      }

      // Updated selector?
      boolean updatedSelector;
      if (req.getSelector() == null && cSub.getSelector() != null)
        updatedSelector = true;
      else if (req.getSelector() != null && cSub.getSelector() == null)
        updatedSelector = true;
      else if (req.getSelector() == null && cSub.getSelector() == null)
        updatedSelector = false;
      else
        updatedSelector = ! req.getSelector().equals(cSub.getSelector());

      // Reactivating the subscription.
      cSub.reactivate(activeCtxId,
                      req.getRequestId(),
                      topicId,
                      req.getSelector(),
                      req.getNoLocal());

      if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
        MomTracing.dbgProxy.log(BasicLevel.DEBUG,
                                "Subscription " + subName + " reactivated.");

      // Updated subscription: updating subscription to topic.  
      if (updatedTopic || updatedSelector) {
        tSub.putSubscription(subName, req.getSelector());
        sent = updateSubscriptionToTopic(topicId,
                                         activeCtxId,
                                         req.getRequestId());
      }
    }
    // Activating the subscription.
    activeCtx.addSubName(subName);

    // Acknowledging the request, if needed.
    if (! sent)
      proxyAgent.sendNot(proxyAgent.getAgentId(),
                         new SyncReply(activeCtxId, new ServerReply(req)));
  }

  /**
   * Method implementing the JMS proxy reaction to a
   * <code>ConsumerSetListRequest</code> notifying the creation of a client
   * listener.
   * <p>
   * Sets the listener for the subscription, launches a delivery sequence.
   *
   * @exception RequestException  If the subscription does not exist.
   */
  private void doReact(ConsumerSetListRequest req) throws RequestException
  {
    // Getting the subscription:
    String subName = req.getTarget();
    ClientSubscription sub = (ClientSubscription) subsTable.get(subName);

    if (sub == null)
      throw new RequestException("Can't set a listener on the non existing"
                                 + " subscription: " + subName);

    sub.setListener(req.getRequestId());

    ConsumerMessages consM = sub.deliver();
    if (consM != null) {
      if (activeCtx.getActivated())
        doReply(consM);
      else
        activeCtx.addPendingDelivery(consM);
    }
  }
   
  /**
   * Method implementing the JMS proxy reaction to a
   * <code>ConsumerUnsetListRequest</code> notifying that a consumer listener
   * is unset.
   *
   * @exception RequestException  If the subscription does not exist.
   */
  private void doReact(ConsumerUnsetListRequest req) throws RequestException
  {
    // If the listener was listening to a queue, cancelling any pending reply:
    if (req.getQueueMode())
      activeCtx.cancelReceive(req.getCancelledRequestId());
    // If the listener was listening to a topic, unsetting the listener.
    else {
      String subName = req.getTarget();
      ClientSubscription sub = (ClientSubscription) subsTable.get(subName);

      if (sub == null)
        throw new RequestException("Can't unset a listener on the non existing"
                                   + " subscription: " + subName);

      sub.unsetListener();
    }
    // Acknowledging the request:
    doReply(new ServerReply(req));
  }

  /**
   * Method implementing the JMS proxy reaction to a
   * <code>ConsumerCloseSubRequest</code> requesting to deactivate a durable
   * subscription.
   *
   * @exception RequestException  If the subscription does not exist. 
   */
  private void doReact(ConsumerCloseSubRequest req) throws RequestException
  {
    // Getting the subscription:
    String subName = req.getTarget();
    ClientSubscription sub = (ClientSubscription) subsTable.get(subName);

    if (sub == null)
      throw new RequestException("Can't desactivate non existing"
                                 + " subscription: " + subName);

    // De-activating the subscription:
    activeCtx.removeSubName(subName);
    sub.deactivate();

    // Acknowledging the request:
    doReply(new ServerReply(req));
  }

  /**
   * Method implementing the JMS proxy reaction to a
   * <code>ConsumerUnsubRequest</code> requesting to remove a subscription.
   *
   * @exception RequestException  If the subscription does not exist.
   */
  private void doReact(ConsumerUnsubRequest req) throws RequestException
  {
    // Getting the subscription.
    String subName = req.getTarget();
    ClientSubscription sub = (ClientSubscription) subsTable.get(subName);

    if (sub == null)
      throw new RequestException("Can't unsubscribe non existing"
                                 + " subscription: " + subName);

    if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
      MomTracing.dbgProxy.log(BasicLevel.DEBUG,
                              "Deleting subscription " + subName);

    // Updating the proxy's subscription to the topic.
    AgentId topicId = sub.getTopicId();
    TopicSubscription tSub = (TopicSubscription) topicsTable.get(topicId);
    tSub.removeSubscription(subName);
    updateSubscriptionToTopic(topicId, -1, -1);

    // Deleting the subscription.
    sub.delete();
    activeCtx.removeSubName(subName);
    subsTable.remove(subName);

    // Acknowledging the request:
    proxyAgent.sendNot(proxyAgent.getAgentId(),
                       new SyncReply(activeCtxId, new ServerReply(req)));
  }

  /**
   * Method implementing the proxy reaction to a
   * <code>ConsumerReceiveRequest</code> instance, requesting a message from a
   * subscription.
   * <p>
   * This method registers the request and launches a delivery sequence. 
   *
   * @exception RequestException  If the subscription does not exist. 
   */
  private void doReact(ConsumerReceiveRequest req) throws RequestException
  {
    String subName = req.getTarget();
    ClientSubscription sub = (ClientSubscription) subsTable.get(subName);

    if (sub == null)
      throw new RequestException("Can't request a message from the unknown"
                                 + " subscription: " + subName);

    // Getting a message from the subscription.
    sub.setReceiver(req.getRequestId(), req.getTimeToLive());
    ConsumerMessages consM = sub.deliver();

    // Nothing to deliver but immediate delivery request: building an empty
    // reply.
    if (consM == null && req.getTimeToLive() == -1) {
      sub.unsetReceiver();
      consM = new ConsumerMessages(req.getRequestId(), subName, false);
    }

    // Delivering.
    if (consM != null && activeCtx.getActivated())
      doReply(consM);
    else if (consM != null)
      activeCtx.addPendingDelivery(consM);
  }

  /** 
   * Method implementing the JMS proxy reaction to a
   * <code>SessAckRequest</code> acknowledging messages either on a queue
   * or on a subscription.
   */
  private void doReact(SessAckRequest req) 
  {
    if (req.getQueueMode()) {
      AgentId qId = AgentId.fromString(req.getTarget());
      Vector ids = req.getIds();
      proxyAgent.sendNot(qId,
                         new AcknowledgeRequest(activeCtxId,
                                                req.getRequestId(),
                                                ids));
    }
    else {
      String subName = req.getTarget();
      ClientSubscription sub = (ClientSubscription) subsTable.get(subName);
      if (sub != null)
        sub.acknowledge(req.getIds().elements());
    }
  }

  /** 
   * Method implementing the JMS proxy reaction to a
   * <code>SessDenyRequest</code> denying messages either on a queue or on
   * a subscription.
   */
  private void doReact(SessDenyRequest req)
  {
    if (req.getQueueMode()) {
      AgentId qId = AgentId.fromString(req.getTarget());
      Vector ids = req.getIds();
      proxyAgent.sendNot(qId,
                         new DenyRequest(activeCtxId, req.getRequestId(), ids));

      // Acknowledging the request unless forbidden:
      if (! req.getDoNotAck())
        proxyAgent.sendNot(proxyAgent.getAgentId(),
                           new SyncReply(activeCtxId, new ServerReply(req)));
    }
    else {
      String subName = req.getTarget();
      ClientSubscription sub = (ClientSubscription) subsTable.get(subName);

      if (sub == null) 
        return;

      sub.deny(req.getIds().elements());

      // Launching a delivery sequence:
      ConsumerMessages consM = sub.deliver();
      // Delivering.
      if (consM != null && activeCtx.getActivated())
        doReply(consM);
      else if (consM != null)
        activeCtx.addPendingDelivery(consM);
    }
  }

  /** 
   * Method implementing the JMS proxy reaction to a
   * <code>ConsumerAckRequest</code> acknowledging a message either on a queue
   * or on a subscription.
   */
  private void doReact(ConsumerAckRequest req)
  {
    if (req.getQueueMode()) {
      AgentId qId = AgentId.fromString(req.getTarget());
      String id = req.getId();
      proxyAgent.sendNot(qId,
                         new AcknowledgeRequest(activeCtxId,
                                                req.getRequestId(),
                                                id));
    }
    else {
      String subName = req.getTarget();
      ClientSubscription sub = (ClientSubscription) subsTable.get(subName);
      if (sub != null) {
        Vector ids = new Vector();
        ids.add(req.getId());
        sub.acknowledge(ids.elements());
      }
    }
  }

  /** 
   * Method implementing the JMS proxy reaction to a
   * <code>ConsumerDenyRequest</code> denying a message either on a queue
   * or on a subscription.
   * <p>
   * This request is acknowledged when destinated to a queue.
   */
  private void doReact(ConsumerDenyRequest req)
  {
    if (req.getQueueMode()) {
      AgentId qId = AgentId.fromString(req.getTarget());
      String id = req.getId();
      proxyAgent.sendNot(qId,
                         new DenyRequest(activeCtxId, req.getRequestId(), id));

      // Acknowledging the request, unless forbidden:
      if (! req.getDoNotAck())
        proxyAgent.sendNot(proxyAgent.getAgentId(),
                           new SyncReply(activeCtxId, new ServerReply(req)));
    }
    else {
      String subName = req.getTarget();
      ClientSubscription sub = (ClientSubscription) subsTable.get(subName);

      if (sub == null) 
        return;

      Vector ids = new Vector();
      ids.add(req.getId());
      sub.deny(ids.elements());

      // Launching a delivery sequence:
      ConsumerMessages consM = sub.deliver();
      // Delivering.
      if (consM != null && activeCtx.getActivated())
        doReply(consM);
      else if (consM != null)
        activeCtx.addPendingDelivery(consM);
    }
  }

  /**
   * Method implementing the JMS proxy reaction to a 
   * <code>TempDestDeleteRequest</code> request for deleting a temporary
   * destination.
   * <p>
   * This method sends a <code>fr.dyade.aaa.agent.DeleteNot</code> to the
   * destination and acknowledges the request.
   */
  private void doReact(TempDestDeleteRequest req)
  {
    // Removing the destination from the context's list:
    AgentId tempId = AgentId.fromString(req.getTarget());
    activeCtx.removeTemporaryDestination(tempId);

    // Sending the request to the destination:
    proxyAgent.sendNot(tempId, new DeleteNot());

    // Acknowledging the request:
    proxyAgent.sendNot(proxyAgent.getAgentId(),
                       new SyncReply(activeCtxId, new ServerReply(req)));
  }

  /**
   * Method implementing the JMS proxy reaction to an
   * <code>XASessPrepare</code> request holding messages and acknowledgements
   * produced in an XA transaction.
   */
  private void doReact(XASessPrepare req)
  {
    activeCtx.registerTxPrepare(req);
    doReply(new ServerReply(req));
  }

  /**
   * Method implementing the JMS proxy reaction to an
   * <code>XASessCommit</code> request commiting the operations performed
   * in a given transaction.
   * <p>
   * This method actually processes the objects sent at the prepare phase,
   * and acknowledges the request.
   */
  private void doReact(XASessCommit req)
  {
    XASessPrepare prepare = activeCtx.getTxPrepare(req.getId());

    Vector sendings = prepare.getSendings();
    Vector acks = prepare.getAcks();

    ProducerMessages pM;
    ClientMessages not;
    while (! sendings.isEmpty()) {
      pM = (ProducerMessages) sendings.remove(0);
      not = new ClientMessages(activeCtxId,
                               pM.getRequestId(),
                               pM.getMessages());
      proxyAgent.sendNot(AgentId.fromString(pM.getTarget()), not);
    }

    while (! acks.isEmpty())
      doReact((SessAckRequest) acks.remove(0));

    doReply(new ServerReply(req));
  }

  /**
   * Method implementing the JMS proxy reaction to an
   * <code>XASessRollback</code> request rolling back the operations performed
   * in a given transaction.
   */
  private void doReact(XASessRollback req)
  {
    String id = req.getId();

    String queueName;
    AgentId qId;
    Vector ids;
    for (Enumeration queues = req.getQueues(); queues.hasMoreElements();) {
      queueName = (String) queues.nextElement();
      qId = AgentId.fromString(queueName);
      ids = req.getQueueIds(queueName);
      proxyAgent.sendNot(qId,
                         new DenyRequest(activeCtxId, req.getRequestId(), ids));
    }

    String subName;
    ClientSubscription sub;
    ConsumerMessages consM;
    for (Enumeration subs = req.getSubs(); subs.hasMoreElements();) {
      subName = (String) subs.nextElement();
      sub = (ClientSubscription) subsTable.get(subName);
      if (sub != null) {
        sub.deny(req.getSubIds(subName).elements());

        consM = sub.deliver();
        if (consM != null && activeCtx.getActivated())
          doReply(consM);
        else if (consM != null)
          activeCtx.addPendingDelivery(consM);
      }
    }

    XASessPrepare prepare = (XASessPrepare) activeCtx.getTxPrepare(id);
    if (prepare != null) {
      Vector acks = prepare.getAcks();

      SessAckRequest ack;
      while (! acks.isEmpty()) {
        ack = (SessAckRequest) acks.remove(0);
        doReact(new SessDenyRequest(ack.getTarget(),
                                    ack.getIds(),
                                    ack.getQueueMode(),
                                    true));
      }
    }

    proxyAgent.sendNot(proxyAgent.getAgentId(),
                       new SyncReply(activeCtxId, new ServerReply(req)));
  }

  
  /**
   * Method implementing the reaction to a <code>SetDMQRequest</code>
   * instance setting the dead message queue identifier for this proxy
   * and its subscriptions.
   */
  private void doReact(AgentId from, SetDMQRequest not)
  {
    dmqId = not.getDmqId();

    for (Enumeration keys = subsTable.keys(); keys.hasMoreElements();) 
      ((ClientSubscription) subsTable.get(keys.nextElement())).setDMQId(dmqId);

    proxyAgent.sendNot(from, new AdminReply(not, true, "DMQ set: " + dmqId));
  }

  /**
   * Method implementing the reaction to a <code>SetThreshRequest</code>
   * instance setting the threshold value for this proxy and its
   * subscriptions.
   */
  private void doReact(AgentId from, SetThreshRequest not)
  {
    threshold = not.getThreshold();

    for (Enumeration keys = subsTable.keys(); keys.hasMoreElements();) 
      ((ClientSubscription)
         subsTable.get(keys.nextElement())).setThreshold(not.getThreshold());

    proxyAgent.sendNot(from,
                       new AdminReply(not,
                                      true,
                                      "Threshold set: " + threshold));
  }

  /**
   * Method implementing the reaction to a <code>Monit_GetDMQSettings</code>
   * instance requesting the DMQ settings of this proxy.
   */
  private void doReact(AgentId from, Monit_GetDMQSettings not)
  {
    String id = null;
    if (dmqId != null)
      id = dmqId.toString();
    proxyAgent.sendNot(from, new Monit_GetDMQSettingsRep(not, id, threshold));
  }

  /**
   * Method implementing the JMS proxy reaction to a
   * <code>SyncReply</code> notification sent by itself, wrapping a reply
   * to be sent to a client.
   */
  private void doReact(SyncReply not)
  {
    doReply(not.key, not.reply);
  }

  /**
   * Distributes the JMS replies to the appropriate reactions.
   * <p>
   * JMS proxies react the following replies:
   * <ul>
   * <li><code>QueueMsgReply</code></li>
   * <li><code>BrowseReply</code></li>
   * <li><code>SubscribeReply</code></li>
   * <li><code>TopicMsgsReply</code></li>
   * <li><code>ExceptionReply</code></li>
   * </ul>
   */
  private void doFwd(AgentId from, AbstractReply rep)
  {
    if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
      MomTracing.dbgProxy.log(BasicLevel.DEBUG, "--- " + this
                              + " got " + rep.getClass().getName()
                              + " with id: " + rep.getCorrelationId()
                              + " from: " + from);

    if (rep instanceof QueueMsgReply)
      doFwd(from, (QueueMsgReply) rep);
    else if (rep instanceof BrowseReply)
      doFwd((BrowseReply) rep);
    else if (rep instanceof SubscribeReply)
      doFwd((SubscribeReply) rep);
    else if (rep instanceof TopicMsgsReply)
      doFwd(from, (TopicMsgsReply) rep);
    else if (rep instanceof ExceptionReply)
      doReact(from, (ExceptionReply) rep);
    else {
      if (MomTracing.dbgProxy.isLoggable(BasicLevel.WARN))
        MomTracing.dbgProxy.log(BasicLevel.WARN, "Unexpected reply!");
    }
  }


  /**
   * Actually forwards a <code>QueueMsgReply</code> coming from a destination
   * as a <code>ConsumerMessages</code> destinated to the requesting client.
   * <p>
   * If the corresponding context is stopped, stores the
   * <code>ConsumerMessages</code> for later delivery.
   */
  private void doFwd(AgentId from, QueueMsgReply rep)
  {
    try {
      // Updating the active context:
      setCtx(rep.getClientContext());

      // If the receive request being replied has been cancelled, denying
      // the message.
      if (rep.getCorrelationId() == activeCtx.getCancelledReceive()) {
        if (rep.getMessage() != null) {
          String msgId = rep.getMessage().getIdentifier();

          if (MomTracing.dbgProxy.isLoggable(BasicLevel.WARN))
            MomTracing.dbgProxy.log(BasicLevel.WARN, "Denying message: "
                                    + msgId);

          proxyAgent.sendNot(from,
                             new DenyRequest(0,
                                             rep.getCorrelationId(),
                                             msgId));
        }
        return;
      }

      // Building the reply and storing the wrapped message id for later
      // denying in the case of a failure:
      Message msg = rep.getMessage();
      ConsumerMessages jRep = new ConsumerMessages(rep.getCorrelationId(),
                                                   msg,
                                                   from.toString(),
                                                   true);

      if (msg != null)
        activeCtx.addDeliveringQueue(from);

      // If the context is started, delivering the message, or buffering
      // it:
      if (activeCtx.getActivated())
        doReply(jRep);
      else
        activeCtx.addPendingDelivery(jRep);
    }
    // The context is lost: denying the message:
    catch (ProxyException pE) {
      if (rep.getMessage() != null) {
        String msgId = rep.getMessage().getIdentifier();

        if (MomTracing.dbgProxy.isLoggable(BasicLevel.WARN))
          MomTracing.dbgProxy.log(BasicLevel.WARN, "Denying message: "
                                  + msgId);

        proxyAgent.sendNot(from,
                           new DenyRequest(0,rep.getCorrelationId(), msgId));
      }
    }
  }


  /**
   * Actually forwards a <code>BrowseReply</code> coming from a
   * destination as a <code>QBrowseReply</code> destinated to the
   * requesting client.
   */
  private void doFwd(BrowseReply rep)
  {
    try {
      // Updating the active context:
      setCtx(rep.getClientContext());
      doReply(new QBrowseReply(rep));
    }
    // The context is lost; nothing to do.
    catch (ProxyException pE) {}
  }

  /**
   * Forwards the topic's <code>SubscribeReply</code> as a
   * <code>ServerReply</code>.
   */
  private void doFwd(SubscribeReply rep)
  {
    try {
      setCtx(rep.getClientContext());
      doReply(new ServerReply(rep.getCorrelationId()));
    }
    // The context is lost; nothing to do.
    catch (ProxyException pE) {}
  }

  /**
   * Method implementing the proxy reaction to a <code>TopicMsgsReply</code>
   * holding messages published by a topic.
   */
  private void doFwd(AgentId from, TopicMsgsReply rep)
  {
    // Browsing the target subscriptions:
    TopicSubscription tSub = (TopicSubscription) topicsTable.get(from);
    if (tSub == null || tSub.isEmpty())
      return;

    // Setting the arrival order of the messages.
    for (Enumeration msgs = rep.getMessages().elements();
         msgs.hasMoreElements();) {
      
      if (arrivalsCounter == Long.MAX_VALUE)
        arrivalsCounter = 0;

      ((Message) msgs.nextElement()).order = arrivalsCounter++;
    }

    String subName;
    ClientSubscription sub;
    for (Enumeration names = tSub.getNames(); names.hasMoreElements();) {
      subName = (String) names.nextElement();
      sub = (ClientSubscription) subsTable.get(subName);
      if (sub == null)
        return;

      // Browsing the delivered messages.
      sub.browseNewMessages(rep.getMessages());

      // If the subscription is active, lauching a delivery sequence.
      if (sub.getActive()) {
        ConsumerMessages consM = sub.deliver();
        if (consM != null) {
          try {
            setCtx(sub.getContextId());
            if (activeCtx.getActivated())
              doReply(consM);
            else
              activeCtx.addPendingDelivery(consM);
          }
          // The context is lost: nothing to do.
          catch (ProxyException pE) {}
        }
      }
    }
  }

  /**
   * Actually forwards an <code>ExceptionReply</code> coming from a destination
   * as a <code>MomExceptionReply</code> destinated to the requesting client.
   * <p>
   * If the wrapped exception is an <code>AccessException</code> thrown by
   * a <code>Topic</code> as a reply to a <code>SubscribeRequest</code>,
   * removing the corresponding subscriptions.
   */
  private void doReact(AgentId from, ExceptionReply rep)
  {
    MomException exc = rep.getException();

    // The exception comes from a topic refusing the access: deleting the subs.
    if (exc instanceof AccessException) {
      TopicSubscription tSub = (TopicSubscription) topicsTable.remove(from);
      if (tSub != null) {
        String name;
        ClientSubscription sub;
        for (Enumeration enum = tSub.getNames(); enum.hasMoreElements();) { 
          name = (String) enum.nextElement();
          sub = (ClientSubscription) subsTable.remove(name); 
          sub.delete();

          try {
            setCtx(sub.getContextId());
            activeCtx.removeSubName(name);
            doReply(new MomExceptionReply(rep.getCorrelationId(), exc));
          }
          catch (ProxyException pExc) {}
        }
        return;
      }
    }
    // Forwarding the exception to the client.
    try {
      setCtx(rep.getClientContext());
      doReply(new MomExceptionReply(rep.getCorrelationId(), exc));
    }
    catch (ProxyException pExc) {}
  }

  /** 
   * An <code>AdminReply</code> acknowledges the setting of a temporary
   * destination; nothing needs to be done.
   */
  private void doReact(AdminReply reply)
  {}

  /**
   * Method implementing the JMS proxy reaction to an <code>UnknownAgent</code>
   * notification notifying that a destination does not exist or is deleted.
   * <p>
   * If it notifies of a deleted topic, the method removes the 
   * corresponding subscriptions. If the wrapped request is messages sending,
   * the messages are sent to the DMQ.
   * <p>
   * A <code>JmsExceptReply</code> is sent to the concerned requester.
   * <p>
   * This case might also happen when sending a <code>ClientMessages</code>
   * to a dead message queue. In that case, the invalid DMQ identifier is set
   * to null.
   */
  private void doReact(UnknownAgent uA)
  {
    Notification not = uA.not;
    AgentId agId = uA.agent;

    if (MomTracing.dbgProxy.isLoggable(BasicLevel.WARN))
      MomTracing.dbgProxy.log(BasicLevel.WARN, "--- " + this
                              + " notified of invalid destination: "
                              + agId.toString());

    // The deleted destination is a topic: deleting its subscriptions.
    TopicSubscription tSub = (TopicSubscription) topicsTable.remove(agId);
    if (tSub != null) {
      String name;
      ClientSubscription sub;
      DestinationException exc = new DestinationException("Destination "
                                                          + agId + " does"
                                                          + " not"
                                                          + " exist.");
      for (Enumeration enum = tSub.getNames(); enum.hasMoreElements();) { 
        name = (String) enum.nextElement();
        sub = (ClientSubscription) subsTable.remove(name); 
        sub.delete();

        try {
          setCtx(sub.getContextId());
          activeCtx.removeSubName(name);
          doReply(new MomExceptionReply(sub.getSubRequestId(), exc));
        }
        catch (ProxyException pExc) {}
      }
      return;
    }

    if (not instanceof AbstractRequest) {
      AbstractRequest req = (AbstractRequest) not;

      // If the wrapped request is messages sending,forwarding them to the DMQ:
      if (req instanceof ClientMessages) {
        // If the queue actually was a dead message queue, updating its
        // identifier:
        if (dmqId != null && agId.equals(dmqId)) {
          dmqId = null;
          for (Enumeration keys = subsTable.keys(); keys.hasMoreElements();)
            ((ClientSubscription)
               subsTable.get(keys.nextElement())).setDMQId(null);
        }
        // Sending the messages again if not coming from the default DMQ:
        if (DeadMQueueImpl.getId() != null
            && ! agId.equals(DeadMQueueImpl.getId()))
          sendToDMQ((ClientMessages) req);
      }
      if (MomTracing.dbgProxy.isLoggable(BasicLevel.WARN))
        MomTracing.dbgProxy.log(BasicLevel.WARN, "Connection "
                                + req.getClientContext() + " notified of"
                                + " the deletion of destination " + agId);
    }
  }

  /**
   * Updates the reference to the active context.
   *
   * @param key  Key of the activated context.
   *
   * @exception ProxyException  If the context has actually been closed or
   *              lost.
   */
  private void setCtx(int key) throws ProxyException
  {
    // If the required context is the last used, no need to update the
    // references:
    if (key == activeCtxId)
      return;

    // Else, updating the activeCtx reference:
    activeCtxId = key;
    activeCtx = (ClientContext) contexts.get(new Integer(key));

    // If context not found, throwing an exception:
    if (activeCtx == null) {
      activeCtxId = 0;
      activeCtx = null;
      throw new ProxyException("Context " + key + " is closed or broken.");
    }
  }

 
  /**
   * Method used for sending an <code>AbstractJmsReply</code> back to an
   * external client within the active context.
   *
   * @param rep  The reply to send.
   */
  private void doReply(AbstractJmsReply reply)
  {
    doReply(activeCtxId, reply);
  }

  /**
   * Method used for sending an <code>AbstractJmsReply</code> back to an
   * external client through a given context.
   *
   * @param key  The context through witch replying.
   * @param rep  The reply to send.
   */
  private void doReply(int key, AbstractJmsReply reply)
  {
    proxyAgent.sendToClient(key, reply);
  }

  /**
   * Method used for sending messages to the appropriate dead message queue.
   */
  private void sendToDMQ(ClientMessages messages)
  {
    if (dmqId != null)
      proxyAgent.sendNot(dmqId, messages);
    else if (DeadMQueueImpl.getId() != null)
      proxyAgent.sendNot(DeadMQueueImpl.getId(), messages);
  }

  
  /**
   * The method closes a given context by denying the non acknowledged
   * messages delivered to this context, and deleting its temporary
   * subscriptions and destinations.
   */
  public void closeConnection(int cKey)
  {
    try {
      setCtx(cKey);

      // Denying the non acknowledged messages:
      AgentId id;
      for (Enumeration ids = activeCtx.getDeliveringQueues();
           ids.hasMoreElements();) {
        id = (AgentId) ids.nextElement();
        proxyAgent.sendNot(id, new DenyRequest(cKey));
      }

      // Removing or deactivating the subscriptions:
      String subName = null;
      ClientSubscription sub;
      Vector topics = new Vector();
      for (Enumeration subs = activeCtx.getActiveSubs();
           subs.hasMoreElements();) {
        subName = (String) subs.nextElement();
        sub = (ClientSubscription) subsTable.get(subName);
  
        if (sub.getDurable()) {
          sub.deactivate();
  
          if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
            MomTracing.dbgProxy.log(BasicLevel.DEBUG, "Durable subscription"
                                    + subName + " de-activated.");
        }
        else {
          sub.delete();
          subsTable.remove(subName);
          TopicSubscription tSub =
            (TopicSubscription) topicsTable.get(sub.getTopicId());
          tSub.removeSubscription(subName);

          if (! topics.contains(sub.getTopicId()))
            topics.add(sub.getTopicId());

          if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
            MomTracing.dbgProxy.log(BasicLevel.DEBUG, "Temporary subscription"
                                    + subName + " deleted.");
        }
      }
      // Browsing the topics which at least have one subscription removed.
      for (Enumeration topicIds = topics.elements();
           topicIds.hasMoreElements();)
         updateSubscriptionToTopic((AgentId) topicIds.nextElement(), -1, -1);

      // Deleting the temporary destinations:
      AgentId destId;
      for (Enumeration dests = activeCtx.getTempDestinations();
           dests.hasMoreElements();) {
        destId = (AgentId) dests.nextElement();
        activeCtx.removeTemporaryDestination(destId);
        proxyAgent.sendNot(destId, new DeleteNot());
    
        if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
          MomTracing.dbgProxy.log(BasicLevel.DEBUG, "Deletes temporary"
                                  + " destination " + destId.toString());
      }
     
      // Finally, deleting the context: 
      contexts.remove(new Integer(cKey));
      activeCtx = null;
      activeCtxId = 0;
    }
    catch (ProxyException pE) {}
  }

  /**
   * This method deletes the proxy by notifying its connected clients,
   * denying the non acknowledged messages, deleting the temporary
   * destinations, removing the subscriptions.
   *
   * @exception Exception  If the requester is not an administrator.
   */
  public void deleteProxy(AgentId from) throws Exception
  {
    if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
      MomTracing.dbgProxy.log(BasicLevel.DEBUG, "--- " + this
                              + " notified to be deleted.");

    if (! from.equals(AdminTopicImpl.ref.getId()))
      throw new Exception();

    // Notifying the connected clients:
    Enumeration keys = contexts.keys();
    int key;
    while (keys.hasMoreElements()) {
      key = ((Integer) keys.nextElement()).intValue();
      try {
        setCtx(key);

        doReply(new MomExceptionReply(new ProxyException("Client proxy is "
                                                         + "deleted.")));

        // Denying the non acknowledged messages:
        AgentId id;
        for (Enumeration ids = activeCtx.getDeliveringQueues();
             ids.hasMoreElements();) {
          id = (AgentId) ids.nextElement();
          proxyAgent.sendNot(id, new DenyRequest(activeCtxId));
        }

        // Deleting the temporary destinations:
        AgentId destId;
        for (Enumeration dests = activeCtx.getTempDestinations();
             dests.hasMoreElements();) {
          destId = (AgentId) dests.nextElement();
          proxyAgent.sendNot(destId, new DeleteNot());
  
          if (MomTracing.dbgProxy.isLoggable(BasicLevel.DEBUG))
            MomTracing.dbgProxy.log(BasicLevel.DEBUG, "Sending DeleteNot to"
                                    + " temporary destination "
                                    + destId.toString());
        }
      }
      catch (ProxyException pE) {}
    }

    // Removing all proxy's subscriptions:
    AgentId destId;
    for (Enumeration topics = topicsTable.keys(); topics.hasMoreElements();) {
      destId = (AgentId) topics.nextElement();
      topicsTable.remove(destId);
      updateSubscriptionToTopic(destId, -1, -1);
    }
  }

  
  /**
   * Updates the proxy's subscription to a topic.
   *
   * @param topicId  Identifier of the topic to subscribe to.
   * @param contextId  Identifier of the subscription context.
   * @param requestId  Identifier of the subscription request.
   *
   * @return  <code>true</code> if a <code>SubscribeRequest</code> has been
   *          sent to the topic.
   */
  private boolean updateSubscriptionToTopic(AgentId topicId,
                                            int contextId,
                                            int requestId)
  {
    TopicSubscription tSub = (TopicSubscription) topicsTable.get(topicId);

    // No more subs to this topic: unsubscribing.
    if (tSub == null || tSub.isEmpty()) {
      topicsTable.remove(topicId);
      proxyAgent.sendNot(topicId,
                         new UnsubscribeRequest(contextId, requestId));
      return false;
    }

    // Otherwise, updating the subscription if the selector evolved.
    String builtSelector = tSub.buildSelector();
    if (tSub.getLastSelector() != null
        && builtSelector.equals(tSub.getLastSelector()))
      return false;

    tSub.setLastSelector(builtSelector);
    proxyAgent.sendNot(topicId, new SubscribeRequest(contextId,
                                                     requestId,
                                                     builtSelector));
   
    return true;
  }

  /** MBean interface implementation: returns the user name. */
  public String getUserName()
  {
    return AdminTopicImpl.ref.getName(proxyAgent.getAgentId());
  }

  /** MBean interface implementation: returns the user password. */
  public String getUserPassword()
  {
    return AdminTopicImpl.ref.getPassword(proxyAgent.getAgentId());
  }

  /** MBean interface implementation: deletes the proxy. */
  public void delete()
  {
    DeleteUser request = new DeleteUser(getUserName(),
                                        proxyAgent.getAgentId().toString());
    proxyAgent.sendNot(AdminTopicImpl.ref.getId(), new ProxyMBeanNot(request));
  }

  /**
   * Changes the user name.
   *
   * @param name  New name.
   *
   * @exception Exception  If the new name is already taken.
   */
  public void updateUserName(String name) throws Exception
  {
    if (AdminTopicImpl.ref.isTaken(name))
      throw new Exception("Name already taken.");

    UpdateUser request = new UpdateUser(getUserName(),
                                        proxyAgent.getAgentId().toString(),
                                        name,
                                        getUserPassword());
    proxyAgent.sendNot(AdminTopicImpl.ref.getId(), new ProxyMBeanNot(request));
  }

  /**
   * Changes the user password.
   *
   * @param pass  New password.
   */
  public void updateUserPassword(String pass)
  {
    UpdateUser request = new UpdateUser(getUserName(),
                                        proxyAgent.getAgentId().toString(),
                                        getUserName(),
                                        pass);
    proxyAgent.sendNot(AdminTopicImpl.ref.getId(), new ProxyMBeanNot(request));
  }
} 
