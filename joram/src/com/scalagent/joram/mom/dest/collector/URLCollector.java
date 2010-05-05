/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2008 - 2010 ScalAgent Distributed Technologies
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
package com.scalagent.joram.mom.dest.collector;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;

import org.objectweb.joram.shared.excepts.MessageValueException;
import org.objectweb.joram.shared.messages.ConversionHelper;
import org.objectweb.joram.shared.messages.Message;

import fr.dyade.aaa.common.stream.Properties;

/**
 * 
 */
public class URLCollector implements Collector, Serializable {
 
  private static final long serialVersionUID = 1L;
  
  public static final String FILE = "collector.file";
  public static final String PATH = "collector.path";
  public static final String HOST = "collector.host";
  public static final String URL = "collector.url";
  public static final String TYPE = "collector.type";
  
  private CollectorDestination collectorDest;
  private String urlStr = null;
  private int type = Message.BYTES;
  
  /**
   * get the file.
   * 
   * @param spec the String to parse as a URL.
   * @return the file in byte format.
   * @throws IOException
   */
  private byte[] getResource(String spec, Properties prop) throws IOException {
    ByteArrayOutputStream baos = null;
    BufferedOutputStream bos = null;
    try {
      URL url = new URL(spec);

      URLConnection urlc = url.openConnection();
      InputStream is = urlc.getInputStream();

      baos = new ByteArrayOutputStream();
      bos = new BufferedOutputStream(baos);

      prop.put(FILE, url.getFile());
      prop.put(PATH, url.getPath());
      prop.put(HOST, url.getHost());

      int c = is.read();
      while (c != -1) {
        bos.write(c);
        c = is.read();
      }
      bos.flush();
      return baos.toByteArray();
    } finally {
      if (bos != null)
        bos.close();
      if (baos != null)
        baos.close();
    }
  }

  /**
   * Check the URL resource.
   * Store file in Queue or send to topic (collector destination.
   * 
   * @see com.scalagent.joram.mom.dest.collector.Collector#check()
   */
  public void check() throws IOException {
    Properties prop = new Properties();
    prop.put(URL, urlStr);
    byte[] tab = getResource(urlStr, prop);
    collectorDest.sendMessage(type, tab, prop);
  }

  public void setCollectorDestination(CollectorDestination collectorDest) {
    this.collectorDest = collectorDest;
  }

  public void setProperties(java.util.Properties properties) {
    urlStr = properties.getProperty(URL);
    try {
      type = ConversionHelper.toByte(properties.getProperty(TYPE));
    } catch (MessageValueException e) {
      type = Message.BYTES;
    }    
  }

}
