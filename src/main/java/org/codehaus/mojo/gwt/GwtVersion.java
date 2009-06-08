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

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public enum GwtVersion
{
    ONE_DOT_FOUR( false, "com.google.gwt.dev.GWTShell", "com.google.gwt.dev.GWTCompiler", EmbeddedServer.TOMCAT, 
                  false, false, "-out" )

    , ONE_DOT_FIVE( true, "com.google.gwt.dev.GWTShell", "com.google.gwt.dev.GWTCompiler", EmbeddedServer.TOMCAT,
                    false, false, "-out" )

    , ONE_DOT_SIX( true, "com.google.gwt.dev.HostedMode", "com.google.gwt.dev.Compiler", EmbeddedServer.JETTY, 
                   true, true, "-war" )

    , FUTURE( true, "com.google.gwt.dev.HostedMode", "com.google.gwt.dev.Compiler", EmbeddedServer.JETTY, 
              true, true, "-war" );

    
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
                return FUTURE;
            }

    private GwtVersion( boolean supportJava5, String shellFQCN, String compilerFQCN,
                        EmbeddedServer emebededServer,
                        boolean supportMultiModuleCompiler, boolean supportParallelCompiler, String webOutputArgument
                         )
    {
        this.supportJava5 = supportJava5;
        this.shellFQCN = shellFQCN;
        this.compilerFQCN = compilerFQCN;
        this.supportMultiModuleCompiler = supportMultiModuleCompiler;
        this.supportParallelCompiler = supportParallelCompiler;
        this.webOutputArgument = webOutputArgument;
        this.emebededServer = emebededServer;
    }

    private boolean supportJava5;
    
    private String shellFQCN;
    
    private String compilerFQCN;
    
    private EmbeddedServer emebededServer;
    
    private boolean supportMultiModuleCompiler;
    
    private boolean supportParallelCompiler;
    
    private String webOutputArgument;
    
    
    public boolean supportJava5()
    {
        return supportJava5;
    }

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

    public boolean supportMultiModuleCompile()
    {
        return supportMultiModuleCompiler;
    }

    public boolean supportParallelCompile()
    {
        return supportParallelCompiler;
    }

    public String getWebOutputArgument()
    {
        return webOutputArgument;
    }


}
