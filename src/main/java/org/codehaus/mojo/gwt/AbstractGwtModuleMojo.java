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
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @version $Id$
 */
public abstract class AbstractGwtModuleMojo
    extends AbstractGwtMojo
{

    /**
     *
     */
    public static final String GWT_MODULE_EXTENSION = ".gwt.xml";

    /**
     * The project GWT modules. If not set, the plugin will scan the project for <code>.gwt.xml</code> files.
     * 
     * @parameter
     * @alias compileTargets
     */
    private String[] modules;

    /**
     * A single GWT module (Shortcut for modules)
     * 
     * @parameter expression="${gwt.module}"
     */
    private String module; // NOPMD

    /**
     *
     */
    public AbstractGwtModuleMojo()
    {
        super();
    }

    /**
     * @param module the module to set
     */
    public void setModule( String module )
    {
        // Note : Plexus will use this setter to inject dependency. The 'module' attribute is unused
        this.modules = new String[] { module };
    }

    /**
     * Return the configured modules or scan the project source/resources folder to find them
     *
     * @return the modules
     */
    @SuppressWarnings( "unchecked" )
    public String[] getModules()
    {
        // module has higher priority if set by expression
        if ( module != null )
        {
            return new String[] { module };
        }
        if ( modules == null )
        {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( getProject().getBuild().getSourceDirectory() );
            scanner.setIncludes( new String[] { "**/*" + GWT_MODULE_EXTENSION } );
            scanner.scan();

            // Use a Set to avoid duplicate when user set src/main/java as <resource>
            Set<String> mods = new HashSet<String>();
            mods.addAll( Arrays.asList( scanner.getIncludedFiles() ) );

            Collection<Resource> resources = (Collection<Resource>) getProject().getResources();
            for ( Resource resource : resources )
            {
                File resourceDirectoryFile = new File( resource.getDirectory() );
                if ( !resourceDirectoryFile.exists() )
                {
                    continue;
                }
                scanner = new DirectoryScanner();
                scanner.setBasedir( resource.getDirectory() );
                scanner.setIncludes( new String[] { "**/*" + GWT_MODULE_EXTENSION } );
                scanner.scan();
                mods.addAll( Arrays.asList( scanner.getIncludedFiles() ) );
            }

            if ( mods.isEmpty() )
            {
                getLog().warn( "GWT plugin is configured to detect modules, but none where found." );
            }

            modules = new String[mods.size()];
            int i = 0;
            for ( String fileName : mods )
            {
                String path = fileName.substring( 0, fileName.length() - GWT_MODULE_EXTENSION.length() );
                modules[i++] = path.replace( File.separatorChar, '.' );
            }
            if ( modules.length > 0 )
            {
                getLog().info( "auto discovered modules " + Arrays.asList( modules ) );
            }

        }
        return modules;
    }

    protected GwtModule readModule( String name )
        throws MojoExecutionException
    {
        Collection<String> sourceRoots = getProject().getCompileSourceRoots();
        for ( String sourceRoot : sourceRoots )
        {
            File root = new File( sourceRoot );
            File xml = new File( root, name.replace( '.', '/' ) + GWT_MODULE_EXTENSION );
            if ( xml.exists() )
            {
                getLog().debug( "GWT module " + name + " found in " + root );
                return readModule( name, xml );
            }
        }
        Collection<Resource> resources = (Collection<Resource>) getProject().getResources();
        for ( Resource resource : resources )
        {
            File root = new File( resource.getDirectory() );
            File xml = new File( root, name.replace( '.', '/' ) + GWT_MODULE_EXTENSION );
            if ( xml.exists() )
            {
                getLog().debug( "GWT module " + name + " found in " + root );
                return readModule( name, xml );
            }
        }
        throw new MojoExecutionException( "GWT Module " + name + " not found in project sources or resources." );
    }

    /**
     * @param module2
     * @return
     */
    private GwtModule readModule( String name, File xml )
        throws MojoExecutionException
    {
        try
        {
            Xpp3Dom dom = Xpp3DomBuilder.build( new FileReader( xml ) );
            return new GwtModule( name, dom );
        }
        catch ( Exception e )
        {
            String error = "Failed to read module XML file " + xml;
            getLog().error( error );
            throw new MojoExecutionException( error, e );
        }
    }

    /**
     * @param path file to add to the project compile directories
     */
    protected void addCompileSourceRoot( File path )
    {
        getProject().addCompileSourceRoot( path.getAbsolutePath() );
    }

}