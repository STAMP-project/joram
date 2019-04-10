package org.objectweb.joram.mom.proxies;


public class ClientContextEncodingTest {
    @org.junit.Test(timeout = 10000)
    public void run() throws java.lang.Exception {
        org.objectweb.joram.mom.proxies.EncodingHelper.init();
        fr.dyade.aaa.agent.AgentId proxyId = null;
        int id = 40;
        org.objectweb.joram.mom.proxies.ClientContext cc1 = new org.objectweb.joram.mom.proxies.ClientContext(proxyId, id);
        org.junit.Assert.assertEquals(131076, ((int) (((org.objectweb.joram.mom.proxies.ClientContext) (cc1)).getEncodableClassId())));
        org.junit.Assert.assertEquals(17, ((int) (((org.objectweb.joram.mom.proxies.ClientContext) (cc1)).getEncodedSize())));
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.proxies.ClientContext) (cc1)).getProxyId());
        org.junit.Assert.assertEquals("ClientContext (proxyId=null,id=40,tempDestinations=[],deliveringQueues={},transactionsTable=null,started=false,cancelledRequestId=-1,activeSubs=[],repliesBuffer=[])", ((org.objectweb.joram.mom.proxies.ClientContext) (cc1)).toString());
        boolean o_run__7 = cc1.getActiveSubList().add("sub1");
        org.junit.Assert.assertTrue(o_run__7);
        boolean o_run__9 = cc1.getActiveSubList().add("sub2");
        org.junit.Assert.assertTrue(o_run__9);
        boolean o_run__11 = cc1.getActiveSubList().add("sub2");
        org.junit.Assert.assertTrue(o_run__11);
        fr.dyade.aaa.agent.AgentId queue1 = new fr.dyade.aaa.agent.AgentId(((short) (50)), ((short) (60)), 70);
        fr.dyade.aaa.agent.AgentId o_run__15 = cc1.getDeliveringQueueTable().put(queue1, queue1);
        org.junit.Assert.assertNull(o_run__15);
        fr.dyade.aaa.agent.AgentId queue2 = new fr.dyade.aaa.agent.AgentId(((short) (80)), ((short) (90)), 100);
        fr.dyade.aaa.agent.AgentId o_run__19 = cc1.getDeliveringQueueTable().put(queue2, queue2);
        org.junit.Assert.assertNull(o_run__19);
        fr.dyade.aaa.agent.AgentId tmpDest1 = new fr.dyade.aaa.agent.AgentId(((short) (110)), ((short) (120)), 130);
        boolean o_run__23 = cc1.getTempDestinationList().add(tmpDest1);
        org.junit.Assert.assertTrue(o_run__23);
        fr.dyade.aaa.agent.AgentId tmpDest2 = new fr.dyade.aaa.agent.AgentId(((short) (120)), ((short) (130)), 140);
        boolean o_run__27 = cc1.getTempDestinationList().add(tmpDest2);
        org.junit.Assert.assertTrue(o_run__27);
        checkEncoding(cc1);
        org.junit.Assert.assertEquals(131076, ((int) (((org.objectweb.joram.mom.proxies.ClientContext) (cc1)).getEncodableClassId())));
        org.junit.Assert.assertEquals(73, ((int) (((org.objectweb.joram.mom.proxies.ClientContext) (cc1)).getEncodedSize())));
        org.junit.Assert.assertNull(((org.objectweb.joram.mom.proxies.ClientContext) (cc1)).getProxyId());
        org.junit.Assert.assertEquals("ClientContext (proxyId=null,id=40,tempDestinations=[#110.120.130, #120.130.140],deliveringQueues={#50.60.70=#50.60.70, #80.90.100=#80.90.100},transactionsTable=null,started=false,cancelledRequestId=-1,activeSubs=[sub1, sub2, sub2],repliesBuffer=[])", ((org.objectweb.joram.mom.proxies.ClientContext) (cc1)).toString());
        org.junit.Assert.assertTrue(o_run__7);
        org.junit.Assert.assertTrue(o_run__9);
        org.junit.Assert.assertTrue(o_run__11);
        org.junit.Assert.assertNull(o_run__15);
        org.junit.Assert.assertNull(o_run__19);
        org.junit.Assert.assertTrue(o_run__23);
        org.junit.Assert.assertTrue(o_run__27);
    }

    private void checkEncoding(org.objectweb.joram.mom.proxies.ClientContext cc) throws java.lang.Exception {
        byte[] bytes = org.objectweb.joram.mom.proxies.EncodingHelper.encode(cc);
        org.objectweb.joram.mom.proxies.ClientContext ccDec = ((org.objectweb.joram.mom.proxies.ClientContext) (org.objectweb.joram.mom.proxies.EncodingHelper.decode(cc.getEncodableClassId(), bytes)));
        junit.framework.Assert.assertEquals(cc.getId(), ccDec.getId());
        junit.framework.Assert.assertEquals(cc.getActiveSubList(), ccDec.getActiveSubList());
        junit.framework.Assert.assertEquals(cc.getDeliveringQueueTable(), ccDec.getDeliveringQueueTable());
        junit.framework.Assert.assertEquals(cc.getTempDestinationList(), ccDec.getTempDestinationList());
        junit.framework.Assert.assertEquals(cc.getTempDestinationList(), ccDec.getTempDestinationList());


    }
}

