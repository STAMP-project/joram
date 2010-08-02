/**
 * (c)2010 Scalagent Distributed Technologies
 */

package com.scalagent.appli.client.widget.handler.message;

import com.scalagent.appli.client.presenter.QueueListPresenter;
import com.scalagent.appli.client.widget.record.QueueListRecord;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

/**
 * @author Yohann CINTRE
 */
public class MessageDetailsClickHandler implements ClickHandler {


	private QueueListPresenter queuePresenter;
	private QueueListRecord record;
	
	
	public MessageDetailsClickHandler(QueueListPresenter queuePresenter, QueueListRecord record) {
		super();
		this.queuePresenter = queuePresenter;
		this.record = record;
	}
	
	@Override
	public void onClick(ClickEvent event) {

		queuePresenter.fireQueueDetailsClick(record.getQueue());
	}
	
	


}
