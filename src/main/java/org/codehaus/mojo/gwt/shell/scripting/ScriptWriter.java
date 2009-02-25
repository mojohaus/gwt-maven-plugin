package org.codehaus.mojo.gwt.shell.scripting;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.gwt.GwtRuntime;

/**
 * @author ccollins
 * @version $Id$
 */
public interface ScriptWriter
{
    /**
     * @param configuration
     * @param string
     */
    void createScript( GwtShellScriptConfiguration configuration, String string )
        throws MojoExecutionException;

    /**
     * @param compileMojo
     * @param runtime
     * @param forkbooter2
     * @param clazz
     */
    void executeClass( GwtShellScriptConfiguration configuration, GwtRuntime runtime, ClasspathStrategy strategy,
                       String clazz )
        throws MojoExecutionException;

    /**
     * @param string
     */
    void print( String string );

    void println( String string );

    void println();

    File getExecutable();

    /**
     * @param string
     * @param absolutePath
     */
    void addVariable( String string, String absolutePath );

}
