/**
 * (c)2010 Scalagent Distributed Technologies
 * @author Yohann CINTRE
 */


package com.scalagent.appli.client.command.topic;

import com.google.gwt.event.shared.HandlerManager;
import com.scalagent.engine.client.command.Handler;


public abstract class LoadTopicResponseHandler extends Handler<LoadTopicResponse>{
  
  public LoadTopicResponseHandler(HandlerManager eventBus){
    super(eventBus);
  }

}
