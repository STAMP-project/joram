/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2010 ScalAgent Distributed Technologies
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
package com.scalagent.appli.client.widget;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.scalagent.appli.client.presenter.SubscriptionDetailPresenter;
import com.scalagent.appli.client.widget.handler.message.MessageEditClickHandler;
import com.scalagent.appli.client.widget.handler.message.NewMessageClickHandler;
import com.scalagent.appli.client.widget.handler.queue.RefreshAllClickHandler;
import com.scalagent.appli.client.widget.record.MessageListRecord;
import com.scalagent.appli.client.widget.record.SubscriptionListRecord;
import com.scalagent.appli.shared.MessageWTO;
import com.scalagent.appli.shared.SubscriptionWTO;
import com.scalagent.engine.client.widget.BaseWidget;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RecordList;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.events.DrawEvent;
import com.smartgwt.client.widgets.events.DrawHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.validator.MaskValidator;
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

/**
 * @author Yohann CINTRE
 */
public class SubscriptionDetailWidget extends BaseWidget<SubscriptionDetailPresenter> {

  boolean redrawChart = false;

  int chartWidth;

  boolean showDelivered = true;
  boolean showSentDMQ = true;
  boolean showPending = true;

  SectionStack subDetailStack;

  SectionStackSection buttonSection;
  VLayout vl;
  HLayout hl;
  IButton refreshButton;
  IButton newQueueButton;
  HLayout hl2;
  DetailViewer subDetailLeft = new DetailViewer();
  DetailViewer subDetailRight = new DetailViewer();;

  SectionStackSection listStackSection;
  ListGrid messageList;

  SectionStackSection viewSection;
  HLayout queueView;
  DetailViewer messageDetailLeft;
  DetailViewer messageDetailRight;
  VLayout queueChart;

  AnnotatedTimeLine chart;
  DynamicForm columnForm;
  CheckboxItem showDeliveredBox;
  CheckboxItem showSentDMQBox;
  CheckboxItem showPendingBox;

  Window winModal = new Window();

  HashMap<String, String> etat = new HashMap<String, String>();

  public SubscriptionDetailWidget(SubscriptionDetailPresenter subDetailsPresenter) {
    super(subDetailsPresenter);
    etat.put("true", Application.baseMessages.main_true());
    etat.put("false", Application.baseMessages.main_false());
  }

  public IButton getRefreshButton() {
    return refreshButton;
  }

  @Override
  public Widget asWidget() {

    subDetailStack = new SectionStack();
    subDetailStack.setVisibilityMode(VisibilityMode.MULTIPLE);
    subDetailStack.setWidth100();
    subDetailStack.setHeight100();

    refreshButton = new IButton();
    refreshButton.setAutoFit(true);
    refreshButton.setIcon("refresh.gif");
    refreshButton.setTitle(Application.messages.queueWidget_buttonRefresh_title());
    refreshButton.setPrompt(Application.messages.queueWidget_buttonRefresh_prompt());
    refreshButton.addClickHandler(new RefreshAllClickHandler(presenter));

    newQueueButton = new IButton();
    newQueueButton.setMargin(0);
    newQueueButton.setAutoFit(true);
    newQueueButton.setIcon("new.png");
    newQueueButton.setTitle(Application.messages.queueDetailWidget_buttonNewMessage_title());
    newQueueButton.setPrompt(Application.messages.queueDetailWidget_buttonNewMessage_prompt());
    newQueueButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        drawForm(null);
      }
    });

    hl = new HLayout();
    hl.setHeight(22);
    hl.setPadding(5);
    hl.setMembersMargin(5);
    hl.addMember(refreshButton);
    hl.addMember(newQueueButton);

    DetailViewerField nameFieldD = new DetailViewerField(SubscriptionListRecord.ATTRIBUTE_NAME,
        Application.messages.subscriptionWidget_nameFieldD_title());
    DetailViewerField activeFieldD = new DetailViewerField(SubscriptionListRecord.ATTRIBUTE_ACTIVE,
        Application.messages.subscriptionWidget_activeFieldD_title());
    DetailViewerField nbMaxMsgFieldD = new DetailViewerField(SubscriptionListRecord.ATTRIBUTE_NBMAXMSG,
        Application.messages.subscriptionWidget_nbMaxMsgsFieldD_title());
    DetailViewerField contextIDFieldD = new DetailViewerField(SubscriptionListRecord.ATTRIBUTE_CONTEXTID,
        Application.messages.subscriptionWidget_contextIdFieldD_title());
    DetailViewerField nbMsgsDeliveredSinceCreationFieldD = new DetailViewerField(
        SubscriptionListRecord.ATTRIBUTE_NBMSGSDELIVEREDSINCECREATION,
        Application.messages.subscriptionWidget_msgsDeliveredFieldD_title());
    DetailViewerField nbMsgsSentToDMQSinceCreationFieldD = new DetailViewerField(
        SubscriptionListRecord.ATTRIBUTE_NBMSGSSENTTODMQSINCECREATION,
        Application.messages.subscriptionWidget_msgsSentFieldD_title());
    DetailViewerField pendingMessageCountFieldD = new DetailViewerField(
        SubscriptionListRecord.ATTRIBUTE_PENDINGMESSAGECOUNT,
        Application.messages.subscriptionWidget_pendingFieldD_title());
    DetailViewerField selectorFieldD = new DetailViewerField(SubscriptionListRecord.ATTRIBUTE_SELECTOR,
        Application.messages.subscriptionWidget_selectorFieldD_title());
    DetailViewerField subRequestIdFieldD = new DetailViewerField(
        SubscriptionListRecord.ATTRIBUTE_SUBREQUESTID,
        Application.messages.subscriptionWidget_subRequestFieldD_title());

    subDetailLeft = new DetailViewer();
    subDetailLeft.setMargin(2);
    subDetailLeft.setWidth("25%");
    subDetailLeft.setFields(nameFieldD, activeFieldD, nbMaxMsgFieldD, contextIDFieldD,
        nbMsgsDeliveredSinceCreationFieldD);

    subDetailRight = new DetailViewer();
    subDetailRight.setMargin(2);
    subDetailRight.setWidth("25%");
    subDetailRight.setFields(nbMsgsSentToDMQSinceCreationFieldD, pendingMessageCountFieldD, selectorFieldD,
        subRequestIdFieldD);

    subDetailLeft.setData(new Record[] { new SubscriptionListRecord(presenter.getSubscription()) });
    subDetailRight.setData(new Record[] { new SubscriptionListRecord(presenter.getSubscription()) });

    chartWidth = (com.google.gwt.user.client.Window.getClientWidth() / 2) - 35;
    chart = new AnnotatedTimeLine(createTable(), createOptions(true), "" + chartWidth, "170");

    columnForm = new DynamicForm();
    columnForm.setNumCols(8);

    showDeliveredBox = new CheckboxItem();
    showDeliveredBox.setTitle(Application.messages.common_delivered());
    showDeliveredBox.setValue(true);
    showDeliveredBox.addChangedHandler(new ChangedHandler() {
      public void onChanged(ChangedEvent event) {

        showDelivered = showDeliveredBox.getValueAsBoolean();
        enableDisableCheckbox();
        redrawChart(false);
      }
    });

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

    showPendingBox = new CheckboxItem();
    showPendingBox.setTitle(Application.messages.common_pending());
    showPendingBox.setValue(true);
    showPendingBox.addChangedHandler(new ChangedHandler() {
      public void onChanged(ChangedEvent event) {
        showPending = showPendingBox.getValueAsBoolean();
        enableDisableCheckbox();
        redrawChart(false);
      }
    });

    columnForm.setFields(showDeliveredBox, showSentDMQBox, showPendingBox);

    queueChart = new VLayout();
    queueChart.setMargin(2);
    queueChart.setPadding(5);
    queueChart.setWidth("50%");
    queueChart.setHeight(175);
    queueChart.setAlign(Alignment.CENTER);
    queueChart.setAlign(VerticalAlignment.TOP);
    queueChart.setShowEdges(true);
    queueChart.setEdgeSize(1);
    queueChart.addMember(columnForm);
    queueChart.addMember(chart);
    queueChart.addDrawHandler(new DrawHandler() {
      public void onDraw(DrawEvent event) {
        redrawChart = true;
      }
    });

    hl2 = new HLayout();
    hl2.setMargin(2);
    hl2.setPadding(2);
    hl2.addMember(subDetailLeft);
    hl2.addMember(subDetailRight);
    hl2.addMember(queueChart);

    vl = new VLayout();
    vl.setPadding(0);
    vl.addMember(hl);
    vl.addMember(hl2);

    buttonSection = new SectionStackSection(
        Application.messages.subscriptionDetailWidget_subscriptionDetailsSection_title());
    buttonSection.setExpanded(true);
    buttonSection.addItem(vl);

    // Liste

    messageList = new ListGrid() {

      @Override
      protected Canvas createRecordComponent(final ListGridRecord record, Integer colNum) {

        String fieldName = this.getFieldName(colNum);

        if (fieldName.equals("deleteField")) {

          IButton buttonDelete = new IButton();
          buttonDelete.setAutoFit(true);
          buttonDelete.setHeight(20);
          buttonDelete.setIcon("remove.png");
          buttonDelete.setTitle(Application.messages.queueDetailWidget_buttonDelete_title());
          buttonDelete.setPrompt(Application.messages.queueDetailWidget_buttonDelete_prompt());
          //					buttonDelete.addClickHandler(new MessageDeleteClickHandler(presenter, (MessageListRecord) record));

          return buttonDelete;

        } else {
          return null;
        }
      }
    };

    messageList.setRecordComponentPoolingMode("viewport");
    messageList.setAlternateRecordStyles(true);
    messageList.setShowRecordComponents(true);
    messageList.setShowRecordComponentsByCell(true);

    ListGridField idSFieldL = new ListGridField(MessageListRecord.ATTRIBUTE_IDS,
        Application.messages.queueDetailWidget_idFieldL_title());
    ListGridField deliverycountFieldL = new ListGridField(MessageListRecord.ATTRIBUTE_DELIVERYCOUNT,
        Application.messages.queueDetailWidget_deliveyCountFieldL_title());
    ListGridField priorityFieldL = new ListGridField(MessageListRecord.ATTRIBUTE_PRIORITY,
        Application.messages.queueDetailWidget_priorityFieldL_title());
    ListGridField typeFieldL = new ListGridField(MessageListRecord.ATTRIBUTE_TYPE,
        Application.messages.queueDetailWidget_typeFieldL_title());
    ListGridField deleteField = new ListGridField("deleteField",
        Application.messages.queueDetailWidget_deleteFieldL_title(), 114);
    deleteField.setAlign(Alignment.CENTER);

    messageList.setFields(idSFieldL, deliverycountFieldL, priorityFieldL, typeFieldL, deleteField);
    messageList.addRecordClickHandler(new RecordClickHandler() {
      public void onRecordClick(RecordClickEvent event) {
        messageDetailLeft.setData(new Record[] { event.getRecord() });
        messageDetailRight.setData(new Record[] { event.getRecord() });
      }
    });

    DetailViewerField idSFieldD = new DetailViewerField(MessageListRecord.ATTRIBUTE_IDS,
        Application.messages.queueDetailWidget_idFieldD_title());
    DetailViewerField expirationFieldD = new DetailViewerField(MessageListRecord.ATTRIBUTE_EXPIRATION,
        Application.messages.queueDetailWidget_expirationFieldD_title());
    DetailViewerField timestampFieldD = new DetailViewerField(MessageListRecord.ATTRIBUTE_TIMESTAMP,
        Application.messages.queueDetailWidget_timestampFieldD_title());
    DetailViewerField deliverycountFieldD = new DetailViewerField(MessageListRecord.ATTRIBUTE_DELIVERYCOUNT,
        Application.messages.queueDetailWidget_deliveyCountFieldD_title());
    DetailViewerField priorityFieldD = new DetailViewerField(MessageListRecord.ATTRIBUTE_PRIORITY,
        Application.messages.queueDetailWidget_priorityFieldD_title());
    DetailViewerField typeFieldD = new DetailViewerField(MessageListRecord.ATTRIBUTE_TYPE,
        Application.messages.queueDetailWidget_typeFieldD_title());
    DetailViewerField textFieldD = new DetailViewerField(MessageListRecord.ATTRIBUTE_TEXT,
        Application.messages.queueDetailWidget_textFieldD_title());
    DetailViewerField propertiesFieldD = new DetailViewerField(MessageListRecord.ATTRIBUTE_PROPERTIES,
        Application.messages.queueDetailWidget_propertiesFieldD_title());

    messageDetailLeft = new DetailViewer();
    messageDetailLeft.setMargin(2);
    messageDetailLeft.setWidth("50%");
    messageDetailLeft.setEmptyMessage(Application.messages
        .subscriptionDetailWidget_messageDetail_emptyMessage());
    messageDetailLeft.setFields(idSFieldD, expirationFieldD, timestampFieldD, deliverycountFieldD,
        priorityFieldD);

    messageDetailRight = new DetailViewer();
    messageDetailRight.setMargin(2);
    messageDetailRight.setWidth("50%");
    messageDetailRight.setEmptyMessage(Application.messages
        .subscriptionDetailWidget_messageDetail_emptyMessage());
    messageDetailRight.setFields(typeFieldD, textFieldD, propertiesFieldD);

    queueView = new HLayout();
    queueView.setMargin(0);
    queueView.setPadding(2);
    queueView.addMember(messageDetailLeft);
    queueView.addMember(messageDetailRight);

    // Section stack of the queue list
    listStackSection = new SectionStackSection(
        Application.messages.subscriptionDetailWidget_messagesSection_title());
    listStackSection.setExpanded(true);
    listStackSection.addItem(messageList);

    // Section stack of the view (details & buttons)
    viewSection = new SectionStackSection(
        Application.messages.subscriptionDetailWidget_messageDetailsSection_title());
    viewSection.setExpanded(true);
    viewSection.addItem(queueView);
    viewSection.setCanReorder(true);

    subDetailStack.addSection(buttonSection);
    subDetailStack.addSection(listStackSection);
    subDetailStack.addSection(viewSection);
    subDetailStack.setCanResizeSections(true);

    return subDetailStack;

  }

  public void setData(List<MessageWTO> data) {

    MessageListRecord[] messageListRecord = new MessageListRecord[data.size()];
    for (int i = 0; i < data.size(); i++) {
      messageListRecord[i] = new MessageListRecord(data.get(i));
    }

    messageList.setData(messageListRecord);
  }

  public void updateMessage(MessageWTO message) {
    MessageListRecord messageListRecords = (MessageListRecord) messageList.getRecordList().find(
        MessageListRecord.ATTRIBUTE_IDS, message.getIdS());
    if (messageListRecords != null) {

      messageListRecords.setIdS(message.getIdS());
      messageListRecords.setExpiration(message.getExpiration());
      messageListRecords.setTimestamp(message.getTimestamp());
      messageListRecords.setDeliveryCount(message.getDeliveryCount());
      messageListRecords.setPriority(message.getPriority());
      messageListRecords.setText(message.getText());
      messageListRecords.setType(message.getType());

      messageListRecords.setMessage(message);
      messageList.markForRedraw();

    }

    messageDetailLeft.setData(new Record[] { messageList.getSelectedRecord() });
    messageDetailRight.setData(new Record[] { messageList.getSelectedRecord() });

  }

  public void addMessage(MessageListRecord message) {
    messageList.addData(message);
    messageList.markForRedraw();
  }

  public void removeMessage(MessageListRecord message) {
    RecordList list = messageList.getDataAsRecordList();
    MessageListRecord toRemove = (MessageListRecord) list.find(MessageListRecord.ATTRIBUTE_IDS,
        message.getIdS());
    if (toRemove != null)
      messageList.removeData(toRemove);
    messageList.markForRedraw();
  }

  public void updateQueue() {
    subDetailLeft.setData(new Record[] { new SubscriptionListRecord(presenter.getSubscription()) });
    subDetailRight.setData(new Record[] { new SubscriptionListRecord(presenter.getSubscription()) });
  }

  public void redrawChart(boolean reuseChart) {
    if (redrawChart) {
      chart.draw(createTable(), createOptions(reuseChart));
    }
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

  private AbstractDataTable createTable() {
    DataTable data = DataTable.create();

    data.addColumn(ColumnType.DATETIME, Application.messages.common_time());
    if (showPending)
      data.addColumn(ColumnType.NUMBER, Application.messages.common_pending());
    if (showDelivered)
      data.addColumn(ColumnType.NUMBER, Application.messages.common_delivered());
    if (showSentDMQ)
      data.addColumn(ColumnType.NUMBER, Application.messages.common_sentDMQ());

    SortedMap<Date, int[]> history = presenter.getSubHistory();
    if (history != null) {
      data.addRows(history.size());

      int i = 0;
      for (Date d : history.keySet()) {
        if (d != null) {
          int j = 1;
          data.setValue(i, 0, d);
          if (showPending) {
            data.setValue(i, j, history.get(d)[0]);
            j++;
          }
          if (showDelivered) {
            data.setValue(i, j, history.get(d)[1]);
            j++;
          }
          if (showSentDMQ) {
            data.setValue(i, j, history.get(d)[2]);
            j++;
          }

          i++;
          j = 1;
        }
      }
    }

    return data;
  }

  public void stopChart() {
    redrawChart = false;
  }

  private void enableDisableCheckbox() {
    if (!showSentDMQ && !showPending) {
      showDeliveredBox.disable();
    } else if (!showDelivered && !showPending) {
      showSentDMQBox.disable();
    } else if (!showDelivered && !showSentDMQ) {
      showPendingBox.disable();
    } else {
      showDeliveredBox.enable();
      showSentDMQBox.enable();
      showPendingBox.enable();
    }
  }

  private void drawForm(MessageListRecord mlr) {

    winModal = new Window();
    winModal.setHeight(350);
    winModal.setWidth(400);
    if (mlr == null)
      winModal.setTitle(Application.messages.queueDetailWidget_winModal_title());
    else
      winModal.setTitle("Message \"" + mlr.getAttributeAsString(MessageListRecord.ATTRIBUTE_IDS) + "\"");
    winModal.setShowMinimizeButton(false);
    winModal.setIsModal(true);
    winModal.setShowModalMask(true);
    winModal.centerInPage();
    winModal.addCloseClickHandler(new CloseClickHandler() {
      public void onCloseClick(CloseClientEvent event) {
        winModal.destroy();
      }
    });

    Label formTitle = new Label();
    if (mlr == null)
      formTitle.setContents(Application.messages.queueDetailWidget_formTitle_title());
    else
      formTitle.setContents("Edit \"" + mlr.getAttributeAsString(MessageListRecord.ATTRIBUTE_IDS) + "\"");
    formTitle.setWidth100();
    formTitle.setAutoHeight();
    formTitle.setMargin(5);
    formTitle.setStyleName("title2");
    formTitle.setLayoutAlign(VerticalAlignment.TOP);
    formTitle.setLayoutAlign(Alignment.CENTER);

    final DynamicForm form = new DynamicForm();
    form.setWidth100();
    form.setPadding(5);
    form.setMargin(10);
    form.setLayoutAlign(VerticalAlignment.TOP);
    form.setLayoutAlign(Alignment.CENTER);

    MaskValidator integerValidator = new MaskValidator();
    integerValidator.setMask("^-?[0-9]*$");

    Map<String, SubscriptionWTO> mapsubscriptions = presenter.getSubscriptions();
    LinkedHashMap<String, String> mapNames = new LinkedHashMap<String, String>();

    for (String name : mapsubscriptions.keySet()) {
      mapNames.put(name, name);
    }

    SelectItem queueNameItem = new SelectItem();
    queueNameItem.setTitle(Application.messages.queueDetailWidget_queueNameItem_title());
    queueNameItem.setName("queueNameItem");
    queueNameItem.setRequired(true);
    queueNameItem.setValueMap(mapNames);
    queueNameItem.setRequired(true);
    queueNameItem.setDefaultValue(presenter.getSubscription().getName());

    TextItem idItem = new TextItem();
    idItem.setTitle(Application.messages.queueDetailWidget_idItem_title());
    idItem.setName("idItem");
    idItem.setRequired(true);

    TextItem expirationItem = new TextItem();
    expirationItem.setTitle(Application.messages.queueDetailWidget_expirationItem_title());
    expirationItem.setName("expirationItem");
    expirationItem.setRequired(true);
    expirationItem.setValidators(integerValidator);

    TextItem timestampItem = new TextItem();
    timestampItem.setTitle(Application.messages.queueDetailWidget_timestampItem_title());
    timestampItem.setName("timestampItem");
    timestampItem.setRequired(true);
    timestampItem.setValidators(integerValidator);

    TextItem priorityItem = new TextItem();
    priorityItem.setTitle(Application.messages.queueDetailWidget_priorityItem_title());
    priorityItem.setName("priorityItem");
    priorityItem.setRequired(true);
    priorityItem.setValidators(integerValidator);

    TextItem textItem = new TextItem();
    textItem.setTitle(Application.messages.queueDetailWidget_textItem_title());
    textItem.setName("textItem");
    textItem.setRequired(true);

    TextItem typeItem = new TextItem();
    typeItem.setTitle(Application.messages.queueDetailWidget_typeItem_title());
    typeItem.setName("typeItem");
    typeItem.setRequired(true);
    typeItem.setValidators(integerValidator);

    //		queueNameItem.setValue(presenter.getQueue().getName());

    if (mlr != null) {
      queueNameItem.setDisabled(true);
      idItem.setValue(mlr.getAttributeAsString(MessageListRecord.ATTRIBUTE_IDS));
      expirationItem.setValue(mlr.getAttributeAsString(MessageListRecord.ATTRIBUTE_EXPIRATION));
      timestampItem.setValue(mlr.getAttributeAsString(MessageListRecord.ATTRIBUTE_TIMESTAMP));
      priorityItem.setValue(mlr.getAttributeAsString(MessageListRecord.ATTRIBUTE_PRIORITY));
      textItem.setValue(mlr.getAttributeAsString(MessageListRecord.ATTRIBUTE_TEXT));
      typeItem.setValue(mlr.getAttributeAsString(MessageListRecord.ATTRIBUTE_TYPE));
    }

    form.setFields(queueNameItem, idItem, expirationItem, timestampItem, priorityItem, textItem, typeItem);

    IButton validateButton = new IButton();
    if (mlr == null) {
      validateButton.setTitle(Application.messages.queueWidget_validateButton_titleCreate());
      validateButton.setIcon("add.png");
      validateButton.addClickHandler(new NewMessageClickHandler(presenter, form));
    } else {
      validateButton.setTitle(Application.messages.queueWidget_validateButton_titleEdit());
      validateButton.setIcon("accept.png");
      validateButton.addClickHandler(new MessageEditClickHandler(presenter, form));
    }
    validateButton.setAutoFit(true);
    validateButton.setLayoutAlign(VerticalAlignment.TOP);
    validateButton.setLayoutAlign(Alignment.CENTER);

    IButton cancelButton = new IButton();
    cancelButton.setTitle(Application.messages.queueWidget_cancelButton_title());
    cancelButton.setIcon("cancel.png");
    cancelButton.setAutoFit(true);
    cancelButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        destroyForm();
      }
    });
    cancelButton.setLayoutAlign(VerticalAlignment.TOP);
    cancelButton.setLayoutAlign(Alignment.CENTER);

    HLayout hl = new HLayout();
    hl.setWidth100();
    hl.setAlign(Alignment.CENTER);
    hl.setAlign(VerticalAlignment.CENTER);
    hl.setMembersMargin(5);
    hl.addMember(validateButton);
    hl.addMember(cancelButton);

    winModal.addItem(formTitle);
    winModal.addItem(form);
    winModal.addItem(hl);
    winModal.show();

  }

  public void destroyForm() {
    winModal.destroy();
  }
}
