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
 */
package fr.dyade.aaa.ext;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Properties;

import fr.dyade.aaa.agent.AgentServer;
import fr.dyade.aaa.util.DBTransaction;

import org.objectweb.util.monolog.api.BasicLevel;

public class JDBCTransaction extends DBTransaction implements JDBCTransactionMBean {
  public final static String JDBC_TRANSACTION_PREFIX = "org.ow2.joram.jdbc.transaction"; 
  
  /* example: "com.mysql.jdbc.Driver" */
  public final static String JDBC_DRIVER_PROP = JDBC_TRANSACTION_PREFIX + ".driver";
  
  public final static String JDBC_URL_PROP = JDBC_TRANSACTION_PREFIX + ".url";
  
  /* example: "jdbc:mysql" */
  public final static String JDBC_DB_PROTOCOL_PROP = JDBC_TRANSACTION_PREFIX + ".protocol";
  
  public final static String JDBC_DB_HOST_PROP = JDBC_TRANSACTION_PREFIX + ".host";
  public final static String JDBC_DB_PORT_PROP = JDBC_TRANSACTION_PREFIX + ".port";
  
  public final static String JDBC_DB_USER_PROP = JDBC_TRANSACTION_PREFIX + ".user";
  public final static String JDBC_DB_PASS_PROP = JDBC_TRANSACTION_PREFIX + ".password";
  
  public final static String JDBC_DB_NAME_PROP = JDBC_TRANSACTION_PREFIX + ".dbname";
  public final static String DFLT_JDBC_DB_NAME = "JoramDB" + AgentServer.getServerId();
  
  public final static String JDBC_DB_INIT_PROP = JDBC_TRANSACTION_PREFIX + ".dbinit";
  // For MySQL: "CREATE TABLE JoramDB (name VARCHAR(255), content LONGBLOB, PRIMARY KEY(name))";
  // For Derby: "CREATE TABLE JoramDB (name VARCHAR(256), content LONG VARCHAR FOR BIT DATA, PRIMARY KEY(name))";
  
  public final static String JDBC_PROPS_FILE_PROP = JDBC_TRANSACTION_PREFIX + ".properties";
  
  private String driver;
  private String connurl;

  private String protocol;

  private String host;
  private String port;
  
  private String user;
  private String password;

  private String dbname;
  private String dbinit;

  private String path = null;
  private Properties props = null;
  
  @Override
  protected void initDB() throws IOException {
    driver = AgentServer.getProperty(JDBC_DRIVER_PROP);
    if (driver == null)
      throw new IOException("Driver property is undefined");
    
    connurl = AgentServer.getProperty(JDBC_URL_PROP);
    
    protocol = AgentServer.getProperty(JDBC_DB_PROTOCOL_PROP);
    host = AgentServer.getProperty(JDBC_DB_HOST_PROP);
    port = AgentServer.getProperty(JDBC_DB_PORT_PROP);
    
    props = new Properties();
    path = AgentServer.getProperty(JDBC_PROPS_FILE_PROP);
    if (path != null) {
      try {
        FileInputStream fis = new FileInputStream(path);
        props.load(fis);
      } catch (Exception exc) {
        logmon.log(BasicLevel.FATAL,
                   "JDBCTransaction.initDB: Cannot load properties from " + path);
        throw new IOException("Bad JDBC configuration", exc);
      }
    }

    user = AgentServer.getProperty(JDBC_DB_USER_PROP);
    if (user != null) {
      if (props.containsKey("user")) {
        logmon.log(BasicLevel.WARN,
            "JDBCTransaction.initDB: user already defined in JDBC properties, cannot overload it.");
      } else {
        props.setProperty("user", user);
      }
    }
    password = AgentServer.getProperty(JDBC_DB_PASS_PROP);
    if (password != null) {
      if (props.containsKey("password")) {
        logmon.log(BasicLevel.WARN,
            "JDBCTransaction.initDB: password already defined in JDBC properties, cannot overload it.");
      } else {
        props.setProperty("password", password);
      }
    }

    if ((props.getProperty("user") == null) || (props.getProperty("password") == null)) {
      logmon.log(BasicLevel.FATAL, "JDBCTransaction.initDB: need to define authentication parameters.");
      throw new IOException("Bad JDBC configuration");
    }

    dbname = AgentServer.getProperty(JDBC_DB_NAME_PROP, DFLT_JDBC_DB_NAME);
    
    dbinit = AgentServer.getProperty(JDBC_DB_INIT_PROP);
    if (dbinit == null) {
      logmon.log(BasicLevel.FATAL, "JDBCTransaction.initDB: JDBC init statement not defined.");
      throw new IOException("Bad JDBC configuration");
    }

    if (connurl != null) {
      if ((protocol != null) || (host != null) || (port != null)) {
        logmon.log(BasicLevel.WARN,
                   "JDBCTransaction.initDB: JDBC URL defined, ignore other parameters (protocol, host and port).");
      }
    } else {
      if ((protocol == null) || (host == null) || (port == null)) {
        logmon.log(BasicLevel.FATAL,
                   "JDBCTransaction.initDB: Should define JDBC URL or protocol, host and port parameters.");
        throw new IOException("Bad JDBC configuration");
      }
      
      // Builds the URL from parameters
      StringBuffer strbuf = new StringBuffer(protocol).append("://");
      strbuf.append(host).append(':').append(port).append('/');
      strbuf.append(dbname);
      
      connurl = strbuf.toString();
    }
    
    try {
      Class.forName(driver).newInstance();
      conn = DriverManager.getConnection(connurl, props);
      conn.setAutoCommit(false);
    } catch (SQLException|InstantiationException|IllegalAccessException|ClassNotFoundException exc) {
      throw new IOException("JDBCTransaction.initDB:", exc);
    }


    try {
      // Creating a statement lets us issue commands against the connection.
      Statement s = conn.createStatement();
      // We create the table.
      s.execute(dbinit);
      s.close();
      
      conn.commit();
    } catch (SQLException sqle) {
      if (logmon.isLoggable(BasicLevel.WARN))
        logmon.log(BasicLevel.WARN, "DBTransaction, init(): DB already exists", sqle);
    }
  }

  @Override
  public String getDriver() {
    return driver;
  }

  @Override
  public String getURL() {
    return connurl;
  }

  @Override
  public Properties getClientInfo() {
    if (conn == null) return null;
    
    try {
      return conn.getClientInfo();
    } catch (SQLException exc) {
      return null;
    }
  }

  @Override
  public String getDBInitStatement() {
    return dbinit;
  }

  @Override
  public String getUser() {
    return user;
  }

  @Override
  public String getPropertiesPath() {
    return path;
  }
}
