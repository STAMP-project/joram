/**
 * (c)2010 Scalagent Distributed Technologies
 * @author Yohann CINTRE
 */

package com.scalagent.appli.client.event.message;

import com.google.gwt.event.shared.EventHandler;
import com.scalagent.appli.shared.MessageWTO;


public interface DeletedMessageHandler extends EventHandler {

	public void onMessageDeleted(MessageWTO message, String queueName);
	
}