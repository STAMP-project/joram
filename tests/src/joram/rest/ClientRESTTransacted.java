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
 * Initial developer(s): ScalAgent D.T.
 * Contributor(s):
 */
package joram.rest;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;

import framework.TestCase;

/**
 * Test: 
 * - lockup queue
 * - create producer transacted
 * - send Text message
 * - create consumer transacted
 * - receive Text message => null
 * - prod commit
 * - receive Text message => !null
 * - cons rollback
 * - send Text message next
 * - prod rollback
 * - receive Text message => !null
 * - close 
 */
public class ClientRESTTransacted extends TestCase {

  public static void main(String[] args) {
    new ClientRESTTransacted().run();
  }

  public void run() {
    try {
      startAgentServer((short)0);
      
      // wait REST bundle
      Helper.waitConnection(Helper.getBaseJmsURI(), 10);
      
      ClientConfig config = new ClientConfig();
      Client client = ClientBuilder.newClient(config);
      WebTarget target = client.target(Helper.getBaseJmsURI());

      Builder builder = target.path("jndi").path("queue").request();
      Response response = builder.accept(MediaType.TEXT_PLAIN).head();
      assertEquals("jndi-queue", 201, response.getStatus());

      URI uriCreateCons = response.getLink("create-consumer-transacted").getUri();

      // create a transacted producer
      response = client.target(response.getLink("create-producer-transacted").getUri()).request().accept(MediaType.TEXT_PLAIN).post(null);
      assertEquals("create-producer-transacted", 201, response.getStatus());

      URI uriCloseProd = response.getLink("close-context").getUri();

      // the messages
      String message = "my test message";
      String messageNext = "my test message next";
      
      // send first message
      response = client.target(response.getLink("send-message").getUri()).request().
          accept(MediaType.TEXT_PLAIN).post(Entity.entity(message, MediaType.TEXT_PLAIN));
      assertEquals("send-message", 200, response.getStatus());
      
      URI uriProdCommit = response.getLink("commit").getUri();
      URI sendNextMsg = response.getLink("send-next-message").getUri();
      
      // create a transacted consumer
      response = client.target(uriCreateCons).request().accept(MediaType.TEXT_PLAIN).post(null);
      assertEquals("create-consumer", 201, response.getStatus());

      URI uriCloseCons = response.getLink("close-context").getUri();
      
      // consume the first message
      URI uriConsume = response.getLink("receive-message").getUri();
      builder = client.target(uriConsume).queryParam("timeout", "0").request().accept(MediaType.TEXT_PLAIN);
      response = builder.get();
      String msg = response.readEntity(String.class);
      assertEquals(200, response.getStatus());
      // the message must be null or empty (prod not commit)
      assertTrue("receive-message", msg == null || msg.isEmpty());
      
      // commit the first message
      response = client.target(uriProdCommit).request().accept(MediaType.TEXT_PLAIN).head();
      assertEquals("prod-commit", 200, response.getStatus());

      // (re) consume the first message
      builder = client.target(uriConsume).queryParam("timeout", "0").request().accept(MediaType.TEXT_PLAIN);
      response = builder.get();
      msg = response.readEntity(String.class);
      assertEquals(200, response.getStatus());
      assertNotNull("receive-message", msg);
      assertEquals("receive-message", message, msg);
      
      // consumer rollback
      URI uriConsRollback = response.getLink("rollback").getUri();
      response = client.target(uriConsRollback).request().accept(MediaType.TEXT_PLAIN).head();
      assertEquals("cons-rollback", 200, response.getStatus());
      
      // send the second message
      response = client.target(sendNextMsg).request()
          .accept(MediaType.TEXT_PLAIN).post(Entity.entity(messageNext, MediaType.TEXT_PLAIN));
      assertEquals("send-next-message", 200, response.getStatus());
      
      URI uriProdRollback = response.getLink("rollback").getUri();

      // producer rollback the second message
      response = client.target(uriProdRollback).request().accept(MediaType.TEXT_PLAIN).head();
      assertEquals("prod-rollback", 200, response.getStatus());
      
      // consume the first message
      builder = client.target(uriConsume).queryParam("timeout", "0").request().accept(MediaType.TEXT_PLAIN);
      response = builder.get();
      msg = response.readEntity(String.class);
      assertEquals(200, response.getStatus());
      assertNotNull("receive-message", msg);
      assertEquals("receive-message", message, msg);
      
      // consume the second message
      builder = client.target(uriConsume).queryParam("timeout", "0").request().accept(MediaType.TEXT_PLAIN);
      response = builder.get();
      msg = response.readEntity(String.class);
      assertEquals(200, response.getStatus());
      // the message must be null or empty (prod rollback)
      assertTrue("receive-message", msg == null || msg.isEmpty());

      response = client.target(uriCloseProd).request().accept(MediaType.TEXT_PLAIN).delete();
      assertEquals("close-producer", 200, response.getStatus());

      response = client.target(uriCloseCons).request().accept(MediaType.TEXT_PLAIN).delete();
      assertEquals("close-consumer", 200, response.getStatus());

    } catch (Throwable exc) {
      exc.printStackTrace();
      error(exc);
    } finally {
      stopAgentServer((short)0);
      endTest(); 
    }
  }
}
