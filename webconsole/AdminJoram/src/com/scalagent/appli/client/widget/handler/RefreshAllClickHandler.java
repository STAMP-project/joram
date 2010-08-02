/**
 * (c)2010 Scalagent Distributed Technologies
 */

package com.scalagent.appli.client.widget.handler;

import com.scalagent.appli.client.presenter.QueueDetailPresenter;
import com.scalagent.appli.client.presenter.QueueListPresenter;
import com.scalagent.appli.client.presenter.SubscriptionListPresenter;
import com.scalagent.appli.client.presenter.TopicListPresenter;
import com.scalagent.appli.client.presenter.UserDetailPresenter;
import com.scalagent.appli.client.presenter.UserListPresenter;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

/**
 * @author Yohann CINTRE
 */
public class RefreshAllClickHandler implements ClickHandler {


	private QueueListPresenter queuePresenter;
	private QueueDetailPresenter queueDetailPresenter;
	private TopicListPresenter topicPresenter;
	private UserListPresenter userPresenter;
	private UserDetailPresenter userDetailPresenter;
	private SubscriptionListPresenter subscriptionPresenter;
	
	
	public RefreshAllClickHandler(QueueListPresenter queuePresenter) {
		super();
		this.queuePresenter = queuePresenter;
	}
	
	public RefreshAllClickHandler(TopicListPresenter topicPresenter) {
		super();
		this.topicPresenter = topicPresenter;
	}
	
	public RefreshAllClickHandler(QueueDetailPresenter queueDetailPresenter) {
		super();
		this.queueDetailPresenter = queueDetailPresenter;
	}
	
	public RefreshAllClickHandler(UserListPresenter userPresenter) {
		super();
		this.userPresenter = userPresenter;
	}
	
	public RefreshAllClickHandler(SubscriptionListPresenter subscriptionPresenter) {
		super();
		this.subscriptionPresenter = subscriptionPresenter;
	}
	
	public RefreshAllClickHandler(UserDetailPresenter userDetailPresenter) {
		super();
		this.userDetailPresenter = userDetailPresenter;
	}
	
	@Override
	public void onClick(ClickEvent event) {

		if(queuePresenter!=null) queuePresenter.fireRefreshAll();
		if(queueDetailPresenter!=null) queueDetailPresenter.fireRefreshAll();
		if(topicPresenter!=null) topicPresenter.fireRefreshAll();
		if(queueDetailPresenter!=null) queueDetailPresenter.fireRefreshAll();
		if(userPresenter!=null) userPresenter.fireRefreshAll();
		if(userDetailPresenter!=null) userDetailPresenter.fireRefreshAll();
		if(subscriptionPresenter!=null) subscriptionPresenter.fireRefreshAll();
	}
	
	


}
