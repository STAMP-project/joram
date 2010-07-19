/**
 * (c)2010 Scalagent Distributed Technologies
 * @author Yohann CINTRE
 */

package com.scalagent.appli.client.widget;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.AbstractDataTable;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.AnnotatedLegendPosition;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.Options;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.WindowMode;
import com.scalagent.appli.client.Application;
import com.scalagent.appli.client.presenter.UserDetailPresenter;
import com.scalagent.appli.client.widget.handler.queue.RefreshAllClickHandler;
import com.scalagent.appli.client.widget.record.SubscriptionListRecord;
import com.scalagent.appli.client.widget.record.UserListRecord;
import com.scalagent.appli.shared.SubscriptionWTO;
import com.scalagent.engine.client.widget.BaseWidget;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RecordList;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.DrawEvent;
import com.smartgwt.client.widgets.events.DrawHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.viewer.DetailViewer;
import com.smartgwt.client.widgets.viewer.DetailViewerField;

public class UserDetailWidget extends BaseWidget<UserDetailPresenter> {

	boolean redrawChart = false;
	AnnotatedTimeLine chartUser;
	AnnotatedTimeLine chartSub;
	int chartUserWidth;
	int chartSubWidth;

	boolean showSentDMQ = true;
	boolean showSubCount = true;

	SectionStack mainStack;

	SectionStackSection headerSection;
	VLayout vlHeader;
	HLayout hlHeader;
	IButton refreshButton;
	HLayout hlHeader2;
	DetailViewer userDetail = new DetailViewer();
	DynamicForm columnForm;
	CheckboxItem showSentDMQBox;
	CheckboxItem showSubCountBox;

	SectionStackSection listSection;
	ListGrid subscriptionList;

	SectionStackSection detailSection;
	HLayout hlDetail;
	DetailViewer subDetailLeft;
	DetailViewer subDetailRight;
	VLayout usrChart;
	VLayout subChart;

	HashMap<String, String> etat=new HashMap<String, String>();

	private boolean active = true;

	public void setActive(boolean active) {
		this.active = active;
	}

	public UserDetailWidget(UserDetailPresenter userDetailPresenter) {
		super(userDetailPresenter);
		etat.put("true", Application.baseMessages.main_true());
		etat.put("false", Application.baseMessages.main_false());
	}

	public IButton getRefreshButton() {
		return refreshButton;
	}

	@Override
	public Widget asWidget() {


		mainStack = new SectionStack();
		mainStack.setVisibilityMode(VisibilityMode.MULTIPLE);
		mainStack.setWidth100();
		mainStack.setHeight100();


		refreshButton = new IButton();  
		refreshButton.setAutoFit(true);
		refreshButton.setIcon("refresh.gif");  
		refreshButton.setTitle(Application.messages.queueWidget_buttonRefresh_title());
		refreshButton.setPrompt(Application.messages.queueWidget_buttonRefresh_prompt());
		refreshButton.addClickHandler(new RefreshAllClickHandler(presenter)); 

		hlHeader = new HLayout();
		hlHeader.setHeight(20);
		hlHeader.setPadding(5);
		hlHeader.addMember(refreshButton);


		DetailViewerField nameFieldD = new DetailViewerField(UserListRecord.ATTRIBUTE_NAME, Application.messages.userWidget_nameFieldL_title());		
		DetailViewerField periodFieldD = new DetailViewerField(UserListRecord.ATTRIBUTE_PERIOD, Application.messages.userWidget_periodFieldL_title());
		DetailViewerField nbMsgsSentToDMQSinceCreationFieldD = new DetailViewerField(UserListRecord.ATTRIBUTE_NBMSGSSENTTODMQSINCECREATION, Application.messages.userWidget_msgsSentFieldL_title());
		DetailViewerField subscriptionNamesFieldD = new DetailViewerField(UserListRecord.ATTRIBUTE_SUBSCRIPTIONNAMES, Application.messages.userWidget_subscriptionFieldL_title());		

		userDetail = new DetailViewer();
		userDetail.setMargin(2);
		userDetail.setWidth("50%");
		userDetail.setFields(nameFieldD, periodFieldD, nbMsgsSentToDMQSinceCreationFieldD, subscriptionNamesFieldD);

		userDetail.setData(new Record[] {new UserListRecord(presenter.getUser())});


		chartUserWidth = (com.google.gwt.user.client.Window.getClientWidth()/2)-35;
		chartUser = new AnnotatedTimeLine(createUserTable(), createOptions(true), ""+chartUserWidth, "170");

		
		columnForm = new DynamicForm();
		columnForm.setNumCols(4);

		showSentDMQBox = new CheckboxItem();  
		showSentDMQBox.setTitle(Application.messages.common_sentDMQ());
		showSentDMQBox.setValue(true);
		showSentDMQBox.addChangedHandler(new ChangedHandler() {
			public void onChanged(ChangedEvent event) {
				showSentDMQ = showSentDMQBox.getValueAsBoolean();
				enableDisableCheckbox();
				redrawChart(false);
			}
		});
		
		showSubCountBox = new CheckboxItem();  
		showSubCountBox.setTitle(Application.messages.common_subscription());
		showSubCountBox.setValue(true);
		showSubCountBox.addChangedHandler(new ChangedHandler() {
			public void onChanged(ChangedEvent event) {
				showSubCount = showSubCountBox.getValueAsBoolean();
				enableDisableCheckbox();
				redrawChart(false);
			}
		});


		columnForm.setFields(showSentDMQBox, showSubCountBox);

		
		
		usrChart = new VLayout();
		usrChart.setMargin(2);
		usrChart.setPadding(5);
		usrChart.setWidth("50%");
		usrChart.setHeight(175);
		usrChart.setAlign(Alignment.CENTER);
		usrChart.setAlign(VerticalAlignment.TOP);
		usrChart.setShowEdges(true);
		usrChart.setEdgeSize(1);
		usrChart.addMember(columnForm);
		usrChart.addMember(chartUser);
		usrChart.addDrawHandler(new DrawHandler() {
			public void onDraw(DrawEvent event) { redrawChart = true; }
		});

		hlHeader2 = new HLayout();
		hlHeader2.setMargin(0);
		hlHeader2.setPadding(2);
		hlHeader2.addMember(userDetail);
		hlHeader2.addMember(usrChart);

		vlHeader = new VLayout();
		vlHeader.setPadding(0);
		vlHeader.addMember(hlHeader);
		vlHeader.addMember(hlHeader2);

		headerSection = new SectionStackSection(Application.messages.userDetailsWidget_userDetailsSection_title());
		headerSection.setExpanded(true);
		headerSection.addItem(vlHeader);

		// Liste


		subscriptionList = new ListGrid() {

			@Override  
			protected Canvas createRecordComponent(final ListGridRecord record, Integer colNum) {  

				String fieldName = this.getFieldName(colNum);  

				if (fieldName.equals("deleteField")) {

					IButton buttonDelete = new IButton();  
					buttonDelete.setAutoFit(true);
					buttonDelete.setHeight(22); 
					buttonDelete.setIcon("remove.png");  
					buttonDelete.setTitle(Application.messages.queueWidget_buttonDelete_title());
					buttonDelete.setPrompt(Application.messages.queueWidget_buttonDelete_prompt());
					buttonDelete.addClickHandler(new ClickHandler() {

						@Override
						public void onClick(ClickEvent event) {
							SC.confirm("titre", "text", null);
						}
					});

					return buttonDelete;

				} else {  
					return null;                     
				}  	   
			}	
		};

		subscriptionList.setRecordComponentPoolingMode("viewport");
		subscriptionList.setAlternateRecordStyles(true);
		subscriptionList.setShowRecordComponents(true);          
		subscriptionList.setShowRecordComponentsByCell(true);
		ListGridField nameFieldL = new ListGridField(SubscriptionListRecord.ATTRIBUTE_NAME, Application.messages.subscriptionWidget_nameFieldL_title());		
		ListGridField activeFieldL = new ListGridField(SubscriptionListRecord.ATTRIBUTE_ACTIVE, Application.messages.subscriptionWidget_activeFieldL_title());		
		ListGridField nbMsgsDeliveredSinceCreationFieldL = new ListGridField(SubscriptionListRecord.ATTRIBUTE_NBMSGSDELIVEREDSINCECREATION, Application.messages.subscriptionWidget_msgsDeliveredFieldL_title());		
		ListGridField nbMsgsSentToDMQSinceCreationFieldL = new ListGridField(SubscriptionListRecord.ATTRIBUTE_NBMSGSSENTTODMQSINCECREATION, Application.messages.subscriptionWidget_msgsSentFieldL_title());		
		ListGridField pendingCountFieldL = new ListGridField(SubscriptionListRecord.ATTRIBUTE_PENDINGMESSAGECOUNT, Application.messages.subscriptionWidget_pendingFieldL_title());		

		subscriptionList.setFields(
				nameFieldL, 
				activeFieldL, 
				nbMsgsDeliveredSinceCreationFieldL, 
				nbMsgsSentToDMQSinceCreationFieldL, 
				pendingCountFieldL);

		subscriptionList.addRecordClickHandler(new RecordClickHandler() {

			@Override
			public void onRecordClick(RecordClickEvent event) {
				subDetailLeft.setData(new Record[]{event.getRecord()});
				subDetailRight.setData(new Record[]{event.getRecord()});
				redrawChart(true);
			}
		});

		DetailViewerField nameFieldDSub = new DetailViewerField(SubscriptionListRecord.ATTRIBUTE_NAME, Application.messages.subscriptionWidget_nameFieldD_title());		
		DetailViewerField activeFieldDSub = new DetailViewerField(SubscriptionListRecord.ATTRIBUTE_ACTIVE, Application.messages.subscriptionWidget_activeFieldD_title());		
		DetailViewerField nbMaxMsgFieldDSub = new DetailViewerField(SubscriptionListRecord.ATTRIBUTE_NBMAXMSG, Application.messages.subscriptionWidget_nbMaxMsgsFieldD_title());		
		DetailViewerField contextIDFieldDSub = new DetailViewerField(SubscriptionListRecord.ATTRIBUTE_CONTEXTID, Application.messages.subscriptionWidget_contextIdFieldD_title());		
		DetailViewerField nbMsgsDeliveredSinceCreationFieldDSub = new DetailViewerField(SubscriptionListRecord.ATTRIBUTE_NBMSGSDELIVEREDSINCECREATION, Application.messages.subscriptionWidget_msgsDeliveredFieldD_title());		
		DetailViewerField nbMsgsSentToDMQSinceCreationFieldDSub = new DetailViewerField(SubscriptionListRecord.ATTRIBUTE_NBMSGSSENTTODMQSINCECREATION, Application.messages.subscriptionWidget_msgsSentFieldD_title());		
		DetailViewerField pendingMessageCountFieldDSub = new DetailViewerField(SubscriptionListRecord.ATTRIBUTE_PENDINGMESSAGECOUNT, Application.messages.subscriptionWidget_pendingFieldD_title());		
		DetailViewerField selectorFieldDSub = new DetailViewerField(SubscriptionListRecord.ATTRIBUTE_SELECTOR, Application.messages.subscriptionWidget_selectorFieldD_title());		
		DetailViewerField subRequestIdFieldDSub = new DetailViewerField(SubscriptionListRecord.ATTRIBUTE_SUBREQUESTID, Application.messages.subscriptionWidget_subRequestFieldD_title());		


		subDetailLeft = new DetailViewer();
		subDetailLeft.setMargin(2);
		subDetailLeft.setWidth("25%");
		subDetailLeft.setEmptyMessage(Application.messages.userDetailWidget_messageDetail_emptyMessage());
		subDetailLeft.setFields(nameFieldDSub, activeFieldDSub, nbMaxMsgFieldDSub, contextIDFieldDSub, nbMsgsDeliveredSinceCreationFieldDSub);

		subDetailRight = new DetailViewer();
		subDetailRight.setMargin(2);
		subDetailRight.setWidth("25%");
		subDetailRight.setEmptyMessage(Application.messages.userDetailWidget_messageDetail_emptyMessage());
		subDetailRight.setFields(nbMsgsSentToDMQSinceCreationFieldDSub, pendingMessageCountFieldDSub, selectorFieldDSub, subRequestIdFieldDSub);

		chartSubWidth = (com.google.gwt.user.client.Window.getClientWidth()/2)-35;
		chartSub = new AnnotatedTimeLine(createSubTable(), createOptions(true), ""+chartSubWidth, "170");

		subChart = new VLayout();
		subChart.setMargin(2);
		subChart.setPadding(5);
		subChart.setWidth("50%");
		subChart.setHeight(175);
		subChart.setAlign(Alignment.CENTER);
		subChart.setAlign(VerticalAlignment.TOP);
		subChart.setShowEdges(true);
		subChart.setEdgeSize(1);
		subChart.addMember(chartSub);


		hlDetail = new HLayout();
		hlDetail.setMargin(0);
		hlDetail.setPadding(2);
		hlDetail.addMember(subDetailLeft);
		hlDetail.addMember(subDetailRight);
		hlDetail.addMember(subChart);


		// Section stack of the queue list
		listSection = new SectionStackSection(Application.messages.userDetailsWidget_subscriptionsSection_title());
		listSection.setExpanded(true);
		listSection.addItem(subscriptionList);


		// Section stack of the view (details & buttons)
		detailSection = new SectionStackSection(Application.messages.userDetailsWidget_subscriptionDetailsSection_title());
		detailSection.setExpanded(true);
		detailSection.addItem(hlDetail);
		detailSection.setCanReorder(true);

		mainStack.addSection(headerSection);
		mainStack.addSection(listSection);
		mainStack.addSection(detailSection);
		mainStack.setCanResizeSections(true);

		return mainStack;

	}

	public void setData(List<SubscriptionWTO> data) {

		SubscriptionListRecord[] subListRecord = new SubscriptionListRecord[data.size()];
		for (int i=0;i<data.size();i++) {
			subListRecord[i] = new SubscriptionListRecord(data.get(i));
		}

		subscriptionList.setData(subListRecord);
	}

	public void updateSubscription(SubscriptionWTO sub) {
		if(active) {

			SubscriptionListRecord subListRecord = (SubscriptionListRecord)subscriptionList.getRecordList().find(SubscriptionListRecord.ATTRIBUTE_NAME, sub.getName());
			if(subListRecord != null)  {

				subListRecord.setSubscription(sub);
				subListRecord.setSubscription(sub);
				subListRecord.setName(sub.getName());
				subListRecord.setActive(sub.isActive());
				subListRecord.setDurable(sub.isDurable());
				subListRecord.setNbMaxMsg(sub.getNbMaxMsg());
				subListRecord.setContextId(sub.getContextId());
				subListRecord.setNbMsgsDeliveredSinceCreation(sub.getNbMsgsDeliveredSinceCreation());
				subListRecord.setNbMsgsSentToDMQSinceCreation((int) sub.getNbMsgsSentToDMQSinceCreation());
				subListRecord.setPendingMessageCount(sub.getPendingMessageCount());
				subListRecord.setSelector(sub.getSelector());
				subListRecord.setSubRequestId(sub.getSubRequestId());

				subscriptionList.markForRedraw();
			}

			// Usefull when a subscription is alerady in the cache but not draw on this tab
			else {
				addSubscription(new SubscriptionListRecord(sub));
			}

			subDetailLeft.setData(new Record[]{subscriptionList.getSelectedRecord()});
			subDetailRight.setData(new Record[]{subscriptionList.getSelectedRecord()});
		}
	}



	public void updateUser() {
		userDetail.setData(new Record[] {new UserListRecord(presenter.getUser())});
	}

	public void addSubscription(SubscriptionListRecord subRec) {
		subscriptionList.addData(subRec);
		subscriptionList.markForRedraw();
	}

	public void removeSubscription(SubscriptionListRecord subRec) {
		RecordList list = subscriptionList.getDataAsRecordList();
		SubscriptionListRecord toRemove = (SubscriptionListRecord)list.find(SubscriptionListRecord.ATTRIBUTE_NAME, subRec.getName());
		if(toRemove!=null)
			subscriptionList.removeData(toRemove);
		subscriptionList.markForRedraw();
	}

	public void enableRefreshButton() {
		if(active)
			refreshButton.enable();
	}


	private Options createOptions(boolean reuseChart) {
		Options options = Options.create();
		options.setDisplayAnnotations(false);
		options.setDisplayAnnotationsFilter(false);
		options.setDisplayZoomButtons(true);
		options.setDisplayRangeSelector(false);
		options.setAllowRedraw(reuseChart);
		options.setDateFormat("dd MMM HH:mm:ss");
		options.setFill(5);
		options.setLegendPosition(AnnotatedLegendPosition.NEW_ROW);
		options.setWindowMode(WindowMode.TRANSPARENT);

		return options;
	}


	private AbstractDataTable createUserTable() {
		DataTable data = DataTable.create();


		data.addColumn(ColumnType.DATETIME, Application.messages.common_time());
		if(showSentDMQ)	data.addColumn(ColumnType.NUMBER, Application.messages.common_sentDMQ()); 
		if(showSubCount) data.addColumn(ColumnType.NUMBER, Application.messages.common_subscription()); 


		SortedMap<Date,int[]> history = presenter.getUserHistory();
		System.out.println(history);
		if(history != null) {
			data.addRows(history.size());

			int i=0;
			for(Date d : history.keySet()) {
				if(d!=null) {
					int j=1;
					data.setValue(i, 0, d);
					if(showSentDMQ) { data.setValue(i, j, history.get(d)[0]); j++; }
					if(showSubCount) { data.setValue(i, j, history.get(d)[1]); j++; }
					i++;
					j=1;
				}
			}
		}
		return data;
	}

	private AbstractDataTable createSubTable() {
		DataTable data = DataTable.create();
		data.addColumn(ColumnType.DATETIME, Application.messages.common_time());
		data.addColumn(ColumnType.NUMBER, Application.messages.common_delivered());
		data.addColumn(ColumnType.NUMBER, Application.messages.common_sentDMQ());
		data.addColumn(ColumnType.NUMBER, Application.messages.common_pending());

		Record selectedRecord = subscriptionList.getSelectedRecord();
		if(selectedRecord != null) {
			SortedMap<Date, int[]> history = presenter.getSubHistory(selectedRecord.getAttributeAsString(SubscriptionListRecord.ATTRIBUTE_NAME));
			if(history != null) {

				data.addRows(history.size());

				int i=0;
				for(Date d : history.keySet()) {
					data.setValue(i, 0, d);
					data.setValue(i, 1, history.get(d)[0]);
					data.setValue(i, 2, history.get(d)[1]);
					data.setValue(i, 3, history.get(d)[2]);
					i++;
				}
			}
		}

		return data;
	}

	public void redrawChart(boolean reuseChart) {
		if(redrawChart) {
			chartUser.draw(createUserTable(), createOptions(reuseChart));
			chartSub.draw(createSubTable(), createOptions(reuseChart));
		}
	}

	public void stopChart() {
		redrawChart = false;
	}

	private void enableDisableCheckbox() {
		if(!showSubCount) {
			showSentDMQBox.disable();
		}
		else if(!showSentDMQ) {
			showSubCountBox.disable();
		}
		else {
			showSentDMQBox.enable();
			showSubCountBox.enable();
		}
	}
}