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
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaSource;

/**
 * Goal which generate Asyn interface.
 * 
 * @goal generateAsync
 * @phase generate-sources
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class GenerateAsyncMojo
    extends AbstractGwtMojo
{
    /**
     * Location of the file.
     *
     * @parameter expression="${project.build.directory}/generated-sources/gwt
     * @required
     */
    private File generateDirectory;

    /**
     * Pattern for GWT service interface
     * 
     * @parameter default-value="**\/*Service.java"
     */
    private String servicePattern;

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException
    {
        getLog().debug( "GenerateAsyncMojo#execute()" );

        List sourceRoots = getProject().getCompileSourceRoots();
        for ( Iterator iterator = sourceRoots.iterator(); iterator.hasNext(); )
        {
            String sourceRoot = (String) iterator.next();
            try
            {
                scanAndGenerateAsync( new File( sourceRoot ) );
            }
            catch ( Exception e )
            {
                getLog().error( "Failed to generate Async interface", e );
            }
        }
        getProject().addCompileSourceRoot( generateDirectory.getAbsolutePath() );
    }

    /**
     * @param file
     */
    private void scanAndGenerateAsync( File file )
        throws Exception
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( file );
        scanner.setIncludes( new String[] { servicePattern } );
        scanner.scan();
        String[] sources = scanner.getIncludedFiles();
        for ( int i = 0; i < sources.length; i++ )
        {
            File source = new File( file, sources[i] );
            generateAsync( source, sources[i] );
        }
    }

    /**
     * @param source
     */
    private void generateAsync( File source, String name )
        throws Exception
    {
        JavaDocBuilder builder = new JavaDocBuilder();
        builder.getClassLibrary().addClassLoader( getProjectClassLoader() );
        builder.addSource( new FileReader( source ) );
        name = name.substring( 0, name.length() - 5 ) + "Async";

        JavaClass clazz = builder.getClasses()[0];
        JavaClass[] implemented = clazz.getImplementedInterfaces();
        boolean isRemoteService = false;
        for ( int i = 0; i < implemented.length; i++ )
        {
            if ( "com.google.gwt.user.client.rpc.RemoteService".equals( implemented[i].getFullyQualifiedName() ) )
            {
                isRemoteService = true;
                break;
            }
        }
        if ( !isRemoteService )
        {
            return;
        }

        File out = new File( generateDirectory, name + ".java" );
        out.getParentFile().mkdirs();
        PrintWriter writer = new PrintWriter( out );

        JavaSource javaSource = builder.getSources()[0];
        writer.println( "package " + javaSource.getPackage() + ";" );
        writer.println();
        String[] imports = javaSource.getImports();
        for ( int i = 0; i < imports.length; i++ )
        {
            writer.println( "import " + imports[i] + ";" );
        }
        writer.println( "import com.google.gwt.user.client.rpc.AsyncCallback;" );
        writer.println();
        writer.println( "public interface " + clazz.getName() + "Async" );
        writer.println( "{" );

        JavaMethod[] methods = clazz.getMethods();
        for ( int i = 0; i < methods.length; i++ )
        {
            JavaMethod method = methods[i];
            writer.println( "" );
            writer.println( "    /**" );
            writer.println( "     * GWT-RPC service  asynchronous (client-side) interface" );
            writer.println( "     * @see " + clazz.getFullyQualifiedName() );
            writer.println( "     */" );
            writer.print( "    void " + method.getName() + "( " );
            JavaParameter[] params = method.getParameters();
            for ( int j = 0; j < params.length; j++ )
            {
                JavaParameter param = params[j];
                if ( j > 0 )
                {
                    writer.print( ", " );
                }
                writer.print( param.getType().getJavaClass().getName() + " " + param.getName() );
            }
            if ( params.length > 0 )
            {
                writer.print( ", " );
            }
            if ( method.getReturns().isVoid() )
            {
                writer.println( "AsyncCallback callback );" );
            }
            else
            {
                writer.println( "AsyncCallback<" + method.getReturns().getJavaClass().getName() + "> callback );" );
            }
            writer.println();
        }

        writer.println();
        writer.println( "}" );
        writer.close();
    }
}
