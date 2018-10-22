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
package org.objectweb.joram.tools.rest.jms;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.StringTokenizer;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import javax.servlet.http.HttpServletRequest;

import org.objectweb.util.monolog.api.BasicLevel;
import org.objectweb.util.monolog.api.Logger;

import fr.dyade.aaa.common.Debug;

@Path("/")
public class RootService implements ContainerRequestFilter {
  public static Logger logger = Debug.getLogger(RootService.class.getName());
  
  private static final String AUTHORIZATION_PROPERTY = "Authorization";
  private static final String AUTHENTICATION_SCHEME = "Basic";
  
  private final Helper helper = Helper.getInstance();

  @GET
  @Produces(MediaType.TEXT_HTML)
  public String info(@Context UriInfo uriInfo) {
    URI jndiURI = uriInfo.getBaseUriBuilder().path("jndi").build();
    URI jmsURI = uriInfo.getBaseUriBuilder().path("jms").build();
    URI adminURI = uriInfo.getBaseUriBuilder().path("admin").build();
    URI contextURI = uriInfo.getBaseUriBuilder().path("context").build();
    return "<html><body>" +
        "<br><a href=\""+jndiURI+"\">"+jndiURI+"</a>"+
        "<br><a href=\""+jmsURI+"\">"+jmsURI+"</a>"+
        "<br><a href=\""+adminURI+"\">"+adminURI+"</a>"+
        "<br><a href=\""+contextURI+"\">"+contextURI+"</a>"+
        "</body></html>";
  }

  @Context
  private HttpServletRequest httpServletRequest;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    if (!helper.authenticationRequired()) {
      if (logger.isLoggable(BasicLevel.INFO))
        logger.log(BasicLevel.INFO, "no authentication.");
      return;
    }
    
    if (httpServletRequest != null) {
      // JSR-315/JSR-339 compliant server
      String remoteIpAddress = httpServletRequest.getRemoteAddr();
      if (remoteIpAddress != null) {
        if (! helper.checkIPAddress(remoteIpAddress)) {
          Response response = Response.status(Response.Status.UNAUTHORIZED)
              .header("WWW-Authenticate", "Basic realm=\"executives\"")
              .entity("You cannot access this resource (IP not allowed)").build();
          requestContext.abortWith(response);
          return;
        }
      }
      if (logger.isLoggable(BasicLevel.DEBUG))
        logger.log(BasicLevel.DEBUG, "request from: " + remoteIpAddress);
    }
    // request headers
    final MultivaluedMap<String, String> headers = requestContext.getHeaders();
    // authorization header
    final List<String> authorization = headers.get(AUTHORIZATION_PROPERTY);
    
    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "authorization = " + authorization);
    
    if (authorization == null) {
      Response response = Response.status(Response.Status.UNAUTHORIZED)
          .header("WWW-Authenticate", "Basic realm=\"executives\"")
          .entity("You cannot access this resource").build();
      requestContext.abortWith(response);
      return;
    }
    
    // get encoded username and password
    final String encodedUserPassword = authorization.get(0).replaceFirst(AUTHENTICATION_SCHEME + " ", "");

    // decode username and password
    String usernameAndPassword = new String(Base64.getDecoder().decode(encodedUserPassword));
    final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
    String username = null;
    String password = null;
    if (tokenizer.hasMoreTokens()) {
      username = tokenizer.nextToken();
    }
    if (tokenizer.hasMoreTokens()) {
      password = tokenizer.nextToken();
    }

    if (logger.isLoggable(BasicLevel.DEBUG))
      logger.log(BasicLevel.DEBUG, "username = " + username);
    
    // Verifying username and password
    if (helper.getRestUser().equals(username) && helper.getRestPass().equals(password)) {
      // the valid authentication
      return;
    }
    
    if (logger.isLoggable(BasicLevel.WARN))
      logger.log(BasicLevel.WARN, "Bad authorization: " + username + ":" + password);

    Response response = Response.status(Response.Status.UNAUTHORIZED)
        .header("WWW-Authenticate", "Basic realm=\"executives\"")
        .entity("You cannot access this resource").build();
    requestContext.abortWith(response);
    return;
  }
}
