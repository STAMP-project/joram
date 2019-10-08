package org.objectweb.joram.mom.notifications;


import fr.dyade.aaa.agent.AgentId;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.joram.mom.proxies.EncodingHelper;
import org.objectweb.joram.shared.messages.Message;

import static junit.framework.Assert.assertEquals;


public class TopicForwardNotTest {
    public static final String PROPERTY1 = "property1";

    public static final String PROPERTY2 = "property2";

    public static final String PROPERTY3 = "property3";

    public static final String PROPERTY4 = "property4";

    @Test(timeout = 10000)
    public void run() throws Exception {
        EncodingHelper.init();
        Message sharedMsg = new Message();
        sharedMsg.id = "msgId";
        Assert.assertEquals("msgId", sharedMsg.id);
        sharedMsg.toId = "toId";
        Assert.assertEquals("toId", sharedMsg.toId);
        sharedMsg.toName = null;
        Assert.assertNull(sharedMsg.toName);
        sharedMsg.toType = 1;
        sharedMsg.timestamp = 17899823;
        sharedMsg.compressed = true;
        sharedMsg.deliveryTime = 100;
        sharedMsg.body = null;
        sharedMsg.clientID = null;
        Assert.assertNull(sharedMsg.clientID);
        sharedMsg.toName = "toName";
        Assert.assertEquals("toName", sharedMsg.toName);
        sharedMsg.body = new byte[1000];
        sharedMsg.clientID = "clientId";
        Assert.assertEquals("clientId", sharedMsg.clientID);
        ClientMessages clientMessages = new ClientMessages(1, 2);
        Assert.assertEquals(0, ((int) (((ClientMessages) (clientMessages)).getMessageCount())));
        Assert.assertNull(((ClientMessages) (clientMessages)).getDMQId());
        Assert.assertFalse(((ClientMessages) (clientMessages)).getAsyncSend());
        Assert.assertTrue(((Collection) (((ClientMessages) (clientMessages)).getMessages())).isEmpty());
        Assert.assertEquals(131082, ((int) (((ClientMessages) (clientMessages)).getEncodableClassId())));
        Assert.assertNull(((ClientMessages) (clientMessages)).getProxyId());
        Assert.assertEquals(13, ((int) (((ClientMessages) (clientMessages)).getEncodedSize())));
        Assert.assertEquals(2, ((int) (((ClientMessages) (clientMessages)).getRequestId())));
        Assert.assertEquals(1, ((int) (((ClientMessages) (clientMessages)).getClientContext())));
        Assert.assertFalse(((ClientMessages) (clientMessages)).hasCallback());
        Assert.assertEquals(0L, ((long) (((ClientMessages) (clientMessages)).getExpiration())));
        Assert.assertNull(((ClientMessages) (clientMessages)).getMessageId());
        Assert.assertTrue(((ClientMessages) (clientMessages)).isPersistent());
        Assert.assertNull(((ClientMessages) (clientMessages)).getDeadNotificationAgentId());
        Assert.assertEquals(4, ((int) (((ClientMessages) (clientMessages)).getPriority())));
        clientMessages.addMessage(sharedMsg);
        clientMessages.setProxyId(new AgentId(((short) (1)), ((short) (2)), 3));
        TopicForwardNot topicForwardNot = new TopicForwardNot(clientMessages, false);
        Assert.assertEquals(131081, ((int) (((TopicForwardNot) (topicForwardNot)).getEncodableClassId())));
        Assert.assertEquals(1093, ((int) (((TopicForwardNot) (topicForwardNot)).getEncodedSize())));
        Assert.assertFalse(((TopicForwardNot) (topicForwardNot)).hasCallback());
        Assert.assertEquals(0L, ((long) (((TopicForwardNot) (topicForwardNot)).getExpiration())));
        Assert.assertNull(((TopicForwardNot) (topicForwardNot)).getMessageId());
        Assert.assertTrue(((TopicForwardNot) (topicForwardNot)).isPersistent());
        Assert.assertNull(((TopicForwardNot) (topicForwardNot)).getDeadNotificationAgentId());
        Assert.assertEquals(4, ((int) (((TopicForwardNot) (topicForwardNot)).getPriority())));
        checkEncoding(topicForwardNot);
        Assert.assertEquals("msgId", sharedMsg.id);
        Assert.assertEquals("toId", sharedMsg.toId);
        Assert.assertEquals("toName", sharedMsg.toName);
        Assert.assertEquals("clientId", sharedMsg.clientID);
        Assert.assertEquals("toName", sharedMsg.toName);
        Assert.assertEquals("clientId", sharedMsg.clientID);
        Assert.assertEquals(1, ((int) (((ClientMessages) (clientMessages)).getMessageCount())));
        Assert.assertNull(((ClientMessages) (clientMessages)).getDMQId());
        Assert.assertFalse(((ClientMessages) (clientMessages)).getAsyncSend());
        Assert.assertFalse(((Collection) (((ClientMessages) (clientMessages)).getMessages())).isEmpty());
        Assert.assertEquals(131082, ((int) (((ClientMessages) (clientMessages)).getEncodableClassId())));
        Assert.assertEquals(2, ((short) (((AgentId) (((ClientMessages) (clientMessages)).getProxyId())).getTo())));
        Assert.assertEquals(1, ((short) (((AgentId) (((ClientMessages) (clientMessages)).getProxyId())).getFrom())));
        Assert.assertEquals(3, ((int) (((AgentId) (((ClientMessages) (clientMessages)).getProxyId())).getStamp())));
        Assert.assertFalse(((AgentId) (((ClientMessages) (clientMessages)).getProxyId())).isNullId());
        Assert.assertFalse(((AgentId) (((ClientMessages) (clientMessages)).getProxyId())).isLocal());
        Assert.assertEquals(-1, ((int) (((AgentId) (((ClientMessages) (clientMessages)).getProxyId())).getEncodableClassId())));
        Assert.assertEquals(8, ((int) (((AgentId) (((ClientMessages) (clientMessages)).getProxyId())).getEncodedSize())));
        Assert.assertEquals("#1.2.3", ((AgentId) (((ClientMessages) (clientMessages)).getProxyId())).toString());
        Assert.assertEquals(3, ((int) (((AgentId) (((ClientMessages) (clientMessages)).getProxyId())).hashCode())));
        Assert.assertEquals(1087, ((int) (((ClientMessages) (clientMessages)).getEncodedSize())));
        Assert.assertEquals(2, ((int) (((ClientMessages) (clientMessages)).getRequestId())));
        Assert.assertEquals(1, ((int) (((ClientMessages) (clientMessages)).getClientContext())));
        Assert.assertFalse(((ClientMessages) (clientMessages)).hasCallback());
        Assert.assertEquals(0L, ((long) (((ClientMessages) (clientMessages)).getExpiration())));
        Assert.assertNull(((ClientMessages) (clientMessages)).getMessageId());
        Assert.assertTrue(((ClientMessages) (clientMessages)).isPersistent());
        Assert.assertNull(((ClientMessages) (clientMessages)).getDeadNotificationAgentId());
        Assert.assertEquals(4, ((int) (((ClientMessages) (clientMessages)).getPriority())));
        Assert.assertEquals(131081, ((int) (((TopicForwardNot) (topicForwardNot)).getEncodableClassId())));
        Assert.assertEquals(1093, ((int) (((TopicForwardNot) (topicForwardNot)).getEncodedSize())));
        Assert.assertFalse(((TopicForwardNot) (topicForwardNot)).hasCallback());
        Assert.assertEquals(0L, ((long) (((TopicForwardNot) (topicForwardNot)).getExpiration())));
        Assert.assertNull(((TopicForwardNot) (topicForwardNot)).getMessageId());
        Assert.assertTrue(((TopicForwardNot) (topicForwardNot)).isPersistent());
        Assert.assertNull(((TopicForwardNot) (topicForwardNot)).getDeadNotificationAgentId());
        Assert.assertEquals(4, ((int) (((TopicForwardNot) (topicForwardNot)).getPriority())));
    }

    private void checkEncoding(TopicForwardNot topicForwardNot) throws Exception {
        byte[] bytes = EncodingHelper.encode(topicForwardNot);
        TopicForwardNot dec = ((TopicForwardNot) (EncodingHelper.decode(topicForwardNot.getEncodableClassId(), bytes)));
        ClientMessages cm = topicForwardNot.messages;
        junit.framework.Assert.assertEquals(topicForwardNot.fromCluster, dec.fromCluster);
        junit.framework.Assert.assertEquals(topicForwardNot.fromCluster, dec.fromCluster);
        assertEquals(cm.getProxyId(), dec.messages.getProxyId());
    }
}

