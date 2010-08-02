/**
 * (c)2010 Scalagent Distributed Technologies
 */

package com.scalagent.appli.client.command.subscription;

import com.scalagent.engine.client.command.Response;

/**
 * Response to the action SendNewSubscriptionAction.
 * 
 * @author Yohann CINTRE
 */
public class SendNewSubscriptionResponse implements Response {

	private boolean success;
	private String message;
	
	public SendNewSubscriptionResponse() {}
	
	public SendNewSubscriptionResponse(boolean success, String message) {
		this.success = success;
		this.message = message;
	}
	 
	public boolean isSuccess() {
		return success;
	}
	
	public String getMessage() {
		return message;
	}
	
}
