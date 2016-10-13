/*
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
package org.ow2.joram.monitoring;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.FileInputStream;
import java.util.Timer;
import java.util.Properties;

import org.objectweb.util.monolog.api.BasicLevel;
import org.objectweb.util.monolog.api.Logger;

import fr.dyade.aaa.common.Debug;
import fr.dyade.aaa.agent.AgentServer;
import fr.dyade.aaa.common.monitoring.MonitoringTimerTask;
import fr.dyade.aaa.common.monitoring.FileMonitoringTimerTask;
import fr.dyade.aaa.common.monitoring.LogMonitoringTimerTask;
import fr.dyade.aaa.common.monitoring.WindowMonitoringTimerTask;
import fr.dyade.aaa.util.management.MXWrapper;

public class Monitoring {
  public static Logger logger = Debug.getLogger(Monitoring.class.getName());

  private static MonitoringTimerTask fileMonitoringTimerTask = null;
  private static MonitoringTimerTask logMonitoringTimerTask = null;
  private static MonitoringTimerTask windowMonitoringTimerTask = null;

  public static String resultPath = null;

  /**
   * Timer provided by the monitoring.
   */
  private static Timer timer;

  /**
   * Method called by Activator to initialize the monitoring service.
   */
  public static void init() throws Exception {
    String config = AgentServer.getProperty(FileMonitoringTimerTask.MONITORING_CONFIG_PATH_PROPERTY,
                                            FileMonitoringTimerTask.DEFAULT_MONITORING_CONFIG_PATH);
    try {
      // if "fileMonitoring.props" file exists configure a FileMonitoringTimerTask.
      File file = new File(config);
      if (file.exists()) {
        long period = AgentServer.getLong(FileMonitoringTimerTask.MONITORING_CONFIG_PERIOD_PROPERTY,
                                          FileMonitoringTimerTask.DEFAULT_MONITORING_CONFIG_PERIOD).longValue();
        String results = AgentServer.getProperty(FileMonitoringTimerTask.MONITORING_RESULT_PATH_PROPERTY,
                                                 FileMonitoringTimerTask.DEFAULT_MONITORING_RESULT_PATH);

        Properties monitoringProps = new Properties();
        monitoringProps.load(new FileInputStream(file));

        fileMonitoringTimerTask = new FileMonitoringTimerTask(getTimer(), period, monitoringProps, results);
        try {
          fileMonitoringTimerTask.MBean_name = "server=" + AgentServer.getName() + ",cons=FileMonitoring";
          MXWrapper.registerMBean(fileMonitoringTimerTask,
                                  "AgentServer", fileMonitoringTimerTask.MBean_name);
        } catch (Exception exc) {
          logger.log(BasicLevel.ERROR, AgentServer.getName() + " jmx failed", exc);
        }
      }
    } catch (Exception exc) {
      logger.log(BasicLevel.WARN, AgentServer.getName() + " Cannot read monitoring configuration file: " + config, exc);
    }

    config = AgentServer.getProperty(LogMonitoringTimerTask.MONITORING_CONFIG_PATH_PROPERTY,
                                     LogMonitoringTimerTask.DEFAULT_MONITORING_CONFIG_PATH);
    try {
      // if "logMonitoring.props" file exists configure a LogMonitoringTimerTask.
      File file = new File(config);
      if (file.exists()) {
        long period = AgentServer.getLong(LogMonitoringTimerTask.MONITORING_CONFIG_PERIOD_PROPERTY,
                                          LogMonitoringTimerTask.DEFAULT_MONITORING_CONFIG_PERIOD).longValue();        
        String logname = AgentServer.getProperty(LogMonitoringTimerTask.MONITORING_RESULT_LOGGER_PROPERTY,
                                                 LogMonitoringTimerTask.DEFAULT_MONITORING_RESULT_LOGGER);
        Logger l = Debug.getLogger(logname);
        int level = AgentServer.getInteger(LogMonitoringTimerTask.MONITORING_RESULT_LEVEL_PROPERTY,
                                              LogMonitoringTimerTask.DEFAULT_MONITORING_RESULT_LEVEL).intValue();
        String msg = AgentServer.getProperty(LogMonitoringTimerTask.MONITORING_RESULT_MESSAGE_PROPERTY,
                                                LogMonitoringTimerTask.DEFAULT_MONITORING_RESULT_MESSAGE);

        Properties monitoringProps = new Properties();
        monitoringProps.load(new FileInputStream(file));

        logMonitoringTimerTask = new LogMonitoringTimerTask(getTimer(), period, monitoringProps, l, msg, level);
        try {
          logMonitoringTimerTask.MBean_name = "server=" + AgentServer.getName() + ",cons=LogMonitoring";
          MXWrapper.registerMBean(logMonitoringTimerTask,
                                  "AgentServer", logMonitoringTimerTask.MBean_name);
        } catch (Exception exc) {
          logger.log(BasicLevel.ERROR, AgentServer.getName() + " jmx failed", exc);
        }
      }
    } catch (Exception exc) {
      logger.log(BasicLevel.WARN, AgentServer.getName() + " Cannot read monitoring configuration file: " + config, exc);
    }

    config = AgentServer.getProperty(WindowMonitoringTimerTask.MONITORING_CONFIG_PATH_PROPERTY,
                                            WindowMonitoringTimerTask.DEFAULT_MONITORING_CONFIG_PATH);
    try {
      // if "fileMonitoring.props" file exists configure a FileMonitoringTimerTask.
      File file = new File(config);
      if (file.exists()) {
        long period = AgentServer.getLong(WindowMonitoringTimerTask.MONITORING_CONFIG_PERIOD_PROPERTY,
                                          WindowMonitoringTimerTask.DEFAULT_MONITORING_CONFIG_PERIOD).longValue();

        Properties monitoringProps = new Properties();
        monitoringProps.load(new FileInputStream(file));

        windowMonitoringTimerTask = new WindowMonitoringTimerTask(getTimer(), period, monitoringProps);
        try {
          windowMonitoringTimerTask.MBean_name = "server=" + AgentServer.getName() + ",cons=WindowMonitoring";
          MXWrapper.registerMBean(windowMonitoringTimerTask,
                                  "AgentServer", windowMonitoringTimerTask.MBean_name);
        } catch (Exception exc) {
          logger.log(BasicLevel.ERROR, AgentServer.getName() + " jmx failed", exc);
        }
      }
    } catch (Exception exc) {
      logger.log(BasicLevel.WARN, AgentServer.getName() + " Cannot read monitoring configuration file: " + config, exc);
    }
  }

  /**
   * Forces this thread to stop executing.
   * This method stops all consumers and services.
   */
  public static void stop() throws Exception {
    try {
      if (fileMonitoringTimerTask != null) {
        MXWrapper.unregisterMBean("MonitoringService", fileMonitoringTimerTask.MBean_name);
        fileMonitoringTimerTask.cancel();
      }
      fileMonitoringTimerTask = null;


      if (logMonitoringTimerTask != null) {
        MXWrapper.unregisterMBean("MonitoringService", logMonitoringTimerTask.MBean_name);
        logMonitoringTimerTask.cancel();
      }
      logMonitoringTimerTask = null;

      if (windowMonitoringTimerTask != null) {
        MXWrapper.unregisterMBean("MonitoringService", windowMonitoringTimerTask.MBean_name);        
        windowMonitoringTimerTask.cancel();
      }
      windowMonitoringTimerTask = null;

      if (timer != null)
        timer.cancel();
      timer = null;
    } catch (Throwable t) {
      logger.log(BasicLevel.ERROR, "Cannot stop", t);
    }
  }

  /**
   * Returns a shared timer provided by the monitoring service.
   */
  public static synchronized final Timer getTimer() {
    if (timer == null) {
      timer = new Timer();
    }
    return timer;
  }
}
