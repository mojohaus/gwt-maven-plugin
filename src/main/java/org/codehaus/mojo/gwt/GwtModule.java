package org.codehaus.mojo.gwt;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.util.xml.Xpp3Dom;

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


/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class GwtModule
{
    private Xpp3Dom xml;

    private String name;

    public GwtModule( String name, Xpp3Dom xml )
    {
        this.name = name;
        this.xml = xml;
    }

    private String getRenameTo()
    {
        return xml.getAttribute( "rename-to" );
    }

    public String getPublic()
    {
         Xpp3Dom node = xml.getChild( "public" );
         return ( node == null ? "public" : node.getAttribute( "path" ) );
    }

    public String[] getSuperSources()
    {
        Xpp3Dom nodes[] = xml.getChildren( "super-source" );
        if ( nodes == null )
        {
            return new String[0];
        }
        String[] superSources = new String[nodes.length];
        int i = 0;
        for ( Xpp3Dom node : nodes )
        {
            String path = node.getAttribute( "path" );
            if ( path == null )
            {
                path = "";
            }
            superSources[i++] = path;
        }
        return superSources;
    }

    public String[] getSources()
    {
        Xpp3Dom nodes[] = xml.getChildren( "source" );
        if ( nodes == null )
        {
            return new String[] { "client" };
        }
        String[] sources = new String[nodes.length];
        int i = 0;
        for ( Xpp3Dom node : nodes )
        {
            sources[i++] = node.getAttribute( "path" );
        }
        return sources;
    }

    public String[] getEntryPoints()
    {
        Xpp3Dom nodes[] = xml.getChildren( "entry-point" );
        if ( nodes == null )
        {
            return new String[0];
        }
        String[] entryPoints = new String[nodes.length];
        int i = 0;
        for ( Xpp3Dom node : nodes )
        {
            entryPoints[i++] = node.getAttribute( "class" );
        }
        return entryPoints;
    }

    public String[] getInherits()
    {
        Xpp3Dom nodes[] = xml.getChildren( "inherits" );
        if ( nodes == null )
        {
            return new String[0];
        }
        String[] inherits = new String[nodes.length];
        int i = 0;
        for ( Xpp3Dom node : nodes )
        {
            inherits[i++] = node.getAttribute( "name" );
        }
        return inherits;
    }

    public Map<String, String> getServlets()
    {
        Map<String, String> servlets = new HashMap<String, String>();
        Xpp3Dom nodes[] = xml.getChildren( "servlet" );
        if ( nodes == null )
        {
            return servlets;
        }
        for ( Xpp3Dom node : nodes )
        {
            servlets.put( getPath() + node.getAttribute( "path" ), node.getAttribute( "class" ) );
        }
        return servlets;
    }

    public String getName()
    {
        return name;
    }

    public String getPackage()
    {
        return name.substring( 0, name.lastIndexOf( '.' ) );
    }

    public String getPath()
    {
        if ( getRenameTo() != null )
        {
            return getRenameTo();
        }
        return name;
    }
}
