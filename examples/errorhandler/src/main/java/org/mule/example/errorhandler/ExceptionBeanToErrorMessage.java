/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.errorhandler;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

public class ExceptionBeanToErrorMessage extends AbstractTransformer
{

    public ExceptionBeanToErrorMessage()
    {
        registerSourceType(ExceptionBean.class);
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            return new ErrorMessage((ExceptionBean)src);
        }
        catch (InstantiationException e)
        {
            throw new TransformerException(this, e);
        }
    }

}
