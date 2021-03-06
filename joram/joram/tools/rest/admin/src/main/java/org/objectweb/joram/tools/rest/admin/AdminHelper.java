/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2016 - 2018 ScalAgent Distributed Technologies
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
package org.objectweb.joram.tools.rest.admin;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.objectweb.joram.client.jms.Destination;
import org.objectweb.joram.client.jms.admin.AdminException;
import org.objectweb.joram.client.jms.admin.JoramAdmin;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.local.LocalConnectionFactory;
import org.objectweb.joram.client.jms.tcp.TcpConnectionFactory;
import org.objectweb.util.monolog.api.BasicLevel;
import org.objectweb.util.monolog.api.Logger;
import org.osgi.framework.BundleContext;

import fr.dyade.aaa.common.Debug;

public class AdminHelper {
  public static Logger logger = Debug.getLogger(AdminHelper.class.getName());

  public static final String BUNDLE_JNDI_FACTORY_INITIAL_PROP = "rest.jndi.factory.initial";
  public static final String BUNDLE_JNDI_FACTORY_HOST_PROP = "rest.jndi.factory.host";
  public static final String BUNDLE_JNDI_FACTORY_PORT_PROP = "rest.jndi.factory.port";
  
  public static final String BUNDLE_REST_ADMIN_ROOT = "rest.admin.root";
  public static final String BUNDLE_REST_ADMIN_PASS = "rest.admin.password";
  public static final String BUNDLE_REST_ADMIN_IP_ALLOWED = "rest.admin.ipallowed";
  
  public static final String BUNDLE_JMS_USER = "rest.admin.jms.user";
  public static final String BUNDLE_JMS_PASS = "rest.admin.jms.password";
  
  // Singleton
  private static AdminHelper helper = null;
  
  private InitialContext ictx;
  private BundleContext bundleContext;
  private Properties jndiProps;
  private JoramAdmin joramAdmin;
  private Connection cnx;
  
  private String restAdminRoot;
  private String restAdminPass;
  
  private String restAdminIPAllowed;
  private IPFilter ipfilter;
  
  private AdminHelper() {  }
  
  static public AdminHelper getInstance() {
    if (helper == null)
      helper = new AdminHelper();
    return helper;
  }
  
  /**
   * @return the restAdminRoot
   */
  public String getRestAdminRoot() {
    return restAdminRoot;
  }

  /**
   * @return the restAdminPass
   */
  public String getRestAdminPass() {
    return restAdminPass;
  }

  /**
   * @return the restAdminIPAllowed
   */
  public String getRestAdminIPAllowed() {
    return restAdminIPAllowed;
  }

  /**
   * Check if the addr is authorized (all local address is authorized).
   * 
   * @param addr The ip address to check
   * @return true if authorized
   * @throws UnknownHostException
   * @throws SocketException
   */
  public boolean checkIpAllowed(String addr) {
    return ipfilter.checkIpAllowed(addr);
  }

  public boolean authenticationRequired() {
    return restAdminRoot != null && !restAdminRoot.isEmpty() &&
        restAdminPass != null && !restAdminPass.isEmpty();
  }
  
  String jmsRootUser = null;
  String jmsRootPassword = null;
  
  public void startJoramAdmin(String name) throws ConnectException, AdminException, JMSException {
    if (joramAdmin == null) {
      ConnectionFactory cf = LocalConnectionFactory.create();
      if (jmsRootUser == null)
        jmsRootUser = LocalConnectionFactory.getDefaultRootLogin();
      if (jmsRootPassword == null)
        jmsRootPassword = LocalConnectionFactory.getDefaultRootPassword();
      cnx = cf.createConnection(jmsRootUser, jmsRootPassword);
      cnx.start();
      joramAdmin = new JoramAdmin(cnx, name);
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, "joramAdmin = " + joramAdmin);
    }
  }
  
  public void stopJoramAdmin() {
    if (joramAdmin != null && !joramAdmin.isClosed()) {
      joramAdmin.close();
      joramAdmin = null;
      try {
        cnx.close();
      } catch (JMSException e) {
        if (logger.isLoggable(BasicLevel.ERROR))
          logger.log(BasicLevel.ERROR, "", e);
      }
    }
  }
  
  public JoramAdmin getJoramAdmin() {
    return joramAdmin;
  }

  public void init(BundleContext bundleContext) throws Exception {
    this.bundleContext = bundleContext;
    
    restAdminRoot = bundleContext.getProperty(BUNDLE_REST_ADMIN_ROOT);
    restAdminPass = bundleContext.getProperty(BUNDLE_REST_ADMIN_PASS);
    
    restAdminIPAllowed = bundleContext.getProperty(BUNDLE_REST_ADMIN_IP_ALLOWED);
    if (logger.isLoggable(BasicLevel.INFO))
      logger.log(BasicLevel.INFO, "IPFilter allowedList = " + restAdminIPAllowed);
    ipfilter = new IPFilter(restAdminIPAllowed);
    
    String name = "dlft-admin";

    jmsRootUser = bundleContext.getProperty(BUNDLE_JMS_USER);
    jmsRootPassword = bundleContext.getProperty(BUNDLE_JMS_USER);

    startJoramAdmin(name);
    
    // set the jndi properties
    if (bundleContext.getProperty(BUNDLE_JNDI_FACTORY_INITIAL_PROP) != null && 
        bundleContext.getProperty(BUNDLE_JNDI_FACTORY_HOST_PROP) != null &&
        bundleContext.getProperty(BUNDLE_JNDI_FACTORY_PORT_PROP) != null) {
      jndiProps = new Properties();
      jndiProps.setProperty("java.naming.factory.initial", bundleContext.getProperty(BUNDLE_JNDI_FACTORY_INITIAL_PROP));
      jndiProps.setProperty("java.naming.factory.host", bundleContext.getProperty(BUNDLE_JNDI_FACTORY_HOST_PROP));
      jndiProps.setProperty("java.naming.factory.port", bundleContext.getProperty(BUNDLE_JNDI_FACTORY_PORT_PROP));
    } else {
      jndiProps = new Properties();
      jndiProps.setProperty("java.naming.factory.initial", "fr.dyade.aaa.jndi2.client.NamingContextFactory");
      jndiProps.setProperty("java.naming.factory.host", "localhost");
      jndiProps.setProperty("java.naming.factory.port", "16400");
    }
    
    ictx = new InitialContext(jndiProps);
    
    // TODO: use the osgi service jndi ?
//    ServiceReference<ObjectFactory> ref = bundleContext.getServiceReference(javax.naming.spi.ObjectFactory.class);
//    ObjectFactory jndiFactory = bundleContext.getService(ref);
//    context = (scnURLContext) jndiFactory.getObjectInstance(null, null, null, jndiProps);
    
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "jndiProperties = " + jndiProps);
   }

  public ArrayList<Destination> getQueueNames(int serverId) throws ConnectException, AdminException {
    Destination[] destinations = joramAdmin.getDestinations(serverId);
    ArrayList<Destination> list = new ArrayList<Destination>();
    for (Destination dest : destinations) {
      if (dest.isQueue()) {
        list.add(dest);
      }
    }
    return list;
  }
  
  public Destination createQueue(String name) throws ConnectException, AdminException {
    return joramAdmin.createQueue(name);
  }
  
  public Destination createQueue(int serverId, String name) throws ConnectException, AdminException {
    return joramAdmin.createQueue(serverId, name);
  }
  
  public Destination createQueue(int serverId, String name, String className, Properties props) throws ConnectException, AdminException {
    return joramAdmin.createQueue(serverId, name, className, props);
  }
  
  public void deleteQueue(int serverId, String name) throws ConnectException, AdminException, JMSException {
    Destination dest = createQueue(serverId, name);
    if (dest != null)
      dest.delete();
  }
  
  public ArrayList<Destination> getTopicNames(int serverId) throws ConnectException, AdminException {
    Destination[] destinations = joramAdmin.getDestinations(serverId);
    ArrayList<Destination> list = new ArrayList<Destination>();
    for (Destination dest : destinations) {
      if (dest.isTopic()) {
        list.add(dest);
      }
    }
    return list;
  }
  
  public Destination createTopic(String name) throws ConnectException, AdminException {
    return joramAdmin.createTopic(name);
  }
  
  public Destination createTopic(int serverId, String name) throws ConnectException, AdminException {
    return joramAdmin.createTopic(serverId, name);
  }
  
  public Destination createTopic(int serverId, String name, String className, Properties props) throws ConnectException, AdminException {
    return joramAdmin.createTopic(serverId, name, className, props);
  }

  public void deleteTopic(int serverId, String name) throws ConnectException, AdminException, JMSException {
    Destination dest = createTopic(serverId, name);
    if (dest != null)
      dest.delete();
  }
  
  public User createUser(String name, String password) throws ConnectException, AdminException {
    return joramAdmin.createUser(name, password);
  }
  
  public User createUser(String name, String password, int serverId, String identityClassName, Properties props) throws ConnectException, AdminException {
    return joramAdmin.createUser(name, password, serverId, identityClassName, props);
  }
  
  public void deleteUser(String name, String password, int serverId) throws ConnectException, AdminException {
    User user = joramAdmin.createUser(name, password, serverId);
    if (user != null)
      user.delete();
  }
  
  public ConnectionFactory createTcpConnectionFactory() {
    return TcpConnectionFactory.create();
  }
  
  public ConnectionFactory createTcpConnectionFactory(String host, int port, String reliableClass) {
    return TcpConnectionFactory.create(host, port, reliableClass);
  }
  
  public ConnectionFactory createlocalConnectionFactory() {
    return LocalConnectionFactory.create();
  }

  public void rebind(String name, Destination dest) throws NamingException {
    ictx.rebind(name, dest);
  }
  
  public void rebind(String name, ConnectionFactory cf) throws NamingException {
    ictx.rebind(name, cf);
  }
  
  public void unbind(String name) throws NamingException {
    ictx.unbind(name);
  }
  
  public int getLocalServerId() throws ConnectException, AdminException {
    return joramAdmin.getLocalServerId();
  }
  
}
