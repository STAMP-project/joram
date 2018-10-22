/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2016 ScalAgent Distributed Technologies
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
package org.objectweb.joram.tools.rest.jmx;

import java.net.SocketException;
import java.net.UnknownHostException;

import org.objectweb.util.monolog.api.BasicLevel;
import org.objectweb.util.monolog.api.Logger;
import org.osgi.framework.BundleContext;

import fr.dyade.aaa.common.Debug;

public class JmxHelper {
  public static Logger logger = Debug.getLogger(JmxHelper.class.getName());

  public static final String BUNDLE_REST_JMX_ROOT = "rest.jmx.root";
  public static final String BUNDLE_REST_JMX_PASS = "rest.jmx.password";
  public static final String BUNDLE_REST_JMX_IP_ALLOWED = "rest.jmx.ipallowed";
  
  // Singleton
  private static JmxHelper helper = null;
  
  private String restJmxRoot;
  private String restJmxPass;
  
  private String restJmxIPAllowed;
  private IPFilter ipfilter;

  private JmxHelper() { }
  
  static public JmxHelper getInstance() {
    if (helper == null)
      helper = new JmxHelper();
    return helper;
  }
  
  /**
   * @return the restJmxRoot
   */
  public String getRestJmxRoot() {
    return restJmxRoot;
  }

  /**
   * @return the restJmxPass
   */
  public String getRestJmxPass() {
    return restJmxPass;
  }

  /**
   * @return the restJmxIPAllowed
   */
  public String getRestJmxIPAllowed() {
    return restJmxIPAllowed;
  }

  /**
   * Check if the addr is authorized (all local address is authorized).
   * 
   * @param addr The ip address to check
   * @return true if authorized
   * @throws UnknownHostException
   * @throws SocketException
   */
  public boolean checkIPAddress(String addr) {
    return ipfilter.checkIpAllowed(addr);
  }

  public boolean authenticationRequired() {
    return restJmxRoot != null && !restJmxRoot.isEmpty() &&
        restJmxPass != null && !restJmxPass.isEmpty();
  }
 
  public void init(BundleContext bundleContext) throws Exception {
    restJmxRoot = bundleContext.getProperty(BUNDLE_REST_JMX_ROOT);
    restJmxPass = bundleContext.getProperty(BUNDLE_REST_JMX_PASS);
 
    restJmxIPAllowed = bundleContext.getProperty(BUNDLE_REST_JMX_IP_ALLOWED);
    if (logger.isLoggable(BasicLevel.INFO))
      logger.log(BasicLevel.INFO, "IPFilter allowedList = " + restJmxIPAllowed);
    ipfilter = new IPFilter(restJmxIPAllowed);
  }
}
