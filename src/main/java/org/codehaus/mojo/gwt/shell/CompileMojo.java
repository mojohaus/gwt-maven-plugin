/*
 * CompileMojo.java
 *
 * Created on January 13, 2007, 11:42 AM
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 *
 */
package org.codehaus.mojo.gwt.shell;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.mojo.gwt.shell.scripting.CompileScriptConfiguration;
import org.codehaus.mojo.gwt.shell.scripting.ScriptUtil;
import org.codehaus.mojo.gwt.shell.scripting.ScriptWriter;

/**
 * Invokes the GWTCompiler for the project source.
 * 
 * @goal gwtcompile
 * @phase process-classes
 * @requiresDependencyResolution compile
 * @description Invokes the GWTCompiler for the project source.
 * @author cooper
 * @author ccollins
 */
public class CompileMojo
    extends AbstractGwtShellMojo
    implements CompileScriptConfiguration
{

    /** Creates a new instance of CompileMojo */
    public CompileMojo() {
        super();
    }

    public void doExecute(GwtRuntime runtime)
        throws MojoExecutionException, MojoFailureException
    {

        if (!this.getOutput().exists()) {
            this.getOutput().mkdirs();
        }

        // build it for the correct platform
        ScriptWriter writer = scriptWriterFactory.getScriptWriter();
        File exec = writer.writeCompileScript( this, runtime );

        // run it
        ScriptUtil.runScript(exec);
    }

    /**
     * Helper hack for classpath problems, used as a fallback.
     * @param runtime TODO
     *
     * @return
     */
    protected ClassLoader fixThreadClasspath( GwtRuntime runtime )
    {
        try
        {
            ClassWorld world = new ClassWorld();
    
            // use the existing ContextClassLoader in a realm of the classloading space
            ClassRealm root = world.newRealm( "gwt-plugin", Thread.currentThread().getContextClassLoader() );
            ClassRealm realm = root.createChildRealm( "gwt-project" );
    
            Collection classpath =
                buildClasspathUtil.buildClasspathList( getProject(), Artifact.SCOPE_COMPILE, runtime, sourcesOnPath,
                                                       resourcesOnPath );
            for ( Iterator it = classpath.iterator(); it.hasNext(); )
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
}
