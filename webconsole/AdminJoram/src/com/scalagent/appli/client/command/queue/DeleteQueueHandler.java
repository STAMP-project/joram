/**
 * (c)2010 Scalagent Distributed Technologies
 * @author Yohann CINTRE
 */

package com.scalagent.appli.client.command.queue;

import com.google.gwt.event.shared.HandlerManager;
import com.scalagent.engine.client.command.Handler;

public abstract class DeleteQueueHandler extends Handler<DeleteQueueResponse> {
	
	public DeleteQueueHandler(HandlerManager eventBus) {
		super(eventBus);
	}
	
}