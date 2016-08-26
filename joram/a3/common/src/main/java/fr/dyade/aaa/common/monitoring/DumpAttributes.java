/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2010 - 2016 ScalAgent Distributed Technologies
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
package fr.dyade.aaa.common.monitoring;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.util.monolog.api.BasicLevel;
import org.objectweb.util.monolog.api.Logger;

import fr.dyade.aaa.common.Debug;
import fr.dyade.aaa.common.Strings;
import fr.dyade.aaa.util.management.MXWrapper;

/**
 * The <code>DumpAttributes</code> class allows to watch all JMX attributes of a
 * specified domain and store the corresponding values in a file.
 */
public class DumpAttributes {
  public static Logger logger = Debug.getLogger(DumpAttributes.class.getName());

  /**
   * Records information about the specified attribute.
   * 
   * @param strbuf A StringBuffer to write into.
   * @param mbean  The name of the related mbean.
   * @param att    The name of the related attribute.
   * @param value  The value of the related attribute.
   */
  static void addRecord(StringBuffer strbuf, String mbean, String att, Object value) {
    strbuf.append(mbean).append(':').append(att).append('=');
    Strings.toString(strbuf, value);
    strbuf.append('\n');
  }
  
  /**
   * Adds a complete Thread dump to the StringBuilder given in parameter.
   * 
   * @param dump  The StringBuilder to append.
   * @return      The StringBuilder given in parameter.
   */  
  public static StringBuffer dumpAllStackTraces(StringBuffer dump) {
    Map<Thread, StackTraceElement[]> stacktraces = Thread.getAllStackTraces();

    for (Thread thread : stacktraces.keySet()) {
      Thread.State state = thread.getState();
      dump.append(String.format("\"%s\" %s prio=%d tid=%d nid=1 %s\njava.lang.Thread.State: %s",
          thread.getName(),
          (thread.isDaemon() ? "daemon" : ""),
          thread.getPriority(),
          thread.getId(),
          Thread.State.WAITING.equals(state) ? "in Object.wait()" : state.name().toLowerCase(),
              (state.equals(Thread.State.WAITING) ? "WAITING (on object monitor)" : state)));
      final StackTraceElement[] stacktrace = stacktraces.get(thread);
      for (final StackTraceElement stackTraceElement : stacktrace) {
        dump.append("\n\tat ").append(stackTraceElement);
      }
      dump.append("\n---\n");
    }
    return dump;
  }
  
//public static StringBuilder dumpAllStackTraces(StringBuilder dump) {
//final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
//final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
//
//for (ThreadInfo threadInfo : threadInfos) {
//  dump.append('"');
//  dump.append(threadInfo.getThreadName());
//  dump.append("\" ");
//  final Thread.State state = threadInfo.getThreadState();
//  dump.append("\n   java.lang.Thread.State: ");
//  dump.append(state);
//
//  final StackTraceElement[] stacktrace = threadInfo.getStackTrace();
//  for (final StackTraceElement stackTraceElement : stacktrace) {
//    dump.append("\n        at ").append(stackTraceElement);
//  }
//  dump.append("\n\n");
//}
//
//return dump;
//}

  public static StringBuffer dumpAttributes(String name, StringBuffer strbuf) {
    Set<String> mBeans = null;
    try {
      mBeans = MXWrapper.queryNames(name);
    } catch (Exception exc) {
      logger.log(BasicLevel.ERROR, "DumpAttributes.dumpAttributes, bad name: " + name, exc);
      return strbuf;
    }

    if (mBeans != null) {
      for (Iterator<String> iterator = mBeans.iterator(); iterator.hasNext();) {
        String mBean = iterator.next();

        // Get all mbean's attributes
        try {
          List<String> attributes = MXWrapper.getAttributeNames(mBean);
          if (attributes != null) {
            for (int i = 0; i < attributes.size(); i++) {
              String attname = (String) attributes.get(i);
              try {
                addRecord(strbuf, mBean, attname, MXWrapper.getAttribute(mBean, attname));
              } catch (Exception exc) {
                if (logger.isLoggable(BasicLevel.DEBUG))
                  logger.log(BasicLevel.DEBUG,
                             "DumpAttributes.dumpAttributes, bad attribute : " + mBean + ":" + attname, exc);
                else
                  logger.log(BasicLevel.WARN,
                             "DumpAttributes.dumpAttributes, bad attribute : " + mBean + ":" + attname);
              }
            }
          }
        } catch (Exception exc) {
          logger.log(BasicLevel.ERROR, "DumpAttributes.dumpAttributes", exc);
        }
      }
    }
    return strbuf;
  }
}
