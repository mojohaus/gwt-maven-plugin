package org.codehaus.mojo.gwt;

/*
 * Copyright 2006- org.codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

/**
 * @author ndeloof
 *
 */
public abstract class AbstractGwtMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * @return the project classloader
     */
    protected ClassLoader getProjectClassLoader()
        throws DependencyResolutionRequiredException, MalformedURLException
    {
        getLog().debug( "GeneratedDtoMojo#getProjectClassLoader()" );

        List compile = project.getCompileClasspathElements();
        URL[] urls = new URL[compile.size()];
        int i = 0;
        for ( Iterator iterator = compile.iterator(); iterator.hasNext(); )
        {
            Object object = (Object) iterator.next();
            if ( object instanceof Artifact )
            {
                urls[i] = ( (Artifact) object ).getFile().toURL();
            }
            else
            {
                urls[i] = new File( (String) object ).toURL();
            }
            i++;
        }
        return
            new URLClassLoader( urls, getClass().getClassLoader().getSystemClassLoader() );
    }

    protected void addCompileSourceRoot( String path )
    {
        project.addCompileSourceRoot( path );
    }

}
