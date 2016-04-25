/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.client;

import static org.mule.api.client.SimpleOptionsBuilder.newOptions;
import static org.mule.api.config.MuleProperties.OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.client.OperationOptions;
import org.mule.api.connector.ConnectorOperationLocator;
import org.mule.api.construct.MuleConnectionsBuilder;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointCache;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.transport.ReceiveException;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.SimpleEndpointCache;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.routing.MuleMessageInfoMapping;

import java.util.Map;

public class DefaultLocalMuleClient implements LocalMuleClient
{
    protected final MuleContext muleContext;
    private final EndpointCache endpointCache;
    private ConnectorOperationLocator connectorOperatorLocator;

    public DefaultLocalMuleClient(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        this.endpointCache = new SimpleEndpointCache(muleContext);
    }

    private ConnectorOperationLocator getConnectorMessageProcessLocator()
    {
        if (connectorOperatorLocator == null)
        {
            this.connectorOperatorLocator = muleContext.getRegistry().get(OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR);
            if (this.connectorOperatorLocator == null)
            {
                throw new MuleRuntimeException(CoreMessages.createStaticMessage("Could not find required %s in the registry under key %s", ConnectorOperationLocator.class.getName(), OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR));
            }
        }
        return connectorOperatorLocator;
    }

    @Override
    public MuleMessage process(OutboundEndpoint endpoint,
                               Object payload,
                               Map<String, Object> messageProperties) throws MuleException
    {
        return process(endpoint, new DefaultMuleMessage(payload, messageProperties, muleContext));

    }

    @Override
    public MuleMessage process(OutboundEndpoint endpoint, MuleMessage message) throws MuleException
    {
        return returnMessage(endpoint.process(createMuleEvent(message, endpoint)));
    }

    @Override
    public MuleMessage request(InboundEndpoint endpoint, long timeout) throws MuleException
    {
        try
        {
            return endpoint.request(timeout);
        }
        catch (Exception e)
        {
            throw new ReceiveException(endpoint, timeout, e);
        }
    }

    @Override
    public void dispatch(String url, Object payload, Map<String, Object> messageProperties)
        throws MuleException
    {
        dispatch(url, new DefaultMuleMessage(payload, messageProperties, muleContext));
    }

    @Override
    public MuleMessage send(String url, Object payload, Map<String, Object> messageProperties)
        throws MuleException
    {
        return send(url, new DefaultMuleMessage(payload, messageProperties, muleContext));
    }

    @Override
    public MuleMessage send(String url, MuleMessage message) throws MuleException
    {
        final MessageProcessor connectorMessageProcessor = getConnectorMessageProcessLocator().locateConnectorOperation(url, newOptions().build(), MessageExchangePattern.REQUEST_RESPONSE);
        if (connectorMessageProcessor != null)
        {
            return returnMessage(connectorMessageProcessor.process(createRequestResponseMuleEvent(message)));
        }
        else
        {
            OutboundEndpoint endpoint = endpointCache.getOutboundEndpoint(url, MessageExchangePattern.REQUEST_RESPONSE, null);
            return returnMessage(endpoint.process(createMuleEvent(message, endpoint)));
        }
    }

    @Override
    public MuleMessage send(String url, MuleMessage message, OperationOptions operationOptions) throws MuleException
    {
        final MessageProcessor connectorMessageProcessor = getConnectorMessageProcessLocator().locateConnectorOperation(url, operationOptions, MessageExchangePattern.REQUEST_RESPONSE);
        if (connectorMessageProcessor != null)
        {
            return returnMessage(connectorMessageProcessor.process(createRequestResponseMuleEvent(message)));
        }
        else
        {
            return internalSend(url, message, operationOptions.getResponseTimeout());
        }
    }

    @Override
    public MuleMessage send(String url, Object payload, Map<String, Object> messageProperties, long timeout)
        throws MuleException
    {
        return send(url, new DefaultMuleMessage(payload, messageProperties, muleContext), timeout);

    }

    @Override
    public MuleMessage send(String url, MuleMessage message, long timeout) throws MuleException
    {
        return send(url, message, newOptions().responseTimeout(timeout).build());
    }

    private MuleMessage internalSend(String url, MuleMessage message, Long timeout) throws MuleException
    {
        OutboundEndpoint endpoint = endpointCache.getOutboundEndpoint(url, MessageExchangePattern.REQUEST_RESPONSE, timeout);
        return returnMessage(endpoint.process(createMuleEvent(message, endpoint)));
    }

    @Override
    public void dispatch(String url, MuleMessage message) throws MuleException
    {
        final MessageProcessor connectorMessageProcessor = getConnectorMessageProcessLocator().locateConnectorOperation(url, newOptions().build(), MessageExchangePattern.ONE_WAY);
        if (connectorMessageProcessor != null)
        {
            connectorMessageProcessor.process(createRequestResponseMuleEvent(message));
        }
        else
        {
            OutboundEndpoint endpoint = endpointCache.getOutboundEndpoint(url, MessageExchangePattern.ONE_WAY, null);
            endpoint.process(createMuleEvent(message, endpoint));
        }
    }

    @Override
    public void dispatch(String url, MuleMessage message, OperationOptions operationOptions) throws MuleException
    {
        final MessageProcessor connectorMessageProcessor = getConnectorMessageProcessLocator().locateConnectorOperation(url, operationOptions, MessageExchangePattern.ONE_WAY);
        if (connectorMessageProcessor != null)
        {
            connectorMessageProcessor.process(createRequestResponseMuleEvent(message));
        }
        else
        {
            dispatch(url, message);
        }
    }

    @Override
    public MuleMessage request(String url, long timeout) throws MuleException
    {
        InboundEndpoint endpoint = endpointCache.getInboundEndpoint(url, MessageExchangePattern.ONE_WAY);
        try
        {
            return endpoint.request(timeout);
        }
        catch (Exception e)
        {
            throw new ReceiveException(endpoint, timeout, e);
        }
    }

    @Override
    public MuleMessage process(String uri,
                               MessageExchangePattern mep,
                               Object payload,
                               Map<String, Object> messageProperties) throws MuleException
    {
        return process(uri, mep, new DefaultMuleMessage(payload, messageProperties, muleContext));
    }

    @Override
    public MuleMessage process(String uri, MessageExchangePattern mep, MuleMessage message)
        throws MuleException
    {
        OutboundEndpoint endpoint = endpointCache.getOutboundEndpoint(uri, mep, null);
        return returnMessage(endpoint.process(createMuleEvent(message, endpoint)));
    }

    protected MuleEvent createMuleEvent(MuleMessage message, OutboundEndpoint endpoint)
        throws EndpointException
    {
        return new DefaultMuleEvent(message, endpoint.getExchangePattern(), new MuleClientFlowConstruct(
            muleContext));
    }

    protected MuleEvent createRequestResponseMuleEvent(MuleMessage message)
            throws EndpointException
    {
        return new DefaultMuleEvent(message, MessageExchangePattern.REQUEST_RESPONSE, new MuleClientFlowConstruct(
                muleContext));
    }

    protected MuleMessage returnMessage(MuleEvent event)
    {
        if (event != null && !VoidMuleEvent.getInstance().equals(event))
        {
            return event.getMessage();
        }
        else
        {
            return null;
        }
    }

    /**
     * Placeholder class which makes the default exception handler available.
     */
    static public class MuleClientFlowConstruct implements FlowConstruct
    {
        static MessageInfoMapping messageInfoMapping = new MuleMessageInfoMapping();

        MuleContext muleContext;

        public MuleClientFlowConstruct(MuleContext muleContext)
        {
            this.muleContext = muleContext;
        }

        @Override
        public String getName()
        {
            return "MuleClient";
        }

        @Override
        public MessagingExceptionHandler getExceptionListener()
        {
            return new DefaultMessagingExceptionStrategy(muleContext);
        }

        @Override
        public LifecycleState getLifecycleState()
        {
            return null;
        }

        @Override
        public FlowConstructStatistics getStatistics()
        {
            return null;
        }

        @Override
        public MuleContext getMuleContext()
        {
            return muleContext;
        }

        @Override
        public MessageInfoMapping getMessageInfoMapping()
        {
            return messageInfoMapping;
        }

        public MessageProcessorChain getMessageProcessorChain()
        {
            return null;
        }

        @Override
        public void visitForConnections(MuleConnectionsBuilder visitor)
        {
            // Nothing to do
        }
    }
}
