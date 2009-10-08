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

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Properties;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.gwt.shell.ArtifactNameUtil;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @version $Id$
 */
public class GwtRuntime
{
    /** The gwt-user jar used at runtime */
    private final File gwtUserJar;

    /** The gwt-dev-[platform] jar used at runtime */
    private final File gwtDevJar;

    /** The SOYC jar (since GWT 2.0) */
    private final File soycJar;

    /** The gwt version we are running */
    private final GwtVersion version;

    public GwtRuntime( File gwtHome ) throws MojoExecutionException
    {
        super();
        if ( !gwtHome.exists() )
        {
            throw new MojoExecutionException( "Invalid GWT home : " + gwtHome );
        }
        gwtUserJar = new File( gwtHome, "gwt-user.jar" );
        if ( !gwtUserJar.exists() )
        {
            throw new MojoExecutionException( "Invalid GWT home : " + gwtHome );
        }
        this.gwtDevJar = new File( gwtHome, ArtifactNameUtil.guessDevJarName() );
        if ( !gwtDevJar.exists() )
        {
            throw new MojoExecutionException( "Invalid GWT home : " + gwtHome );
        }
        this.version = GwtVersion.fromMavenVersion( readGwtDevVersion( gwtDevJar ) );
        if ( version.compareTo( GwtVersion.TWO_DOT_ZERO ) >= 0 )
        {
            soycJar = new File( gwtHome, "gwt-soyc-vis.jar" );
            if ( !soycJar.exists() )
            {
                throw new MojoExecutionException( "Invalid GWT home : " + gwtHome );
            }
        }
        else
        {
            this.soycJar = null;
        }
    }

    /**
     * @param gwtUserJar gwt user library
     * @param gwtDevJar gwt dev library
     * @param version gwt version
     */
    public GwtRuntime( String version, GwtArtifactResolver resolver )
    throws MojoExecutionException
    {
        this.version = GwtVersion.fromMavenVersion( version );

        Artifact gwtUser = resolver.resolve( "gwt-user", version, "jar", null );
        this.gwtUserJar = gwtUser.getFile();

        boolean supportOOPHM = this.version.supportOOPHM();

        String platformVersion = supportOOPHM ? null : ArtifactNameUtil.getPlatformName();
        Artifact gwtDev = resolver.resolve( "gwt-dev", version, "jar", platformVersion );
        this.gwtDevJar = gwtDev.getFile();

        if ( !supportOOPHM )
        {
            Artifact gwtNatives =
                resolver.resolve( "gwt-dev", version, "zip", ArtifactNameUtil.getPlatformName() + "-libs" );
            unpackNativeLibraries( gwtNatives.getFile(), resolver.getArchiverManager() );
        }

        if ( this.version.supportSOYC() )
        {
            Artifact soyc = resolver.resolve( "gwt-soyc-vis", version, "jar", null );
            this.soycJar = soyc.getFile();
        }
        else
        {
            this.soycJar = null;
        }
    }

    /**
     * Read the GWT version from the About class present in gwt-dev JAR
     *
     * @param gwtDevJar gwt platform-dependent developer library
     * @return version declared in dev library
     */
    private static String readGwtDevVersion( File gwtDevJar )
    {
        try
        {
            try
            {
                // Try to get version from About.properties - typically for unreleased version of gwt-dev.jar
                URL aboutPropreties = new URL( "jar:" + gwtDevJar.toURL() + "!/com/google/gwt/dev/About.properties" );
                Properties props = new Properties();
                props.load( aboutPropreties.openStream() );
                return props.getProperty( "gwt.version" );
            }
            catch ( FileNotFoundException e )
            {
                // No About.propertiesn this may be a released jar
            }

            // Read About.GWT_VERSION_NUM constant on a released version of gwt-dev.jar
            URL about = new URL( "jar:" + gwtDevJar.toURL() + "!/com/google/gwt/dev/About.class" );
            ClassParser parser = new ClassParser( about.openStream(), "About.class" );
            JavaClass clazz = parser.parse();
            for ( org.apache.bcel.classfile.Field field : clazz.getFields() )
            {
                if ( "GWT_VERSION_NUM".equals( field.getName() ) )
                {
                    // Return the constant value between quotes
                    String constant = field.getConstantValue().toString();
                    return constant.substring( 1, constant.length() - 1 );
                }
            }
            throw new IllegalStateException( "Failed to retrieve GWT_VERSION_NUM in " + gwtDevJar.getName()
                + " 'About' class" );

            // Can't get this to work as expected, always return maven dependency "1.5.3" :'-(
            // ClassLoader cl = new URLClassLoader( new URL[] { gwtDevJar.toURL() },
            // ClassLoader.getSystemClassLoader()
            // );
            // Class<?> about = cl.loadClass( "com.google.gwt.dev.About" );
            // Field versionNumber = about.getField( "GWT_VERSION_NUM" );
            // String version = versionNumber.get( about ).toString();
            // return version;
        }
        catch ( Exception ex )
        {
            throw new IllegalStateException( "Failed to read gwt-dev version from " + gwtDevJar.getAbsolutePath() );
        }
    }

    public File getGwtUserJar()
    {
        return gwtUserJar;
    }

    public File getGwtDevJar()
    {
        return gwtDevJar;
    }

    public File getSoycJar()
    {
        if ( version.compareTo( GwtVersion.TWO_DOT_ZERO ) < 0 )
        {
            throw new IllegalStateException( "Cannot use SOYC with GWT SDK prior to 2.0" );
        }
        return soycJar;
    }

    private static void unpackNativeLibraries( File nativeLibs, ArchiverManager archiverManager )
        throws MojoExecutionException
    {
        try
        {
            UnArchiver unArchiver = archiverManager.getUnArchiver( nativeLibs );
            unArchiver.setSourceFile( nativeLibs );
            unArchiver.setDestDirectory( nativeLibs.getParentFile() );
            unArchiver.extract();
            unArchiver.setOverwrite( false );
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "Failed to retrieve Archiver implementation", e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Failed to unpack native libs", e );
        }
    }

    public GwtVersion getVersion()
    {
        return version;
    }
}
