/**
 * (c)2010 Scalagent Distributed Technologies
 * @author Yohann CINTRE
 */


package com.scalagent.appli.client.command.user;

import com.google.gwt.event.shared.HandlerManager;
import com.scalagent.engine.client.command.Handler;


public abstract class LoadUserResponseHandler extends Handler<LoadUserResponse>{
  
  public LoadUserResponseHandler(HandlerManager eventBus){
    super(eventBus);
  }

}
