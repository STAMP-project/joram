package org.objectweb.joram.mom.proxies;


import fr.dyade.aaa.agent.AgentId;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.joram.mom.util.MessageTable;

import static junit.framework.Assert.assertEquals;


public class ClientSubscriptionEncodingTest {
    @Test(timeout = 10000)
    public void run() throws Exception {
        EncodingHelper.init();
        AgentId proxyId = new AgentId(((short) (10)), ((short) (20)), 30);
        int contextId = 40;
        int reqId = 50;
        boolean durable = true;
        AgentId topicId = new AgentId(((short) (60)), ((short) (70)), 80);
        String name = "test-subscription";
        Assert.assertEquals("test-subscription", name);
        String selector = "test-selector";
        Assert.assertEquals("test-selector", selector);
        boolean noLocal = true;
        AgentId dmqId = new AgentId(((short) (90)), ((short) (100)), 110);
        int threshold = 120;
        int nbMaxMsg = 130;
        MessageTable messagesTable = null;
        String clientID = null;
        Assert.assertNull(clientID);
        ClientSubscription cs1 = new ClientSubscription(proxyId, contextId, reqId, durable, topicId, name, selector, noLocal, dmqId, threshold, nbMaxMsg, messagesTable, clientID);
        Assert.assertEquals(131075, ((int) (((ClientSubscription) (cs1)).getEncodableClassId())));
        Assert.assertEquals(120, ((int) (((ClientSubscription) (cs1)).getThreshold())));
        Assert.assertEquals(0L, ((long) (((ClientSubscription) (cs1)).getNbMsgsSentToDMQSinceCreation())));
        Assert.assertEquals(0L, ((long) (((ClientSubscription) (cs1)).getNbMsgsDeliveredSinceCreation())));
        Assert.assertEquals(90, ((int) (((ClientSubscription) (cs1)).getEncodedSize())));
        Assert.assertEquals(130, ((int) (((ClientSubscription) (cs1)).getNbMaxMsg())));
        Assert.assertEquals(20, ((short) (((AgentId) (((ClientSubscription) (cs1)).getProxyId())).getTo())));
        Assert.assertEquals(10, ((short) (((AgentId) (((ClientSubscription) (cs1)).getProxyId())).getFrom())));
        Assert.assertEquals(30, ((int) (((AgentId) (((ClientSubscription) (cs1)).getProxyId())).getStamp())));
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs1)).getProxyId())).isNullId());
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs1)).getProxyId())).isLocal());
        Assert.assertEquals(-1, ((int) (((AgentId) (((ClientSubscription) (cs1)).getProxyId())).getEncodableClassId())));
        Assert.assertEquals(8, ((int) (((AgentId) (((ClientSubscription) (cs1)).getProxyId())).getEncodedSize())));
        Assert.assertEquals("#10.20.30", ((AgentId) (((ClientSubscription) (cs1)).getProxyId())).toString());
        Assert.assertEquals(30, ((int) (((AgentId) (((ClientSubscription) (cs1)).getProxyId())).hashCode())));
        Assert.assertEquals(40, ((int) (((ClientSubscription) (cs1)).getContextId())));
        Assert.assertEquals(50, ((int) (((ClientSubscription) (cs1)).getSubRequestId())));
        Assert.assertEquals(70, ((short) (((AgentId) (((ClientSubscription) (cs1)).getTopicId())).getTo())));
        Assert.assertEquals(60, ((short) (((AgentId) (((ClientSubscription) (cs1)).getTopicId())).getFrom())));
        Assert.assertEquals(80, ((int) (((AgentId) (((ClientSubscription) (cs1)).getTopicId())).getStamp())));
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs1)).getTopicId())).isNullId());
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs1)).getTopicId())).isLocal());
        Assert.assertEquals(-1, ((int) (((AgentId) (((ClientSubscription) (cs1)).getTopicId())).getEncodableClassId())));
        Assert.assertEquals(8, ((int) (((AgentId) (((ClientSubscription) (cs1)).getTopicId())).getEncodedSize())));
        Assert.assertEquals("#60.70.80", ((AgentId) (((ClientSubscription) (cs1)).getTopicId())).toString());
        Assert.assertEquals(80, ((int) (((AgentId) (((ClientSubscription) (cs1)).getTopicId())).hashCode())));
        Assert.assertEquals("#60.70.80", ((ClientSubscription) (cs1)).getTopicIdAsString());
        Assert.assertEquals("test-selector", ((ClientSubscription) (cs1)).getSelector());
        Assert.assertTrue(((ClientSubscription) (cs1)).getDurable());
        Assert.assertEquals(100, ((int) (((ClientSubscription) (cs1)).getActive())));
        Assert.assertEquals(0, ((int) (((ClientSubscription) (cs1)).getDeliveredMessageCount())));
        Assert.assertEquals("ClientSubscription#10.20.30test-subscription", ((ClientSubscription) (cs1)).toString());
        Assert.assertEquals("test-subscription", ((ClientSubscription) (cs1)).getName());
        String o_run__23 = cs1.getDeliveredIds().put("msg1", "msg1");
        Assert.assertNull(o_run__23);
        String o_run__25 = cs1.getDeliveredIds().put("msg2", "msg2");
        Assert.assertNull(o_run__25);
        Integer o_run__27 = cs1.getDeniedMsgs().put("msg3", 1);
        Assert.assertNull(o_run__27);
        Integer o_run__29 = cs1.getDeniedMsgs().put("msg4", 2);
        Assert.assertNull(o_run__29);
        checkEncoding(cs1);
        clientID = "clientId";
        Assert.assertEquals("clientId", clientID);
        ClientSubscription cs2 = new ClientSubscription(proxyId, contextId, reqId, durable, topicId, name, selector, noLocal, dmqId, threshold, nbMaxMsg, messagesTable, clientID);
        Assert.assertEquals(131075, ((int) (((ClientSubscription) (cs2)).getEncodableClassId())));
        Assert.assertEquals(120, ((int) (((ClientSubscription) (cs2)).getThreshold())));
        Assert.assertEquals(0L, ((long) (((ClientSubscription) (cs2)).getNbMsgsSentToDMQSinceCreation())));
        Assert.assertEquals(0L, ((long) (((ClientSubscription) (cs2)).getNbMsgsDeliveredSinceCreation())));
        Assert.assertEquals(102, ((int) (((ClientSubscription) (cs2)).getEncodedSize())));
        Assert.assertEquals(130, ((int) (((ClientSubscription) (cs2)).getNbMaxMsg())));
        Assert.assertEquals(20, ((short) (((AgentId) (((ClientSubscription) (cs2)).getProxyId())).getTo())));
        Assert.assertEquals(10, ((short) (((AgentId) (((ClientSubscription) (cs2)).getProxyId())).getFrom())));
        Assert.assertEquals(30, ((int) (((AgentId) (((ClientSubscription) (cs2)).getProxyId())).getStamp())));
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs2)).getProxyId())).isNullId());
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs2)).getProxyId())).isLocal());
        Assert.assertEquals(-1, ((int) (((AgentId) (((ClientSubscription) (cs2)).getProxyId())).getEncodableClassId())));
        Assert.assertEquals(8, ((int) (((AgentId) (((ClientSubscription) (cs2)).getProxyId())).getEncodedSize())));
        Assert.assertEquals("#10.20.30", ((AgentId) (((ClientSubscription) (cs2)).getProxyId())).toString());
        Assert.assertEquals(30, ((int) (((AgentId) (((ClientSubscription) (cs2)).getProxyId())).hashCode())));
        Assert.assertEquals(40, ((int) (((ClientSubscription) (cs2)).getContextId())));
        Assert.assertEquals(50, ((int) (((ClientSubscription) (cs2)).getSubRequestId())));
        Assert.assertEquals(70, ((short) (((AgentId) (((ClientSubscription) (cs2)).getTopicId())).getTo())));
        Assert.assertEquals(60, ((short) (((AgentId) (((ClientSubscription) (cs2)).getTopicId())).getFrom())));
        Assert.assertEquals(80, ((int) (((AgentId) (((ClientSubscription) (cs2)).getTopicId())).getStamp())));
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs2)).getTopicId())).isNullId());
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs2)).getTopicId())).isLocal());
        Assert.assertEquals(-1, ((int) (((AgentId) (((ClientSubscription) (cs2)).getTopicId())).getEncodableClassId())));
        Assert.assertEquals(8, ((int) (((AgentId) (((ClientSubscription) (cs2)).getTopicId())).getEncodedSize())));
        Assert.assertEquals("#60.70.80", ((AgentId) (((ClientSubscription) (cs2)).getTopicId())).toString());
        Assert.assertEquals(80, ((int) (((AgentId) (((ClientSubscription) (cs2)).getTopicId())).hashCode())));
        Assert.assertEquals("#60.70.80", ((ClientSubscription) (cs2)).getTopicIdAsString());
        Assert.assertEquals("test-selector", ((ClientSubscription) (cs2)).getSelector());
        Assert.assertTrue(((ClientSubscription) (cs2)).getDurable());
        Assert.assertEquals(100, ((int) (((ClientSubscription) (cs2)).getActive())));
        Assert.assertEquals(0, ((int) (((ClientSubscription) (cs2)).getDeliveredMessageCount())));
        Assert.assertEquals("ClientSubscription#10.20.30test-subscription", ((ClientSubscription) (cs2)).toString());
        Assert.assertEquals("test-subscription", ((ClientSubscription) (cs2)).getName());
        checkEncoding(cs2);
        Assert.assertEquals("test-subscription", name);
        Assert.assertEquals("test-selector", selector);
        Assert.assertEquals("clientId", clientID);
        Assert.assertEquals(131075, ((int) (((ClientSubscription) (cs1)).getEncodableClassId())));
        Assert.assertEquals(120, ((int) (((ClientSubscription) (cs1)).getThreshold())));
        Assert.assertEquals(0L, ((long) (((ClientSubscription) (cs1)).getNbMsgsSentToDMQSinceCreation())));
        Assert.assertEquals(0L, ((long) (((ClientSubscription) (cs1)).getNbMsgsDeliveredSinceCreation())));
        Assert.assertEquals(130, ((int) (((ClientSubscription) (cs1)).getEncodedSize())));
        Assert.assertEquals(130, ((int) (((ClientSubscription) (cs1)).getNbMaxMsg())));
        Assert.assertEquals(20, ((short) (((AgentId) (((ClientSubscription) (cs1)).getProxyId())).getTo())));
        Assert.assertEquals(10, ((short) (((AgentId) (((ClientSubscription) (cs1)).getProxyId())).getFrom())));
        Assert.assertEquals(30, ((int) (((AgentId) (((ClientSubscription) (cs1)).getProxyId())).getStamp())));
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs1)).getProxyId())).isNullId());
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs1)).getProxyId())).isLocal());
        Assert.assertEquals(-1, ((int) (((AgentId) (((ClientSubscription) (cs1)).getProxyId())).getEncodableClassId())));
        Assert.assertEquals(8, ((int) (((AgentId) (((ClientSubscription) (cs1)).getProxyId())).getEncodedSize())));
        Assert.assertEquals("#10.20.30", ((AgentId) (((ClientSubscription) (cs1)).getProxyId())).toString());
        Assert.assertEquals(30, ((int) (((AgentId) (((ClientSubscription) (cs1)).getProxyId())).hashCode())));
        Assert.assertEquals(40, ((int) (((ClientSubscription) (cs1)).getContextId())));
        Assert.assertEquals(50, ((int) (((ClientSubscription) (cs1)).getSubRequestId())));
        Assert.assertEquals(70, ((short) (((AgentId) (((ClientSubscription) (cs1)).getTopicId())).getTo())));
        Assert.assertEquals(60, ((short) (((AgentId) (((ClientSubscription) (cs1)).getTopicId())).getFrom())));
        Assert.assertEquals(80, ((int) (((AgentId) (((ClientSubscription) (cs1)).getTopicId())).getStamp())));
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs1)).getTopicId())).isNullId());
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs1)).getTopicId())).isLocal());
        Assert.assertEquals(-1, ((int) (((AgentId) (((ClientSubscription) (cs1)).getTopicId())).getEncodableClassId())));
        Assert.assertEquals(8, ((int) (((AgentId) (((ClientSubscription) (cs1)).getTopicId())).getEncodedSize())));
        Assert.assertEquals("#60.70.80", ((AgentId) (((ClientSubscription) (cs1)).getTopicId())).toString());
        Assert.assertEquals(80, ((int) (((AgentId) (((ClientSubscription) (cs1)).getTopicId())).hashCode())));
        Assert.assertEquals("#60.70.80", ((ClientSubscription) (cs1)).getTopicIdAsString());
        Assert.assertEquals("test-selector", ((ClientSubscription) (cs1)).getSelector());
        Assert.assertTrue(((ClientSubscription) (cs1)).getDurable());
        Assert.assertEquals(100, ((int) (((ClientSubscription) (cs1)).getActive())));
        Assert.assertEquals(2, ((int) (((ClientSubscription) (cs1)).getDeliveredMessageCount())));
        Assert.assertEquals("ClientSubscription#10.20.30test-subscription", ((ClientSubscription) (cs1)).toString());
        Assert.assertEquals("test-subscription", ((ClientSubscription) (cs1)).getName());
        Assert.assertNull(o_run__23);
        Assert.assertNull(o_run__25);
        Assert.assertNull(o_run__27);
        Assert.assertNull(o_run__29);
        Assert.assertEquals("clientId", clientID);
        Assert.assertEquals(131075, ((int) (((ClientSubscription) (cs2)).getEncodableClassId())));
        Assert.assertEquals(120, ((int) (((ClientSubscription) (cs2)).getThreshold())));
        Assert.assertEquals(0L, ((long) (((ClientSubscription) (cs2)).getNbMsgsSentToDMQSinceCreation())));
        Assert.assertEquals(0L, ((long) (((ClientSubscription) (cs2)).getNbMsgsDeliveredSinceCreation())));
        Assert.assertEquals(102, ((int) (((ClientSubscription) (cs2)).getEncodedSize())));
        Assert.assertEquals(130, ((int) (((ClientSubscription) (cs2)).getNbMaxMsg())));
        Assert.assertEquals(20, ((short) (((AgentId) (((ClientSubscription) (cs2)).getProxyId())).getTo())));
        Assert.assertEquals(10, ((short) (((AgentId) (((ClientSubscription) (cs2)).getProxyId())).getFrom())));
        Assert.assertEquals(30, ((int) (((AgentId) (((ClientSubscription) (cs2)).getProxyId())).getStamp())));
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs2)).getProxyId())).isNullId());
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs2)).getProxyId())).isLocal());
        Assert.assertEquals(-1, ((int) (((AgentId) (((ClientSubscription) (cs2)).getProxyId())).getEncodableClassId())));
        Assert.assertEquals(8, ((int) (((AgentId) (((ClientSubscription) (cs2)).getProxyId())).getEncodedSize())));
        Assert.assertEquals("#10.20.30", ((AgentId) (((ClientSubscription) (cs2)).getProxyId())).toString());
        Assert.assertEquals(30, ((int) (((AgentId) (((ClientSubscription) (cs2)).getProxyId())).hashCode())));
        Assert.assertEquals(40, ((int) (((ClientSubscription) (cs2)).getContextId())));
        Assert.assertEquals(50, ((int) (((ClientSubscription) (cs2)).getSubRequestId())));
        Assert.assertEquals(70, ((short) (((AgentId) (((ClientSubscription) (cs2)).getTopicId())).getTo())));
        Assert.assertEquals(60, ((short) (((AgentId) (((ClientSubscription) (cs2)).getTopicId())).getFrom())));
        Assert.assertEquals(80, ((int) (((AgentId) (((ClientSubscription) (cs2)).getTopicId())).getStamp())));
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs2)).getTopicId())).isNullId());
        Assert.assertFalse(((AgentId) (((ClientSubscription) (cs2)).getTopicId())).isLocal());
        Assert.assertEquals(-1, ((int) (((AgentId) (((ClientSubscription) (cs2)).getTopicId())).getEncodableClassId())));
        Assert.assertEquals(8, ((int) (((AgentId) (((ClientSubscription) (cs2)).getTopicId())).getEncodedSize())));
        Assert.assertEquals("#60.70.80", ((AgentId) (((ClientSubscription) (cs2)).getTopicId())).toString());
        Assert.assertEquals(80, ((int) (((AgentId) (((ClientSubscription) (cs2)).getTopicId())).hashCode())));
        Assert.assertEquals("#60.70.80", ((ClientSubscription) (cs2)).getTopicIdAsString());
        Assert.assertEquals("test-selector", ((ClientSubscription) (cs2)).getSelector());
        Assert.assertTrue(((ClientSubscription) (cs2)).getDurable());
        Assert.assertEquals(100, ((int) (((ClientSubscription) (cs2)).getActive())));
        Assert.assertEquals(0, ((int) (((ClientSubscription) (cs2)).getDeliveredMessageCount())));
        Assert.assertEquals("ClientSubscription#10.20.30test-subscription", ((ClientSubscription) (cs2)).toString());
        Assert.assertEquals("test-subscription", ((ClientSubscription) (cs2)).getName());
    }

    private void checkEncoding(ClientSubscription cs) throws Exception {
        byte[] bytes = EncodingHelper.encode(cs);
        ClientSubscription csDec = ((ClientSubscription) (EncodingHelper.decode(cs.getEncodableClassId(), bytes)));
        junit.framework.Assert.assertEquals(cs.getThreshold(), csDec.getThreshold());
        junit.framework.Assert.assertEquals(cs.getDeliveredIds(), csDec.getDeliveredIds());
        junit.framework.Assert.assertEquals(cs.getDeniedMsgs(), csDec.getDeniedMsgs());
        assertEquals(cs.getClientID(), csDec.getClientID());
    }
}

