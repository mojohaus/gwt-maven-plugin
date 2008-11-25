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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.maven.project.MavenProject;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.mojo.gwt.AbstractGwtMojo;
import org.codehaus.mojo.gwt.shell.scripting.DependencyScope;
import org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration;
import org.codehaus.plexus.util.FileUtils;

/**
 * Abstract Mojo for GWT-Maven.
 *
 * @author ccollins
 * @author cooper
 * @author willpugh
 */
public abstract class AbstractGwtShellMojo
    extends AbstractGwtMojo
    implements ScriptConfiguration
{

    public static final String OS_NAME = System.getProperty( "os.name" ).toLowerCase( Locale.US );

    public static final String GWT_GROUP_ID = "com.google.gwt";

    public static final String WINDOWS = "windows";

    public static final String LINUX = "linux";

    public static final String MAC = "mac";

    public static final String LEOPARD = "leopard";

    public static final String GOOGLE_WEBTOOLKIT_HOME = "google.webtoolkit.home";

    public static final String JAVA_COMMAND =
        ( System.getProperty( "java.home" ) != null ) ? FileUtils.normalize( System.getProperty( "java.home" )
            + File.separator + "bin" + File.separator + "java" ) : "java";

    // Maven properties

    /**
     * <i>Maven Internal</i>: List of artifacts for the plugin.
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    private List pluginClasspathList;

    /**
     * @component
     */
    private org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

    /**
     * @component
     */
    private org.apache.maven.artifact.resolver.ArtifactResolver resolver;

    /**
     * @parameter expression="${localRepository}"
     */
    private org.apache.maven.artifact.repository.ArtifactRepository localRepository;

    /**
     * @parameter expression="${project.remoteArtifactRepositories}"
     */
    private java.util.List remoteRepositories;

    // GWT-Maven properties

    /**
     * Location on filesystem where project should be built.
     *
     * @parameter expression="${project.build.directory}"
     */
    private File buildDir;

    /**
     * Set the GWT version number - used to build dependency paths, should match the "version" in the Maven repo.
     *
     * @parameter default-value="1.5.3"
     */
    private String gwtVersion;

    /**
     * Location on filesystem where GWT is installed - for manual mode (existing GWT on machine - not needed for
     * automatic mode).
     *
     * @parameter expression="${google.webtoolkit.home}"
     */
    private File gwtHome;

    /**
     * Location on filesystem where GWT will write output files (-out option to GWTCompiler).
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     */
    private File output;

    /**
     * Location on filesystem where GWT will write generated content for review (-gen option to GWTCompiler).
     *
     * @parameter expression="${project.build.directory}/.generated"
     */
    private File gen;

    /**
     * List of GWT module names that should be compiled with the GWT compiler.
     *
     * @parameter property="compileTargets"
     * @required
     */
    private String[] compileTarget;

    /**
     * URL that should be automatically opened by default in the GWT shell.
     *
     * @parameter
     * @required
     */
    private String runTarget;

    /**
     * GWT logging level (-logLevel ERROR, WARN, INFO, TRACE, DEBUG, SPAM, or ALL).
     *
     * @parameter default-value="INFO"
     */
    private String logLevel;

    /**
     * GWT JavaScript compiler output style (-style OBF[USCATED], PRETTY, or DETAILED).
     *
     * @parameter default-value="OBF"
     */
    private String style;

    /**
     * Prevents the embedded GWT Tomcat server from running (even if a port is specified).
     *
     * @parameter default-value="false"
     */
    private boolean noServer;

    /**
     * Runs the embedded GWT Tomcat server on the specified port.
     *
     * @parameter default-value="8888"
     */
    private int port;

    /**
     * Specify the location on the filesystem for the generated embedded Tomcat directory.
     *
     * @parameter expression="${project.build.directory}/tomcat"
     */
    private File tomcat;

    /**
     * Port to listen for debugger connection on.
     *
     * @parameter default-value="8000"
     */
    private int debugPort;

    /**
     * Source Tomcat context.xml for GWT shell - copied to /gwt/localhost/ROOT.xml (used as the context.xml for the
     * SHELL - requires Tomcat 5.0.x format - hence no default).
     *
     * @parameter
     */
    private File contextXml;

    /**
     * Source web.xml deployment descriptor that is used for GWT shell and for deployment WAR to "merge" servlet
     * entries.
     *
     * @parameter expression="${basedir}/src/main/webapp/WEB-INF/web.xml"
     */
    private File webXml;

    /**
     * Specifies whether or not to add the module name as a prefix to the servlet path when merging web.xml. If you set
     * this to false the exact path from the GWT module will be used, nothing else will be prepended.
     *
     * @parameter default-value="false"
     */
    private boolean webXmlServletPathAsIs;

    /**
     * Whether or not to suspend execution until a debugger connects.
     *
     * @parameter default-value="true"
     */
    private boolean debugSuspend;

    /**
     * Extra JVM arguments that are passed to the GWT-Maven generated scripts (for compiler, shell, etc - typically use
     * -Xmx512m here, or -XstartOnFirstThread, etc).
     *
     * @parameter expression="${google.webtoolkit.extrajvmargs}"
     */
    private String extraJvmArgs;

    /**
     * Simple string filter for classes that should be treated as GWTTestCase type (and therefore invoked during gwtTest
     * goal).
     *
     * @parameter default-value="GwtTest*"
     */
    private String testFilter;

    /**
     * Extra JVM arguments that are passed only to the GWT-Maven generated test scripts (in addition to std
     * extraJvmArgs).
     *
     * @parameter default-value=""
     */
    private String extraTestArgs;

    /**
     * Whether or not to skip GWT testing.
     *
     * @parameter default-value="false"
     */
    private boolean testSkip;

    /**
     * Whether or not to add compile source root to classpath.
     *
     * @parameter default-value="true"
     */
    private boolean sourcesOnPath;

    /**
     * Whether or not to add resources root to classpath.
     *
     * @parameter default-value="true"
     */
    private boolean resourcesOnPath;

    /**
     * Whether or not to enable assertions in generated scripts (-ea).
     *
     * @parameter default-value="false"
     */
    private boolean enableAssertions;

    /**
     * Specifies the mapping URL to be used with the shell servlet.
     *
     * @parameter default-value="/*"
     */
    private String shellServletMappingURL;

    /**
     * Location on filesystem to output generated i18n Constants and Messages interfaces.
     *
     * @parameter expression="${basedir}/src/main/java/"
     */
    private File i18nOutputDir;

    /**
     * List of names of properties files that should be used to generate i18n Messages interfaces.
     *
     * @parameter
     */
    private String[] i18nMessagesNames;

    /**
     * List of names of properties files that should be used to generate i18n Constants interfaces.
     *
     * @parameter
     */
    private String[] i18nConstantsNames;

    /**
     * Top level (root) of classes to begin generation from.
     *
     * @parameter property="generatorRootClasses"
     */
    private String[] generatorRootClasses;

    /**
     * Destination package for generated classes.
     *
     * @parameter
     */
    private String generatorDestinationPackage;

    /**
     * Whether or not to generate getter/setter methods for generated classes.
     *
     * @parameter
     */
    private boolean generateGettersAndSetters;

    /**
     * Whether or not to generate PropertyChangeSupport handling for generated classes.
     *
     * @parameter
     */
    private boolean generatePropertyChangeSupport;

    /**
     * Whether or not to overwrite generated classes if they exist.
     *
     * @parameter
     */
    private boolean overwriteGeneratedClasses;

    // methods

    /**
     * Helper hack for classpath problems, used as a fallback.
     *
     * @return
     */
    protected ClassLoader fixThreadClasspath()
    {
        try
        {
            ClassWorld world = new ClassWorld();

            // use the existing ContextClassLoader in a realm of the classloading space
            ClassRealm root = world.newRealm( "gwt-plugin", Thread.currentThread().getContextClassLoader() );
            ClassRealm realm = root.createChildRealm( "gwt-project" );

            for ( Iterator it = BuildClasspathUtil.buildClasspathList( this, DependencyScope.COMPILE ).iterator(); it.hasNext(); )
            {
                realm.addConstituent( ( (File) it.next() ).toURI().toURL() );
            }

            Thread.currentThread().setContextClassLoader( realm.getClassLoader() );
            // /System.out.println("AbstractGwtMojo realm classloader = " + realm.getClassLoader().toString());

            return realm.getClassLoader();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw new RuntimeException( e );
        }
    }

    //
    // accessors/mutators
    //

    public void setBuildDir( File buildDir )
    {
        this.buildDir = buildDir;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getBuildDir()
     */
    public File getBuildDir()
    {
        return this.buildDir;
    }

    public void setCompileTarget( String[] compileTarget )
    {
        this.compileTarget = compileTarget;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getCompileTarget()
     */
    public String[] getCompileTarget()
    {
        return this.compileTarget;
    }

    public void setContextXml( File contextXml )
    {
        this.contextXml = contextXml;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getContextXml()
     */
    public File getContextXml()
    {
        return this.contextXml;
    }

    public void setExtraJvmArgs( String extraJvmArgs )
    {
        this.extraJvmArgs = extraJvmArgs;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getExtraJvmArgs()
     */
    public String getExtraJvmArgs()
    {
        return this.extraJvmArgs;
    }

    public void setGen( File gen )
    {
        this.gen = gen;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getGen()
     */
    public File getGen()
    {
        return this.gen;
    }

    public void setGwtHome( File gwtHome )
    {
        this.gwtHome = gwtHome;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getGwtHome()
     */
    public File getGwtHome()
    {
        return this.gwtHome;
    }

    public void setLogLevel( String logLevel )
    {
        this.logLevel = logLevel;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getLogLevel()
     */
    public String getLogLevel()
    {
        return this.logLevel;
    }

    public void setNoServer( boolean noServer )
    {
        this.noServer = noServer;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#isNoServer()
     */
    public boolean isNoServer()
    {
        return this.noServer;
    }

    public void setOutput( File output )
    {
        this.output = output;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getOutput()
     */
    public File getOutput()
    {
        return this.output;
    }

    public void setPort( int port )
    {
        this.port = port;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getPort()
     */
    public int getPort()
    {
        return this.port;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getProject()
     */
    public MavenProject getProject()
    {
        return this.project;
    }

    public void setRunTarget( String runTarget )
    {
        this.runTarget = runTarget;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getRunTarget()
     */
    public String getRunTarget()
    {
        return this.runTarget;
    }

    public void setStyle( String style )
    {
        this.style = style;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getStyle()
     */
    public String getStyle()
    {
        return this.style;
    }

    public void setTomcat( File tomcat )
    {
        this.tomcat = tomcat;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getTomcat()
     */
    public File getTomcat()
    {
        return this.tomcat;
    }

    public void setWebXml( File webXml )
    {
        this.webXml = webXml;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getWebXml()
     */
    public File getWebXml()
    {
        return this.webXml;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#isWebXmlServletPathAsIs()
     */
    public boolean isWebXmlServletPathAsIs()
    {
        return this.webXmlServletPathAsIs;
    }

    public void setWebXmlServletPathAsIs( boolean webXmlServletPathAsIs )
    {
        this.webXmlServletPathAsIs = webXmlServletPathAsIs;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getShellServletMappingURL()
     */
    public String getShellServletMappingURL()
    {
        return this.shellServletMappingURL;
    }

    public void setShellServletMappingURL( String shellServletMappingURL )
    {
        this.shellServletMappingURL = shellServletMappingURL;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getGeneratorRootClasses()
     */
    public String[] getGeneratorRootClasses()
    {
        return this.generatorRootClasses;
    }

    public void setGeneratorRootClasses( String[] generatorRootClasses )
    {
        this.generatorRootClasses = generatorRootClasses;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getGeneratorDestinationPackage()
     */
    public String getGeneratorDestinationPackage()
    {
        return this.generatorDestinationPackage;
    }

    public void setGeneratorDestinationPackage( String generatorDestinationPackage )
    {
        this.generatorDestinationPackage = generatorDestinationPackage;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#isGenerateGettersAndSetters()
     */
    public boolean isGenerateGettersAndSetters()
    {
        return this.generateGettersAndSetters;
    }

    public void setGenerateGettersAndSetters( boolean generateGettersAndSetters )
    {
        this.generateGettersAndSetters = generateGettersAndSetters;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#isGeneratePropertyChangeSupport()
     */
    public boolean isGeneratePropertyChangeSupport()
    {
        return this.generatePropertyChangeSupport;
    }

    public void setGeneratePropertyChangeSupport( boolean generatePropertyChangeSupport )
    {
        this.generatePropertyChangeSupport = generatePropertyChangeSupport;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#isOverwriteGeneratedClasses()
     */
    public boolean isOverwriteGeneratedClasses()
    {
        return this.overwriteGeneratedClasses;
    }

    public void setOverwriteGeneratedClasses( boolean overwriteGeneratedClasses )
    {
        this.overwriteGeneratedClasses = overwriteGeneratedClasses;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getDebugPort()
     */
    public int getDebugPort()
    {
        return this.debugPort;
    }

    public void setDebugPort( int debugPort )
    {
        this.debugPort = debugPort;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#isDebugSuspend()
     */
    public boolean isDebugSuspend()
    {
        return this.debugSuspend;
    }

    public void setDebugSuspend( boolean debugSuspend )
    {
        this.debugSuspend = debugSuspend;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getGwtVersion()
     */
    public String getGwtVersion()
    {
        return this.gwtVersion;
    }

    public void setGwtVersion( String gwtVersion )
    {
        this.gwtVersion = gwtVersion;
    }

    public void setCompileTargets( String[] targets )
    {
        this.compileTarget = targets;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getTestFilter()
     */
    public String getTestFilter()
    {
        return this.testFilter;
    }

    public void setTestFilter( String testFilter )
    {
        this.testFilter = testFilter;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getSourcesOnPath()
     */
    public boolean getSourcesOnPath()
    {
        return this.sourcesOnPath;
    }

    public void setSourcesOnPath( boolean value )
    {
        this.sourcesOnPath = value;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getResourcesOnPath()
     */
    public boolean getResourcesOnPath()
    {
        return resourcesOnPath;
    }

    public void setResourcesOnPath( boolean resourcesOnPath )
    {
        this.resourcesOnPath = resourcesOnPath;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#isEnableAssertions()
     */
    public boolean isEnableAssertions()
    {
        return this.enableAssertions;
    }

    public void setEnableAssertions( boolean enableAssertions )
    {
        this.enableAssertions = enableAssertions;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getPluginClasspathList()
     */
    public List getPluginClasspathList()
    {
        return this.pluginClasspathList;
    }

    public void setPluginClasspathList( List pluginClasspathList )
    {
        this.pluginClasspathList = pluginClasspathList;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getArtifactFactory()
     */
    public org.apache.maven.artifact.factory.ArtifactFactory getArtifactFactory()
    {
        return this.artifactFactory;
    }

    public void setArtifactFactory( org.apache.maven.artifact.factory.ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getResolver()
     */
    public org.apache.maven.artifact.resolver.ArtifactResolver getResolver()
    {
        return this.resolver;
    }

    public void setResolver( org.apache.maven.artifact.resolver.ArtifactResolver resolver )
    {
        this.resolver = resolver;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getLocalRepository()
     */
    public org.apache.maven.artifact.repository.ArtifactRepository getLocalRepository()
    {
        return this.localRepository;
    }

    public void setLocalRepository( org.apache.maven.artifact.repository.ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getRemoteRepositories()
     */
    public java.util.List getRemoteRepositories()
    {
        return this.remoteRepositories;
    }

    public void setRemoteRepositories( java.util.List remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getI18nOutputDir()
     */
    public File getI18nOutputDir()
    {
        return this.i18nOutputDir;
    }

    public void setI18nOutputDir( File outputDir )
    {
        this.i18nOutputDir = outputDir;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getI18nMessagesNames()
     */
    public String[] getI18nMessagesNames()
    {
        return this.i18nMessagesNames;
    }

    public void setI18nMessagesNames( String[] messagesNames )
    {
        this.i18nMessagesNames = messagesNames;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getI18nConstantsNames()
     */
    public String[] getI18nConstantsNames()
    {
        return this.i18nConstantsNames;
    }

    public void setI18nConstantsNames( String[] constantsNames )
    {
        this.i18nConstantsNames = constantsNames;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getExtraTestArgs()
     */
    public String getExtraTestArgs()
    {
        return this.extraTestArgs;
    }

    public void setExtraTestArgs( String extraTestArgs )
    {
        this.extraTestArgs = extraTestArgs;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#isTestSkip()
     */
    public boolean isTestSkip()
    {
        return this.testSkip;
    }

    public void setTestSkip( boolean skip )
    {
        this.testSkip = skip;
    }

}
