package org.objectweb.joram.mom.notifications;


public class TopicForwardNotTest {
    public static final java.lang.String PROPERTY1 = "property1";

    public static final java.lang.String PROPERTY2 = "property2";

    public static final java.lang.String PROPERTY3 = "property3";

    public static final java.lang.String PROPERTY4 = "property4";

    @org.junit.Test(timeout = 10000)
    public void run() throws java.lang.Exception {
        org.objectweb.joram.mom.proxies.EncodingHelper.init();
        org.objectweb.joram.shared.messages.Message sharedMsg = new org.objectweb.joram.shared.messages.Message();
        sharedMsg.id = "msgId";
        org.junit.Assert.assertEquals("msgId", sharedMsg.id);
        sharedMsg.toId = "toId";
        org.junit.Assert.assertEquals("toId", sharedMsg.toId);
        sharedMsg.toName = null;
        org.junit.Assert.assertNull(sharedMsg.toName);
        sharedMsg.toType = 1;
        sharedMsg.timestamp = 17899823;
        sharedMsg.compressed = true;
        sharedMsg.deliveryTime = 100;
        sharedMsg.body = null;
        sharedMsg.clientID = null;
        org.junit.Assert.assertNull(sharedMsg.clientID);
        sharedMsg.toName = "toName";
        org.junit.Assert.assertEquals("toName", sharedMsg.toName);
        sharedMsg.body = new byte[1000];
        sharedMsg.clientID = "clientId";
        org.junit.Assert.assertEquals("clientId", sharedMsg.clientID);
        org.objectweb.joram.mom.notifications.ClientMessages clientMessages = new org.objectweb.joram.mom.notifications.ClientMessages(1, 2);
        org.junit.Assert.assertEquals(0, ((int) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getMessageCount())));
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getDMQId());
        org.junit.Assert.assertFalse(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getAsyncSend());
        org.junit.Assert.assertTrue(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getMessages().isEmpty());
        org.junit.Assert.assertEquals(131082, ((int) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getEncodableClassId())));
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getProxyId());
        org.junit.Assert.assertEquals(13, ((int) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getEncodedSize())));
        org.junit.Assert.assertEquals(2, ((int) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getRequestId())));
        org.junit.Assert.assertEquals(1, ((int) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getClientContext())));
        org.junit.Assert.assertFalse(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).hasCallback());
        org.junit.Assert.assertEquals(0L, ((long) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getExpiration())));
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getMessageId());
        org.junit.Assert.assertTrue(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).isPersistent());
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getDeadNotificationAgentId());
        org.junit.Assert.assertEquals(4, ((int) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getPriority())));
        clientMessages.addMessage(sharedMsg);
        clientMessages.setProxyId(new fr.dyade.aaa.agent.AgentId(((short) (1)), ((short) (2)), 3));
        org.objectweb.joram.mom.notifications.TopicForwardNot topicForwardNot = new org.objectweb.joram.mom.notifications.TopicForwardNot(clientMessages, false);
        org.junit.Assert.assertEquals(131081, ((int) (((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).getEncodableClassId())));
        org.junit.Assert.assertEquals(1093, ((int) (((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).getEncodedSize())));
        org.junit.Assert.assertFalse(((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).hasCallback());
        org.junit.Assert.assertEquals(0L, ((long) (((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).getExpiration())));
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).getMessageId());
        org.junit.Assert.assertTrue(((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).isPersistent());
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).getDeadNotificationAgentId());
        org.junit.Assert.assertEquals(4, ((int) (((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).getPriority())));
        checkEncoding(topicForwardNot);
        org.junit.Assert.assertEquals("msgId", sharedMsg.id);
        org.junit.Assert.assertEquals("toId", sharedMsg.toId);
        org.junit.Assert.assertEquals("toName", sharedMsg.toName);
        org.junit.Assert.assertEquals("clientId", sharedMsg.clientID);
        org.junit.Assert.assertEquals("toName", sharedMsg.toName);
        org.junit.Assert.assertEquals("clientId", sharedMsg.clientID);
        org.junit.Assert.assertEquals(1, ((int) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getMessageCount())));
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getDMQId());
        org.junit.Assert.assertFalse(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getAsyncSend());
        org.junit.Assert.assertFalse(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getMessages().isEmpty());
        org.junit.Assert.assertEquals(131082, ((int) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getEncodableClassId())));
        org.junit.Assert.assertEquals(2, ((short) (((fr.dyade.aaa.agent.AgentId) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getProxyId())).getTo())));
        org.junit.Assert.assertEquals(1, ((short) (((fr.dyade.aaa.agent.AgentId) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getProxyId())).getFrom())));
        org.junit.Assert.assertEquals(3, ((int) (((fr.dyade.aaa.agent.AgentId) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getProxyId())).getStamp())));
        org.junit.Assert.assertFalse(((fr.dyade.aaa.agent.AgentId) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getProxyId())).isNullId());
        org.junit.Assert.assertFalse(((fr.dyade.aaa.agent.AgentId) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getProxyId())).isLocal());
        org.junit.Assert.assertEquals(-1, ((int) (((fr.dyade.aaa.agent.AgentId) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getProxyId())).getEncodableClassId())));
        org.junit.Assert.assertEquals(8, ((int) (((fr.dyade.aaa.agent.AgentId) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getProxyId())).getEncodedSize())));
        org.junit.Assert.assertEquals("#1.2.3", ((fr.dyade.aaa.agent.AgentId) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getProxyId())).toString());
        org.junit.Assert.assertEquals(3, ((int) (((fr.dyade.aaa.agent.AgentId) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getProxyId())).hashCode())));
        org.junit.Assert.assertEquals(1087, ((int) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getEncodedSize())));
        org.junit.Assert.assertEquals(2, ((int) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getRequestId())));
        org.junit.Assert.assertEquals(1, ((int) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getClientContext())));
        org.junit.Assert.assertFalse(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).hasCallback());
        org.junit.Assert.assertEquals(0L, ((long) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getExpiration())));
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getMessageId());
        org.junit.Assert.assertTrue(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).isPersistent());
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getDeadNotificationAgentId());
        org.junit.Assert.assertEquals(4, ((int) (((org.objectweb.joram.mom.notifications.ClientMessages) (clientMessages)).getPriority())));
        org.junit.Assert.assertEquals(131081, ((int) (((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).getEncodableClassId())));
        org.junit.Assert.assertEquals(1093, ((int) (((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).getEncodedSize())));
        org.junit.Assert.assertFalse(((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).hasCallback());
        org.junit.Assert.assertEquals(0L, ((long) (((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).getExpiration())));
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).getMessageId());
        org.junit.Assert.assertTrue(((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).isPersistent());
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).getDeadNotificationAgentId());
        org.junit.Assert.assertEquals(4, ((int) (((org.objectweb.joram.mom.notifications.TopicForwardNot) (topicForwardNot)).getPriority())));
    }

    private void checkEncoding(org.objectweb.joram.mom.notifications.TopicForwardNot topicForwardNot) throws java.lang.Exception {
        byte[] bytes = org.objectweb.joram.mom.proxies.EncodingHelper.encode(topicForwardNot);
        org.objectweb.joram.mom.notifications.TopicForwardNot dec = ((org.objectweb.joram.mom.notifications.TopicForwardNot) (org.objectweb.joram.mom.proxies.EncodingHelper.decode(topicForwardNot.getEncodableClassId(), bytes)));
        org.objectweb.joram.mom.notifications.ClientMessages cm = topicForwardNot.messages;
        junit.framework.Assert.assertEquals(topicForwardNot.fromCluster, dec.fromCluster);
        junit.framework.Assert.assertEquals(topicForwardNot.fromCluster, dec.fromCluster);
        junit.framework.Assert.assertEquals(cm.getProxyId(), dec.messages.getProxyId());
    }
}

