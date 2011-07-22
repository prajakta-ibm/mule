/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class Http10TestCase extends AbstractServiceAndFlowTestCase
{
    // Dynamic ports is static because test is using setDisposeContextPerClass(true);
    private static DynamicPort dynamicPort;    
    
    public Http10TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-10-conf-service.xml"},
            {ConfigVariant.FLOW, "http-10-conf-flow.xml"}
        });
    }      

    @BeforeClass
    public static void createDynamicPort() throws Throwable
    {
        dynamicPort = new DynamicPort("port1");
        dynamicPort.before();
    }

    @AfterClass
    public static void releaseDynamicPort()
    {
        dynamicPort.after();
    }

    @Test
    public void testHttp10TransformerNotOnProtocol() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String,String> props = new HashMap<String, String>();
        
        MuleMessage result = client.send("cxfOutbound", "Dan", props);
        assertEquals("Hello Dan", result.getPayload());
        
        result = client.request("vm://out", 1000);
        assertFalse("chunked".equals(result.getOutboundProperty(HttpConstants.HEADER_TRANSFER_ENCODING)));
    }

    @Test
    public void testHttp10TransformerOnProtocol() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String,String> props = new HashMap<String, String>();
        
        MuleMessage result = client.send("cxfOutbound2", "Dan", props);
        assertEquals("Hello Dan", result.getPayload());
        
        result = client.request("vm://out", 1000);
        assertFalse("chunked".equals(result.getOutboundProperty(HttpConstants.HEADER_TRANSFER_ENCODING)));
    }
}
