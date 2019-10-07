package org.objectweb.joram.mom.messages;


public class MessageEncodingTest {
    public static final java.lang.String PROPERTY1 = "property1";

    public static final java.lang.String PROPERTY2 = "property2";

    public static final java.lang.String PROPERTY3 = "property3";

    public static final java.lang.String PROPERTY4 = "property4";

    public static final java.lang.String PROPERTY5 = "property5";

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
        org.objectweb.joram.mom.messages.Message momMsg = new org.objectweb.joram.mom.messages.Message(sharedMsg);
        org.junit.Assert.assertEquals(17899823L, ((long) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getTimestamp())));
        org.junit.Assert.assertEquals(0L, ((long) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getExpiration())));
        org.junit.Assert.assertEquals(131072, ((int) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getEncodableClassId())));
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getJMSCorrelationIDAsBytes());
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getAdminMessage());
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getBody());
        org.junit.Assert.assertTrue(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).isNullBody());
        org.junit.Assert.assertEquals(0, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getBodyLength())));
        org.junit.Assert.assertEquals(-1, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getEncodableClassId())));
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getText());
        org.junit.Assert.assertEquals(40, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getEncodedSize())));
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getObject());
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.messages.Message) (momMsg)).getText());
        org.junit.Assert.assertFalse(((org.objectweb.joram.mom.messages.Message) (momMsg)).isRedelivered());
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getJMSCorrelationIDAsBytes());
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getAdminMessage());
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getBody());
        org.junit.Assert.assertTrue(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).isNullBody());
        org.junit.Assert.assertEquals(0, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getBodyLength())));
        org.junit.Assert.assertEquals(-1, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getEncodableClassId())));
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getText());
        org.junit.Assert.assertEquals(40, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getEncodedSize())));
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getObject());
        org.junit.Assert.assertTrue(((org.objectweb.joram.mom.messages.Message) (momMsg)).isPersistent());
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.messages.Message) (momMsg)).getCorrelationId());
        org.junit.Assert.assertEquals(0, ((int) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getDeliveryCount())));
        org.junit.Assert.assertEquals(100L, ((long) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getDeliveryTime())));
        org.junit.Assert.assertEquals(0L, ((long) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getOrder())));
        org.junit.Assert.assertEquals(0, ((int) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getAcksCounter())));
        org.junit.Assert.assertEquals(0, ((int) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getDurableAcksCounter())));
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getJMSCorrelationIDAsBytes());
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getAdminMessage());
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getBody());
        org.junit.Assert.assertTrue(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).isNullBody());
        org.junit.Assert.assertEquals(0, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getBodyLength())));
        org.junit.Assert.assertEquals(-1, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getEncodableClassId())));
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getText());
        org.junit.Assert.assertEquals(40, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getEncodedSize())));
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getObject());
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.messages.Message) (momMsg)).getClientID());
        org.junit.Assert.assertEquals(49, ((int) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getEncodedSize())));
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.messages.Message) (momMsg)).getProperties());
        org.junit.Assert.assertEquals(4, ((int) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getPriority())));
        org.junit.Assert.assertEquals("msgId", ((org.objectweb.joram.mom.messages.Message) (momMsg)).getId());
        org.junit.Assert.assertEquals(0, ((int) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getType())));
        sharedMsg.toName = "toName";
        org.junit.Assert.assertEquals("toName", sharedMsg.toName);
        sharedMsg.body = new byte[1000];
        sharedMsg.clientID = "clientId";
        org.junit.Assert.assertEquals("clientId", sharedMsg.clientID);
        momMsg.order = 999;
        checkEncoding(momMsg);
        sharedMsg.toName = null;
        org.junit.Assert.assertNull(sharedMsg.toName);
        sharedMsg.body = null;
        sharedMsg.clientID = null;
        org.junit.Assert.assertNull(sharedMsg.clientID);
        checkEncoding(momMsg);
        sharedMsg.type = org.objectweb.joram.shared.messages.Message.TEXT;
        sharedMsg.replyToId = "replyToId";
        org.junit.Assert.assertEquals("replyToId", sharedMsg.replyToId);
        sharedMsg.replyToName = "replyToName";
        org.junit.Assert.assertEquals("replyToName", sharedMsg.replyToName);
        sharedMsg.replyToType = 1;
        sharedMsg.priority = 10;
        sharedMsg.expiration = 150;
        sharedMsg.correlationId = "correlationId";
        org.junit.Assert.assertEquals("correlationId", sharedMsg.correlationId);
        sharedMsg.deliveryCount = 4;
        sharedMsg.jmsType = "jmsType";
        org.junit.Assert.assertEquals("jmsType", sharedMsg.jmsType);
        sharedMsg.redelivered = true;
        sharedMsg.persistent = true;
        checkEncoding(momMsg);
        fr.dyade.aaa.common.stream.Properties properties = new fr.dyade.aaa.common.stream.Properties();
        java.lang.Object o_run__41 = properties.put(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY1, "propValue1");
        org.junit.Assert.assertNull(o_run__41);
        java.lang.Object o_run__42 = properties.put(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY2, new java.lang.Integer(1));
        org.junit.Assert.assertNull(o_run__42);
        java.lang.Object o_run__44 = properties.put(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY3, new java.lang.Long(1));
        org.junit.Assert.assertNull(o_run__44);
        java.lang.Object o_run__46 = properties.put(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY4, java.lang.Boolean.TRUE);
        org.junit.Assert.assertNull(o_run__46);
        java.lang.Object o_run__47 = properties.put(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY5, "propValue5");
        org.junit.Assert.assertNull(o_run__47);
        sharedMsg.properties = properties;
        checkEncoding(momMsg);
        org.junit.Assert.assertEquals("msgId", sharedMsg.id);
        org.junit.Assert.assertEquals("toId", sharedMsg.toId);
        org.junit.Assert.assertNull(sharedMsg.toName);
        org.junit.Assert.assertNull(sharedMsg.clientID);
        org.junit.Assert.assertEquals(17899823L, ((long) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getTimestamp())));
        org.junit.Assert.assertEquals(150L, ((long) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getExpiration())));
        org.junit.Assert.assertEquals(131072, ((int) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getEncodableClassId())));
        org.junit.Assert.assertArrayEquals(new byte[]{99,111,114,114,101,108,97,116,105,111,110,73,100}, ((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getJMSCorrelationIDAsBytes());
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getAdminMessage());
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getBody());
        org.junit.Assert.assertTrue(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).isNullBody());
        org.junit.Assert.assertEquals(0, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getBodyLength())));
        org.junit.Assert.assertEquals(-1, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getEncodableClassId())));
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getText());
        org.junit.Assert.assertEquals(229, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getEncodedSize())));
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getFullMessage())).getObject());
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.messages.Message) (momMsg)).getText());
        org.junit.Assert.assertTrue(((org.objectweb.joram.mom.messages.Message) (momMsg)).isRedelivered());
        org.junit.Assert.assertArrayEquals(new byte[]{99,111,114,114,101,108,97,116,105,111,110,73,100}, ((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getJMSCorrelationIDAsBytes());
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getAdminMessage());
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getBody());
        org.junit.Assert.assertTrue(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).isNullBody());
        org.junit.Assert.assertEquals(0, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getBodyLength())));
        org.junit.Assert.assertEquals(-1, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getEncodableClassId())));
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getText());
        org.junit.Assert.assertEquals(229, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getEncodedSize())));
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getHeaderMessage())).getObject());
        org.junit.Assert.assertTrue(((org.objectweb.joram.mom.messages.Message) (momMsg)).isPersistent());
        org.junit.Assert.assertEquals("correlationId", ((org.objectweb.joram.mom.messages.Message) (momMsg)).getCorrelationId());
        org.junit.Assert.assertEquals(4, ((int) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getDeliveryCount())));
        org.junit.Assert.assertEquals(100L, ((long) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getDeliveryTime())));
        org.junit.Assert.assertEquals(999L, ((long) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getOrder())));
        org.junit.Assert.assertEquals(0, ((int) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getAcksCounter())));
        org.junit.Assert.assertEquals(0, ((int) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getDurableAcksCounter())));
        org.junit.Assert.assertArrayEquals(new byte[]{99,111,114,114,101,108,97,116,105,111,110,73,100}, ((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getJMSCorrelationIDAsBytes());
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getAdminMessage());
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getBody());
        org.junit.Assert.assertTrue(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).isNullBody());
        org.junit.Assert.assertEquals(0, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getBodyLength())));
        org.junit.Assert.assertEquals(-1, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getEncodableClassId())));
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getText());
        org.junit.Assert.assertEquals(229, ((int) (((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getEncodedSize())));
        org.junit.Assert.assertNull(((org.objectweb.joram.shared.messages.Message) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getMsg())).getObject());
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.messages.Message) (momMsg)).getClientID());
        org.junit.Assert.assertEquals(238, ((int) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getEncodedSize())));
        org.junit.Assert.assertTrue(((org.objectweb.joram.mom.messages.Message)momMsg).getProperties().containsKey("property2"));
        org.junit.Assert.assertEquals("1", ((org.objectweb.joram.mom.messages.Message)momMsg).getProperties().get("property2"));
        org.junit.Assert.assertTrue(((org.objectweb.joram.mom.messages.Message)momMsg).getProperties().containsKey("property1"));
        org.junit.Assert.assertEquals("propValue1", ((org.objectweb.joram.mom.messages.Message)momMsg).getProperties().get("property1"));
        org.junit.Assert.assertTrue(((org.objectweb.joram.mom.messages.Message)momMsg).getProperties().containsKey("property4"));
        org.junit.Assert.assertEquals("true", ((org.objectweb.joram.mom.messages.Message)momMsg).getProperties().get("property4"));
        org.junit.Assert.assertTrue(((org.objectweb.joram.mom.messages.Message)momMsg).getProperties().containsKey("property3"));
        org.junit.Assert.assertEquals("1", ((org.objectweb.joram.mom.messages.Message)momMsg).getProperties().get("property3"));
        org.junit.Assert.assertTrue(((org.objectweb.joram.mom.messages.Message)momMsg).getProperties().containsKey("property5"));
        org.junit.Assert.assertEquals("propValue5", ((org.objectweb.joram.mom.messages.Message)momMsg).getProperties().get("property5"));
        org.junit.Assert.assertEquals(10, ((int) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getPriority())));
        org.junit.Assert.assertEquals("msgId", ((org.objectweb.joram.mom.messages.Message) (momMsg)).getId());
        org.junit.Assert.assertEquals(1, ((int) (((org.objectweb.joram.mom.messages.Message) (momMsg)).getType())));
        org.junit.Assert.assertNull(sharedMsg.toName);
        org.junit.Assert.assertNull(sharedMsg.clientID);
        org.junit.Assert.assertNull(sharedMsg.toName);
        org.junit.Assert.assertNull(sharedMsg.clientID);
        org.junit.Assert.assertEquals("replyToId", sharedMsg.replyToId);
        org.junit.Assert.assertEquals("replyToName", sharedMsg.replyToName);
        org.junit.Assert.assertEquals("correlationId", sharedMsg.correlationId);
        org.junit.Assert.assertEquals("jmsType", sharedMsg.jmsType);
        org.junit.Assert.assertNull(o_run__41);
        org.junit.Assert.assertNull(o_run__42);
        org.junit.Assert.assertNull(o_run__44);
        org.junit.Assert.assertNull(o_run__46);
        org.junit.Assert.assertNull(o_run__47);
    }

    private void checkEncoding(org.objectweb.joram.mom.messages.Message msg) throws java.lang.Exception {
        byte[] bytes = org.objectweb.joram.mom.proxies.EncodingHelper.encode(msg);
        org.objectweb.joram.mom.messages.Message msgDec = ((org.objectweb.joram.mom.messages.Message) (org.objectweb.joram.mom.proxies.EncodingHelper.decode(msg.getEncodableClassId(), bytes)));
        junit.framework.Assert.assertEquals(msg.order, msgDec.order);
        junit.framework.Assert.assertEquals(msg.getFullMessage().id, msgDec.getFullMessage().id);
        junit.framework.Assert.assertEquals(msg.getFullMessage().toId, msgDec.getFullMessage().toId);
        junit.framework.Assert.assertEquals(msg.getFullMessage().toName, msgDec.getFullMessage().toName);
        junit.framework.Assert.assertEquals(msg.getFullMessage().toType, msgDec.getFullMessage().toType);
        junit.framework.Assert.assertEquals(msg.getFullMessage().timestamp, msgDec.getFullMessage().timestamp);
        junit.framework.Assert.assertEquals(msg.getFullMessage().compressed, msgDec.getFullMessage().compressed);
        junit.framework.Assert.assertEquals(msg.getFullMessage().deliveryTime, msgDec.getFullMessage().deliveryTime);
        junit.framework.Assert.assertEquals(msg.getFullMessage().type, msgDec.getFullMessage().type);
        junit.framework.Assert.assertEquals(msg.getFullMessage().replyToId, msgDec.getFullMessage().replyToId);
        junit.framework.Assert.assertEquals(msg.getFullMessage().replyToName, msgDec.getFullMessage().replyToName);
        junit.framework.Assert.assertEquals(msg.getFullMessage().replyToId, msgDec.getFullMessage().replyToId);
        junit.framework.Assert.assertEquals(msg.getFullMessage().priority, msgDec.getFullMessage().priority);
        junit.framework.Assert.assertEquals(msg.getFullMessage().expiration, msgDec.getFullMessage().expiration);
        junit.framework.Assert.assertEquals(msg.getFullMessage().correlationId, msgDec.getFullMessage().correlationId);
        junit.framework.Assert.assertEquals(msg.getFullMessage().deliveryCount, msgDec.getFullMessage().deliveryCount);
        junit.framework.Assert.assertEquals(msg.getFullMessage().jmsType, msgDec.getFullMessage().jmsType);
        junit.framework.Assert.assertEquals(msg.getFullMessage().redelivered, msgDec.getFullMessage().redelivered);
        junit.framework.Assert.assertEquals(msg.getFullMessage().persistent, msgDec.getFullMessage().persistent);
        if ((msg.getFullMessage().properties) != null) {
            junit.framework.Assert.assertEquals(msg.getFullMessage().properties.size(), msgDec.getFullMessage().properties.size());
            junit.framework.Assert.assertEquals(msg.getFullMessage().properties.get(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY1), msgDec.getFullMessage().properties.get(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY1));
            junit.framework.Assert.assertEquals(msg.getFullMessage().properties.get(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY2), msgDec.getFullMessage().properties.get(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY2));
            junit.framework.Assert.assertEquals(msg.getFullMessage().properties.get(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY3), msgDec.getFullMessage().properties.get(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY3));
            junit.framework.Assert.assertEquals(msg.getFullMessage().properties.get(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY4), msgDec.getFullMessage().properties.get(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY4));
            junit.framework.Assert.assertEquals(msg.getFullMessage().properties.get(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY5), msgDec.getFullMessage().properties.get(org.objectweb.joram.mom.messages.MessageEncodingTest.PROPERTY5));
        }
        if ((msg.getFullMessage().body) != null) {
            junit.framework.Assert.assertEquals(msg.getFullMessage().body.length, msgDec.getFullMessage().body.length);
        } else {
            junit.framework.Assert.assertEquals(null, msgDec.getFullMessage().body);
        }
        junit.framework.Assert.assertEquals(msg.getFullMessage().clientID, msgDec.getFullMessage().clientID);
    }
}

