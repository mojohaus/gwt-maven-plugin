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
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

/**
 * Copy GWT java source code and module descriptor as resources in the build outputDirectory. Alternative to declaring a
 * &lt;resource&gt; in the POM with finer filtering as the module descriptor is read to detect sources to be copied.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @goal resources
 * @phase process-resources
 */
public class GwtResourcesMojo
    extends AbstractGwtModuleMojo
{
    /**
     * @parameter expression="${project.build.outputDirectory}
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        for ( String name : getModules() )
        {
            GwtModule module = readModule( name );
            int count = 0;
            for ( String source : module.getSources() )
            {
                getLog().debug( "copy GWT sources from " + name + '.' + source );
                count += copyAsResources( module, source );
            }
            for ( String source : module.getSuperSources() )
            {
                getLog().debug( "copy GWT sources from " + name + '.' + source );
                count += copyAsResources( module, source );
            }
            getLog().info( count + " source files copied from GWT module " + name );
        }
    }

    /**
     * @param source
     * @param name
     */
    private int copyAsResources( GwtModule module, String source )
    throws MojoExecutionException
    {
        String targetPath = module.getPackage().replace( '.', '/' ) + '/' + module.getFile().getName();
        try
        {
            FileUtils.copyFile( module.getFile(), new File( outputDirectory, targetPath ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to copy GWT module descriptor", e );
        }

        String pattern = module.getPackage().replace( '.', '/' ) + '/';
        Set<String> paths = new HashSet<String>();
        paths.addAll( getProject().getCompileSourceRoots() );
        for ( Resource resource : (Collection<Resource>) getProject().getResources() )
        {
            paths.add( resource.getDirectory() );
        }

        int count = 0;
        for ( String path : paths )
        {
            File basedir = new File( path );
            // the default "src/main/resource" may not phisicaly exist in project
            if ( !basedir.exists() )
            {
                continue;
            }
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( basedir );
            scanner.setIncludes( new String[] { pattern + source + "/**/*.java" } );
            scanner.scan();
            String[] includedFiles = scanner.getIncludedFiles();
            for ( String included : includedFiles )
            {
                File f = new File( basedir, included );
                File target = new File( outputDirectory, included );
                try
                {
                    getLog().debug( "copy " + f + " to outputDirectory" );
                    target.getParentFile().mkdirs();
                    FileUtils.copyFile( f, target );
                    count++;
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Failed to copy GWT class source " + f, e );
                }
            }
        }


        return count;
    }

}