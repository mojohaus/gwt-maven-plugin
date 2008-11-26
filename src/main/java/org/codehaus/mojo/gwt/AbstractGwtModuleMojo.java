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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.maven.model.Resource;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public abstract class AbstractGwtModuleMojo
    extends AbstractGwtMojo
{

    /**
     *
     */
    private static final String GWT_MODULE_EXTENSION = ".gwt.xml";

    /**
     * A single GWT module (Shortcut for modules)
     * 
     * @parameter expression="${gwt.module}"
     */
    private String module; // NOPMD

    /**
     * The project GWT modules. If not set, the plugin will scan the project for <code>.gwt.xml</code> files.
     * 
     * @parameter
     * @alias compileTargets
     */
    private String[] modules;

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
        List < String > mods = new ArrayList < String > ();
        if ( modules == null )
        {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( project.getBuild().getSourceDirectory() );
            scanner.setIncludes( new String[] { "**/*" + GWT_MODULE_EXTENSION } );
            scanner.scan();
            mods.addAll( Arrays.asList( scanner.getIncludedFiles() ) );

            Collection < Resource > resources = ( Collection < Resource > ) project.getResources();
            for ( Resource resource : resources )
            {
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
        }
        return modules;
    }

}