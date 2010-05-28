package org.codehaus.mojo.gwt;

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

import static org.codehaus.mojo.gwt.EmbeddedServer.JETTY;
import static org.codehaus.mojo.gwt.EmbeddedServer.TOMCAT;
import static org.codehaus.mojo.gwt.GwtVersion.Capabilities.JAVA5_SYNTAX;
import static org.codehaus.mojo.gwt.GwtVersion.Capabilities.MULTI_MODULE_COMPILER;
import static org.codehaus.mojo.gwt.GwtVersion.Capabilities.MULTI_MODULE_SHELL;
import static org.codehaus.mojo.gwt.GwtVersion.Capabilities.OOPHM;
import static org.codehaus.mojo.gwt.GwtVersion.Capabilities.PARALLEL_COMPILER;
import static org.codehaus.mojo.gwt.GwtVersion.Capabilities.SOYC;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public enum GwtVersion
{
    ONE_DOT_FOUR( 0, 
        "com.google.gwt.dev.GWTShell", "com.google.gwt.dev.GWTCompiler", TOMCAT, "-out" )

    , ONE_DOT_FIVE( ONE_DOT_FOUR.capabilities + JAVA5_SYNTAX,
        "com.google.gwt.dev.GWTShell", "com.google.gwt.dev.GWTCompiler", TOMCAT, "-out" )

    , ONE_DOT_SIX( ONE_DOT_FIVE.capabilities + PARALLEL_COMPILER + MULTI_MODULE_COMPILER,
        "com.google.gwt.dev.HostedMode", "com.google.gwt.dev.Compiler", JETTY, "-war" )

    , ONE_DOT_SEVEN( ONE_DOT_SIX.capabilities,
        "com.google.gwt.dev.HostedMode", "com.google.gwt.dev.Compiler", JETTY, "-war" )

    , TWO_DOT_ZERO( ONE_DOT_SEVEN.capabilities + MULTI_MODULE_SHELL + SOYC + OOPHM,
        "com.google.gwt.dev.DevMode", "com.google.gwt.dev.Compiler", JETTY, "-war" )

    , TWO_DOT_ONE( TWO_DOT_ZERO.capabilities,
        "com.google.gwt.dev.DevMode", "com.google.gwt.dev.Compiler", JETTY, "-war" );


    static GwtVersion fromMavenVersion( String version )
    {
        if ( version.startsWith( "1.4" ) )
        {
            return ONE_DOT_FOUR;
        }
        if ( version.startsWith( "1.5" ) )
        {
            return ONE_DOT_FIVE;
        }
        if ( version.startsWith( "1.6" ) )
        {
            return ONE_DOT_SIX;
        }
        if ( version.startsWith( "1.7" ) )
        {
            return ONE_DOT_SEVEN;
        }
        if ( version.startsWith( "2.0" ) )
        {
            return TWO_DOT_ZERO;
        }
        if ( version.startsWith( "2.1" ) )
        {
            return TWO_DOT_ONE;
        }
        throw new IllegalStateException( "Unsupported GWT version " + version );
    }

    private GwtVersion( int capabilities, String shellFQCN, String compilerFQCN,
                        EmbeddedServer emebededServer,
        String webOutputArgument )
    {
        this.capabilities = capabilities;
        this.shellFQCN = shellFQCN;
        this.compilerFQCN = compilerFQCN;
        this.webOutputArgument = webOutputArgument;
        this.emebededServer = emebededServer;
    }

    private int capabilities;

    private String shellFQCN;

    private String compilerFQCN;

    private EmbeddedServer emebededServer;

    private String webOutputArgument;


    /**
     * @return fully qualified class name of the GWTShell "main" class
     */
    public String getShellFQCN()
    {
        return shellFQCN;
    }

    public String getCompilerFQCN()
    {
        return compilerFQCN;
    }

    public EmbeddedServer getEmbeddedServer()
    {
        return emebededServer;
    }

    public boolean supportJava5()
    {
        return ( capabilities & JAVA5_SYNTAX ) != 0;
    }

    public boolean supportMultiModuleCompile()
    {
        return ( capabilities & MULTI_MODULE_COMPILER ) != 0;
    }

    public boolean supportMultiModuleShell()
    {
        return ( capabilities & MULTI_MODULE_SHELL ) != 0;
    }

    public boolean supportParallelCompile()
    {
        return ( capabilities & PARALLEL_COMPILER ) != 0;
    }

    public boolean supportSOYC()
    {
        return ( capabilities & SOYC ) != 0;
    }
    
    public boolean supportOOPHM()
    {
        return ( capabilities & OOPHM ) != 0;
    }

    public String getWebOutputArgument()
    {
        return webOutputArgument;
    }

    static final class Capabilities
    {
        static final int JAVA5_SYNTAX = 1;

        static final int PARALLEL_COMPILER = 2;

        static final int MULTI_MODULE_SHELL = 4;

        static final int MULTI_MODULE_COMPILER = 8;

        static final int SOYC = 16;

        static final int OOPHM = 32;
    }
}
