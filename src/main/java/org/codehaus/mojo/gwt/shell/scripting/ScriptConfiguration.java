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
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * @author ndeloof
 *
 */
public interface ScriptConfiguration
{
    Log getLog();

    File getBuildDir();

    String[] getCompileTarget();

    File getContextXml();

    String getExtraJvmArgs();

    File getGen();

    File getGwtHome();

    String getLogLevel();

    boolean isNoServer();

    File getOutput();

    int getPort();

    MavenProject getProject();

    String getRunTarget();

    String getStyle();

    File getTomcat();

    File getWebXml();

    boolean isWebXmlServletPathAsIs();

    String getShellServletMappingURL();

    String[] getGeneratorRootClasses();

    String getGeneratorDestinationPackage();

    boolean isGenerateGettersAndSetters();

    boolean isGeneratePropertyChangeSupport();

    boolean isOverwriteGeneratedClasses();

    int getDebugPort();

    boolean isDebugSuspend();

    String getGwtVersion();

    String getTestFilter();

    boolean getSourcesOnPath();

    boolean getResourcesOnPath();

    boolean isEnableAssertions();

    List getPluginClasspathList();

    org.apache.maven.artifact.factory.ArtifactFactory getArtifactFactory();

    org.apache.maven.artifact.resolver.ArtifactResolver getResolver();

    org.apache.maven.artifact.repository.ArtifactRepository getLocalRepository();

    java.util.List getRemoteRepositories();

    File getI18nOutputDir();

    String[] getI18nMessagesNames();

    String[] getI18nConstantsNames();

    String getExtraTestArgs();

    boolean isTestSkip();

}