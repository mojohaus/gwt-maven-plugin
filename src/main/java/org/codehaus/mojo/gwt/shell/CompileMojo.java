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

import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.GwtModule;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.mojo.gwt.GwtVersion;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SingleTargetSourceMapping;

/**
 * Invokes the GWTCompiler for the project source.
 *
 * @phase process-classes
 * @goal compile
 * @requiresDependencyResolution compile
 * @version $Id$
 * @author cooper
 * @author ccollins
 * @author <a href="mailto:nicolas@apache.org">Nicolas De loof</a>
 */
// @phase prepare-package should be even better to avoid unecessary gwt:compile when used with m2eclipse
public class CompileMojo
    extends AbstractGwtShellMojo
{
    /**
     * @parameter expression="${gwt.compiler.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * Don't try to detect if GWT compilation is up-to-date and can be skipped.
     *
     * @parameter expression="${gwt.compiler.force}" default-value="false"
     */
    private boolean force;

    /**
     * On GWT 1.6+, number of parallel processes used to compile GWT premutations. Defaults to
     * platform available processors number.
     * @parameter
     */
    private int localWorkers;

    /**
     * Whether or not to enable assertions in generated scripts (-ea).
     *
     * @parameter default-value="false"
     */
    private boolean enableAssertions;

    /**
     * Ask GWT to create the Story of Your Compile (SOYC). Enabled by default if GWT >= 2.0 is detected
     *
     * @parameter expression="${gwt.compiler.soyc}"
     */
    private String soyc;

    /**
     * Logs output in a graphical tree view.
     * <p>
     * Can be set from command line using '-Dgwt.treeLogger=true'.
     *
     * @parameter default-value="false" expression="${gwt.treeLogger}"
     */
    private boolean treeLogger;

    /**
     * EXPERIMENTAL: Disables some java.lang.Class methods (e.g. getName()).
     * <p>
     * Can be set from command line using '-Dgwt.disableClassMetadata=true'.
     *
     * @parameter default-value="false" expression="${gwt.disableClassMetadata}"
     */
    private boolean disableClassMetadata;

    /**
     * EXPERIMENTAL: Disables run-time checking of cast operations.
     * <p>
     * Can be set from command line using '-Dgwt.disableCastChecking=true'.
     *
     * @parameter default-value="false" expression="${gwt.disableCastChecking}"
     */
    private boolean disableCastChecking;

    /**
     * Validate all source code, but do not compile.
     * <p>
     * Can be set from command line using '-Dgwt.validateOnly=true'.
     *
     * @parameter default-value="false" expression="${gwt.validateOnly}"
     */
    private boolean validateOnly;

    /**
     * Enable faster, but less-optimized, compilations.
     * <p>
     * Can be set from command line using '-Dgwt.draftCompile=true'.
     *
     * @parameter default-value="false" expression="${gwt.draftCompile}"
     */
    private boolean draftCompile;

    /**
     * The directory into which extra, non-deployed files will be written.
     *
     * @parameter default-value="${project.build.directory}/extra"
     */
    private File extra;

    public void doExecute( GwtRuntime runtime )
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip || "pom".equals( getProject().getPackaging() ) )
        {
            getLog().info( "GWT compilation is skipped" );
            return;
        }

        if ( !this.getOutputDirectory().exists() )
        {
            this.getOutputDirectory().mkdirs();
        }

        String[] modules = getModules();
        if ( runtime.getVersion().supportMultiModuleCompile() )
        {
            compile( runtime, modules );
        }
        else
        {
            for ( String module : modules )
            {
                compile( runtime, new String[] { module } );
            }
        }

    }

    private void compile( GwtRuntime runtime, String[] modules )
        throws MojoExecutionException
    {
        boolean upToDate = true;
        GwtVersion gwtVersion = runtime.getVersion();

        String clazz = gwtVersion.getCompilerFQCN();
        JavaCommand cmd = new JavaCommand( clazz, runtime )
            .withinScope( Artifact.SCOPE_COMPILE )
            .arg( "-gen" )
            .arg( getGen().getAbsolutePath() )
            .arg( "-logLevel" )
            .arg( getLogLevel() )
            .arg( "-style" )
            .arg( getStyle() )
            .arg( enableAssertions, "-ea" )
            .arg( gwtVersion.getWebOutputArgument() )
            .arg( getOutputDirectory().getAbsolutePath() );

        if ( gwtVersion.supportParallelCompile() )
        {
            cmd.arg( "-localWorkers" )
               .arg( String.valueOf( getLocalWorkers() ) );
        }

        if ( gwtVersion.supportSOYC() )
        {
            if ( soyc != null && Boolean.valueOf( soyc ).booleanValue() == false )
            {
                getLog().debug( "SOYC has been disabled by user" );
            }
            else
            {
                cmd.arg( "-soyc" )
                   .arg( "-extra")
                   .arg( extra.getAbsolutePath() );
                extra.mkdirs();
            }
        }

        if ( gwtVersion.compareTo( GwtVersion.TWO_DOT_ZERO ) >= 0 )
        {
            cmd.arg( draftCompile, "-draftCompile" )
               .arg( validateOnly, "-validateOnly" )
               .arg( treeLogger, "-treeLogger" )
               .arg( disableClassMetadata, "-XdisableClassMetadata" )
               .arg( disableCastChecking, "-XdisableCastChecking" );
        }

        for ( String target : modules )
        {
            if ( !compilationRequired( target, getOutputDirectory() ) )
            {
                getLog().info( target + " is up to date. GWT compilation skipped" );
                continue;
            }
            cmd.arg( target );
            upToDate = false;
        }
        if ( !upToDate )
        {
            cmd.execute();
        }
    }

    private int getLocalWorkers()
    {
        if ( localWorkers > 0 )
        {
            return localWorkers;
        }
        return Runtime.getRuntime().availableProcessors();
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
        GwtModule gwtModule = readModule( module );
        if ( gwtModule.getEntryPoints().length == 0 )
        {
            // No entry-point, this is a secondary module : compiling this one will fail
            // with '[ERROR] Module has no entry points defined'
            return false;
        }

        if ( force )
        {
            return true;
        }

        String modulePath = gwtModule.getPath();
        String outputTarget = modulePath + "/" + modulePath + ".nocache.js";

        // Require compilation if no js file present in target.
        if ( !new File( output, outputTarget ).exists() )
        {
            return true;
        }

        // js file allreay exists, but may not be up-to-date with project source files
        SingleTargetSourceMapping singleTargetMapping = new SingleTargetSourceMapping( ".java", outputTarget );
        StaleSourceScanner scanner = new StaleSourceScanner();
        scanner.addSourceMapping( singleTargetMapping );

        SingleTargetSourceMapping gwtModuleMapping = new SingleTargetSourceMapping( ".gwt.xml", outputTarget );
        scanner.addSourceMapping( gwtModuleMapping );

        Collection<File> compileSourceRoots = new HashSet<File>();
        classpathBuilder.addSourcesWithActiveProjects( getProject(), compileSourceRoots, SCOPE_COMPILE );
        classpathBuilder.addResourcesWithActiveProjects( getProject(), compileSourceRoots, SCOPE_COMPILE );
        for ( File sourceRoot : compileSourceRoots )
        {
            if ( !sourceRoot.isDirectory() )
            {
                continue;
            }
            try
            {
                if ( !scanner.getIncludedSources( sourceRoot, output ).isEmpty() )
                {
                    getLog().debug( "found stale source in " + sourceRoot + " compared with " + output );
                    return true;
                }
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
