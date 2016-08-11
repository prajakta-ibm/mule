/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.config.spring.dsl.spring.ObjectFactoryCommonConfigurator;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.util.ClassUtils;

import java.util.Map;

/**
 * {@link ObjectFactoryCommonConfigurator} for transformers in Mules.
 *
 * The transformer class that will be configured setting the returnType, mimeType,
 * name, encoding and ignoreBadInput configuration.
 *
 * @since 4.0
 */
public class TransformerConfigurator implements ObjectFactoryCommonConfigurator<AbstractTransformer> {

  private Class<?> getReturnType(String returnClass) {
    Class<?> returnType = Object.class;
    if (returnClass != null) {
      try {
        returnType = ClassUtils.getClass(returnClass);
      } catch (ClassNotFoundException e) {
        throw new MuleRuntimeException(e);
      }
    }
    return returnType;
  }

  @Override
  public void configure(AbstractTransformer transformerInstance, Map<String, Object> parameters) {
    String returnClass = (String) parameters.get("returnClass");
    String mimeType = (String) parameters.get("mimeType");
    String name = (String) parameters.get("name");
    String encoding = (String) parameters.get("encoding");
    Boolean ignoreBadInput = (Boolean) parameters.get("ignoreBadInput");
    if (returnClass != null || mimeType != null) {
      DataTypeParamsBuilder builder = DataType.builder().type(getReturnType(returnClass));
      if (isNotEmpty(mimeType)) {
        builder.mediaType(mimeType);
      }
      transformerInstance.setReturnDataType(builder.charset(encoding).build());
    }
    if (ignoreBadInput != null) {
      transformerInstance.setIgnoreBadInput(ignoreBadInput);
    }
    transformerInstance.setName(name);
  }

}
