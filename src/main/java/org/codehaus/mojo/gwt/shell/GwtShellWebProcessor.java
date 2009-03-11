package org.codehaus.mojo.gwt.shell;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.ArrayList;

import org.codehaus.mojo.gwt.webxml.GwtWebInfProcessor;
import org.codehaus.mojo.gwt.webxml.ServletDescriptor;

/**
 *
 * @author cooper
 * @version $Id$
 */
public class GwtShellWebProcessor
    extends GwtWebInfProcessor
{

    /** Creates a new instance of GwtShellWebProcessor */
    public GwtShellWebProcessor( String targetWebXml, File sourceWebXml, String shellServletMappingURL )
        throws Exception
    {
        // obtain web.xml
        this.webXmlPath = sourceWebXml.getAbsolutePath();

        if ( !sourceWebXml.exists() || !sourceWebXml.canRead() )
        {
            throw new Exception( "Unable to locate source web.xml" );
        }

        this.destination = new File( targetWebXml );
        this.servletDescriptors = new ArrayList<ServletDescriptor>();
        ServletDescriptor d = new ServletDescriptor( shellServletMappingURL, "com.google.gwt.dev.shell.GWTShellServlet" );
        d.setName( "shell" );
        this.servletDescriptors.add( d );
    }
}
