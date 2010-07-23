/**
 * (c)2010 Scalagent Distributed Technologies
 * @author Yohann CINTRE
 */

package com.scalagent.appli.client.command.topic;

import com.scalagent.engine.client.command.Response;

public class SendNewTopicResponse implements Response {

	private boolean success;
	private String message;
	
	public SendNewTopicResponse() {}
	
	public SendNewTopicResponse(boolean success, String message) {
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