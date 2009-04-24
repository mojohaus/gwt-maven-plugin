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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.GwtRuntime;

/**
 * Invokes the GWTCompiler for the project source.
 *
 * @goal compile
 * @phase process-classes
 * @requiresDependencyResolution compile
 * @version $Id$
 * @author cooper
 * @author ccollins
 */
// @phase prepare-package should be even better to avoid unecessary gwt:compile when used with m2eclipse
public class CompileMojo
    extends AbstractGwtShellMojo
{
    /**
     * @parameter expression="${gwt.compiler.skip}" default-value="false"
     */
    private boolean skip;

    /** Creates a new instance of CompileMojo */
    public CompileMojo()
    {
        super();
    }

    public void doExecute( GwtRuntime runtime )
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip )
        {
            getLog().info( "GWT compilation is skipped" );
            return;
        }

        if ( !this.getOutput().exists() )
        {
            this.getOutput().mkdirs();
        }
        for ( String target : getModules() )
        {
            String clazz = runtime.getVersion().getCompilerFQCN();
            List<String> args = new ArrayList<String>();
            args.add( "-gen" );
            args.add( quote( getGen().getAbsolutePath() ) );
            args.add( "-logLevel" );
            args.add( getLogLevel() );
            args.add( "-style" );
            args.add( getStyle() );

            switch ( runtime.getVersion() )
            {
                case ONE_DOT_FOUR:
                case ONE_DOT_FIVE:
                    args.add( "-out" );
                    args.add( quote( getOutput().getAbsolutePath() ) );
                    break;
                default:
                    args.add( "-war" );
                    args.add( quote( getOutput().getAbsolutePath() ) );
                    args.add( "-localWorkers" );
                    args.add( String.valueOf( Runtime.getRuntime().availableProcessors() ) );
                    break;
            }

            if ( isEnableAssertions() )
            {
                args.add( "-ea" );
            }

            args.add( target );
            execute( clazz, Artifact.SCOPE_COMPILE, runtime, args, null );
        }
    }
}
