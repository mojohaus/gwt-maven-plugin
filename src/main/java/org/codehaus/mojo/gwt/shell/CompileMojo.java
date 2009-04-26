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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SingleTargetSourceMapping;

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
            if ( !compilationRequired( target, getOutput() ) )
            {
                getLog().info( target + " is up to date. GWT compilation skipped" );
                continue;
            }

            String clazz = runtime.getVersion().getCompilerFQCN();
            JavaCommand cmd = new JavaCommand( clazz, runtime )
                .withinScope( Artifact.SCOPE_COMPILE )
                .arg( "-gen" )
                .arg( quote( getGen().getAbsolutePath() ) )
                .arg( "-logLevel" )
                .arg( getLogLevel() )
                .arg( "-style" )
                .arg( getStyle() )
                .arg( isEnableAssertions(), "-ea" );

            switch ( runtime.getVersion() )
            {
                case ONE_DOT_FOUR:
                case ONE_DOT_FIVE:
                    cmd.arg( "-out" )
                        .arg( quote( getOutput().getAbsolutePath() ) );
                    break;
                default:
                    cmd.arg( "-war" )
                        .arg( quote( getOutput().getAbsolutePath() ) )
                        .arg( "-localWorkers" )
                        .arg( String.valueOf( Runtime.getRuntime().availableProcessors() ) );
                    break;
            }
            cmd.arg( target ).execute();
        }
    }

    /**
     * Try to find out, if there are stale sources. If aren't some, we don't have to compile... ...this heuristic
     * doesn't take into account, that there could be updated dependencies. But for this case, as 'clean compile' could
     * be executed which would force a compilation.
     * 
     * @param module Name of the GWT module to compile
     * @param output Output path
     * @return true if compilation is required (i.e. stale sources are found)
     * @throws MojoExecutionException When sources scanning fails
     * @author Alexander Gordt
     */
    private boolean compilationRequired( String module, File output )
        throws MojoExecutionException
    {
        String outputTarget = module + "/" + module + ".nocache.js";
        SingleTargetSourceMapping singleTargetMapping = new SingleTargetSourceMapping( ".java", outputTarget );
        StaleSourceScanner scanner = new StaleSourceScanner();
        scanner.addSourceMapping( singleTargetMapping );

        for ( Object sourceRoot : getProject().getCompileSourceRoots() )
        {
            File rootFile = new File( sourceRoot.toString() );
            if ( !rootFile.isDirectory() )
            {
                continue;
            }
            try
            {
                return !scanner.getIncludedSources( rootFile, output ).isEmpty();
            }
            catch ( InclusionScanException e )
            {
                throw new MojoExecutionException( "Error scanning source root: \'" + sourceRoot + "\' "
                    + "for stale files to recompile.", e );
            }
        }
        return false;
    }
}
