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

import org.apache.maven.plugin.logging.Log;

/**
 * @author ndeloof
 * @version $Id$
 */
public interface GwtShellScriptConfiguration
    extends MavenScriptConfiguration
{

    /**
     * @return
     */
    String getExtraJvmArgs();

    /**
     * @return
     */
    File getGen();

    /**
     * @return
     */
    String getLogLevel();

    /**
     * @return
     */
    String getStyle();

    /**
     * @return
     */
    File getOutput();

    /**
     * @return
     */
    boolean isNoServer();

    /**
     * @return
     */
    boolean getSourcesOnPath();

    /**
     * @return
     */
    boolean getResourcesOnPath();

    /**
     * @return The File path to the plugin JAR artifact in the local repository
     * @since 1.1
     */
    File getPluginJar();
    
    /**
     * @return the path to the java executable to use with the forked script
     * @since 1.1
     */
    String getJvm();
    
    Log getLog();
    
    String getVersion();

}
