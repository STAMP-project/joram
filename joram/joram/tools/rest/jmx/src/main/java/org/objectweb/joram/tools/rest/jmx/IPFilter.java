/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2018 ScalAgent Distributed Technologies
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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.objectweb.util.monolog.api.BasicLevel;
import org.objectweb.util.monolog.api.Logger;
import fr.dyade.aaa.common.Debug;

public class IPFilter {
  public static Logger logger = Debug.getLogger(IPFilter.class.getName());
  
  class Authorized {
    String addr;
    int ip;
    int mask;
    
    public Authorized(String addr, int ip, int mask) {
      this.addr = addr;
      this.ip = ip;
      this.mask = mask;
    }
    
    public String toString() {
      return addr;
    }
  }

  private ArrayList<Authorized> ipAlloweds = null;

  /**
   * Creates list of IP allowed list from declaration.
   */
  public IPFilter(String IPAllowedList) {
    if (IPAllowedList == null) return;
    IPAllowedList = IPAllowedList.trim();
    if (IPAllowedList.length() == 0) return;
    
    ipAlloweds = new ArrayList<Authorized>();
    StringTokenizer st = new StringTokenizer(IPAllowedList, ",");
    while (st.hasMoreTokens()) {
      String addr = st.nextToken().trim();
      String[] parts = addr.split("/");
      String ip = parts[0];
      int prefix;

      if (parts.length < 2) {
        prefix = 0;
      } else {
        prefix = Integer.parseInt(parts[1]);
      }
      Inet4Address a = null;
      try {
        a = (Inet4Address) InetAddress.getByName(ip);
      } catch (UnknownHostException e) { }

      byte[] b = a.getAddress();
      ipAlloweds.add(new Authorized(addr,
          ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | ((b[3] & 0xFF) << 0),
          ~((1 << (32 - prefix)) - 1)));
    }
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
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "IPFilter.checkIpAllowed address=" + addr + ", ipAlloweds=" + ipAlloweds);

    if (ipAlloweds == null)
      return true;
    
    try {
      Inet4Address a = (Inet4Address) InetAddress.getByName(addr);

      if (a.isAnyLocalAddress() || a.isLoopbackAddress())
        return true;
      // Check if the address is defined on any interface
      if(NetworkInterface.getByInetAddress(a) != null)
        return true;

      byte[] b = a.getAddress();
      int ipInt = ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | ((b[3] & 0xFF) << 0);

      for (Authorized authorized : ipAlloweds) {
        if ((authorized.ip & authorized.mask) == (ipInt & authorized.mask)) {
          if (logger.isLoggable(BasicLevel.DEBUG))
            logger.log(BasicLevel.DEBUG, "IPFilter.checkIpAllowed address " + addr + " authorized.");
          return true;
        }
      }
    } catch (Exception exc) {
      logger.log(BasicLevel.WARN, "IPFilter.checkIpAllowed " + addr, exc);
    }
    if (logger.isLoggable(BasicLevel.INFO))
      logger.log(BasicLevel.INFO, "IPFilter.checkIpAllowed " + addr + " failed.");
    return false;
  }
}
