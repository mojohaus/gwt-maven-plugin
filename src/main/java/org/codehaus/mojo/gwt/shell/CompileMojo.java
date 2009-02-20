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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.mojo.gwt.shell.scripting.ScriptWriter;

/**
 * Invokes the GWTCompiler for the project source.
 *
 * @goal compile
 * @phase process-classes
 * @requiresDependencyResolution compile
 * @description Invokes the GWTCompiler for the project source.
 * @version $Id$
 * @author cooper
 * @author ccollins
 */
public class CompileMojo
    extends AbstractGwtShellMojo
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
        ScriptWriter script = scriptWriterFactory.getScript();

        script.createScript( this, "compile" );

        for ( String target : getModules() )
        {
            String clazz = runtime.getVersion().getCompilerFQCN();
            script.executeClass( this, runtime, ScriptWriter.FORKBOOTER, clazz );
            script.print( " -gen \"" );
            script.print( getGen().getAbsolutePath() );
            script.print( "\" -logLevel " );
            script.print( getLogLevel() );
            script.print( " -style " );
            script.print( getStyle() );

            switch ( runtime.getVersion() )
            {
                case ONE_DOT_FOUR:
                case ONE_DOT_FIVE:
                    script.print( " -out " );
                    script.print( "\"" + getOutput().getAbsolutePath() + "\"" );
                    break;
                default:
                    script.print( " -war " );
                    script.print( "\"" + getOutput().getAbsolutePath() + "\"" );
                    script.print( " -localWorkers " );
                    script.print( String.valueOf( Runtime.getRuntime().availableProcessors() ) );
                    break;
            }

            script.print( " " );

            if ( isEnableAssertions() )
            {
                script.print( " -ea " );
            }

            script.print( target );
            script.println();
        }

        // run it
        runScript( script.getExecutable() );
    }

}
