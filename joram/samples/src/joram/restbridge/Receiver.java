/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2017 ScalAgent Distributed Technologies
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
package restbridge;

import java.util.Properties;
import javax.jms.*;
import javax.naming.*;

/**
 * Receives messages from the queue.
 */
public class Receiver {

  public static void main(String[] args) throws Exception {

    System.out.println("Receive from the rest bridge");

    Properties jndiProps = new Properties();
    jndiProps.setProperty("java.naming.factory.initial", "fr.dyade.aaa.jndi2.client.NamingContextFactory");
    jndiProps.setProperty("java.naming.factory.host", "localhost");
    jndiProps.setProperty("java.naming.factory.port", "16401");
 
    javax.naming.Context jndiCtx = new javax.naming.InitialContext(jndiProps);
    Destination bridgeDest = (Destination) jndiCtx.lookup("queueAcq");
    ConnectionFactory bridgeCF = (ConnectionFactory) jndiCtx.lookup("bridgeCF");
    jndiCtx.close();

    Connection qc = bridgeCF.createConnection();
    Session qs = qc.createSession(false, Session.AUTO_ACKNOWLEDGE);
    MessageConsumer qrec = qs.createConsumer(bridgeDest);

    qc.start();

    for (int j = 0; j < 10; j++) {
      Message msg = qrec.receive();
      if (msg instanceof TextMessage)
        System.out.println("Received: " + ((TextMessage) msg).getText()); 
    }

    qc.close();

    System.out.println();
    System.out.println("Consumer closed.");
  }
}
