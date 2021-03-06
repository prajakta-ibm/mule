/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.context.MuleContextAware;

import java.util.Optional;

public class QueueOutboundMessagesCounter implements TransactionScenarios.OutboundMessagesCounter, MuleContextAware {

  private int numberOfMessagesArrived;
  private MuleClient muleClient;

  @Override
  public int numberOfMessagesThatArrived() throws Exception {
    while (true) {
      Optional<MuleMessage> messageOptional = muleClient.request("outboundRequester", 100).getRight();
      if (messageOptional.isPresent()) {
        numberOfMessagesArrived++;
      } else {
        break;
      }
    }
    return numberOfMessagesArrived;
  }

  @Override
  public void close() {
    numberOfMessagesArrived = 0;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleClient = context.getClient();
  }
}
