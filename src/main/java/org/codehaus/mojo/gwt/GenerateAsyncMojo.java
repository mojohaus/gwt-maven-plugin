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
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.Annotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaSource;

/**
 * Goal which generate Asyn interface.
 * 
 * @goal generateAsync
 * @phase generate-sources
 * @requiresDependencyResolution compile
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @version $Id$
 */
public class GenerateAsyncMojo
    extends AbstractGwtMojo
{
    private static final String REMOTE_SERVICE_INTERFACE = "com.google.gwt.user.client.rpc.RemoteService";

    private final static Map<String, String> WRAPPERS = new HashMap<String, String>();
    static
    {
        WRAPPERS.put( "boolean", Boolean.class.getName() );
        WRAPPERS.put( "byte", Byte.class.getName() );
        WRAPPERS.put( "char", Character.class.getName() );
        WRAPPERS.put( "short", Short.class.getName() );
        WRAPPERS.put( "int", Integer.class.getName() );
        WRAPPERS.put( "long", Long.class.getName() );
        WRAPPERS.put( "float", Float.class.getName() );
        WRAPPERS.put( "double", Double.class.getName() );
    }

    /**
     * Pattern for GWT service interface
     * 
     * @parameter default-value="**\/*Service.java"
     */
    private String servicePattern;

    /**
     * A (MessageFormat) Pattern to get the GWT-RPC servlet URL based on service interface name. For example to
     * "{0}.rpc" if you want to map GWT-RPC calls to "*.rpc" in web.xml, for example when using Spring dispatch servlet
     * to handle RPC requests.
     * 
     * @parameter default-value="{0}" expression="${gwt.rpcPattern}"
     */
    private String rpcPattern;

    /**
     * Stop the build on error
     * 
     * @parameter default-value="true" expression="${maven.gwt.failOnError}"
     */
    private boolean failOnError;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings( "unchecked" )
    public void execute()
        throws MojoExecutionException
    {
        getLog().debug( "GenerateAsyncMojo#execute()" );
        boolean supportJava5 = getGwtRuntime().getVersion().supportJava5();
        JavaDocBuilder builder = createJavaDocBuilder();

        List<String> sourceRoots = getProject().getCompileSourceRoots();
        boolean generated = false;
        for ( String sourceRoot : sourceRoots )
        {
            try
            {
                generated |= scanAndGenerateAsync( new File( sourceRoot ), builder, supportJava5 );
            }
            catch ( Throwable e )
            {
                getLog().error( "Failed to generate Async interface", e );
                if ( failOnError )
                {
                    throw new MojoExecutionException( "Failed to generate Async interface", e );
                }
            }
        }
        if ( generated )
        {
            getLog().debug( "add compile source root " + getGenerateDirectory() );
            addCompileSourceRoot( getGenerateDirectory() );
        }
    }

    /**
     * @param file the base directory to scan for RPC services
     * @return true if some file have been generated
     * @throws Exception generation failure
     */
    private boolean scanAndGenerateAsync( File file, JavaDocBuilder builder, boolean supportJava5 )
        throws Exception
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( file );
        scanner.setIncludes( new String[] { servicePattern } );
        scanner.scan();
        String[] sources = scanner.getIncludedFiles();
        if ( sources.length == 0 )
        {
            return false;
        }
        String[] classNames = getTopLevelClassNames( sources );
        boolean fileGenerated = false;
        for ( int i = 0; i < classNames.length; i++ )
        {
            JavaClass clazz = builder.getClassByName( classNames[i] );
            if ( isEligibleForGeneration( clazz ) )
            {
                String targetFileName = sources[i].substring( 0, sources[i].length() - 5 ) + "Async.java";
                File targetFile = new File( getGenerateDirectory(), targetFileName );
                targetFile.getParentFile().mkdirs();
                generateAsync( clazz, targetFile, supportJava5 );
                fileGenerated = true;
            }
        }
        return fileGenerated;
    }


    /**
     * @param source the RPC service java source file
     * @param name the service name
     * @throws Exception generation failure
     */
    private void generateAsync( JavaClass clazz, File targetFile, boolean supportJava5 )
        throws IOException
    {
        PrintWriter writer = new PrintWriter( targetFile ); // TODO: use source encoding instead of platform encoding

        String className = clazz.getName();
        JavaSource javaSource = clazz.getSource();
        writer.println( "package " + javaSource.getPackage().getName() + ";" );
        writer.println();
        String[] imports = javaSource.getImports();
        for ( String string : imports )
        {
            if ( !REMOTE_SERVICE_INTERFACE.equals( string ) )
            {
                writer.println( "import " + string + ";" );
            }
        }
        writer.println( "import com.google.gwt.core.client.GWT;" );
        writer.println( "import com.google.gwt.user.client.rpc.AsyncCallback;" );
        writer.println( "import com.google.gwt.user.client.rpc.ServiceDefTarget;" );

        writer.println();
        writer.println( "public interface " + className + "Async" );
        writer.println( "{" );

        JavaMethod[] methods = clazz.getMethods( true );
        for ( JavaMethod method : methods )
        {
            writer.println( "" );
            writer.println( "    /**" );
            writer.println( "     * GWT-RPC service  asynchronous (client-side) interface" );
            writer.println( "     * @see " + method.getParentClass().getFullyQualifiedName() );
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
                writer.print( fixClassName(param.getType().getGenericValue()) );
                if ( param.getType().isArray() )
                {
                    writer.print( "[]" );
                }
                writer.print( " " + param.getName() );
            }
            if ( params.length > 0 )
            {
                writer.print( ", " );
            }

            if ( supportJava5 )
            {
                if ( method.getReturns().isVoid() )
                {
                    writer.println( "AsyncCallback<Void> callback );" );
                }
                else if ( method.getReturns().isPrimitive() )
                {
                    String primitive = method.getReturns().getGenericValue();
                    writer.println( "AsyncCallback<" + WRAPPERS.get( primitive ) + "> callback );" );
                }
                else
                {
                    String type = fixClassName(method.getReturns().getGenericValue());
                    if ( method.getReturns().isArray() )
                    {
                        type += "[]";
                    }
                    writer.println( "AsyncCallback<" + type + "> callback );" );
                }
            }
            else
            {
                writer.println( "AsyncCallback callback );" );
            }

            writer.println();
        }

        writer.println();

        String uri = MessageFormat.format( rpcPattern, className );
        if ( clazz.getAnnotations() != null )
        {
            for ( Annotation annotation : clazz.getAnnotations() )
            {
                getLog().debug( "annotation found on service interface " + annotation );
                if ( annotation.getType().getValue().equals( "com.google.gwt.user.client.rpc.RemoteServiceRelativePath" ) )
                {
                    uri = annotation.getNamedParameter( "value" ).toString();
                    // remove quotes
                    uri = uri.substring( 1, uri.length() - 1 );
                    getLog().debug( "@RemoteServiceRelativePath annotation found on service interface " + uri );
                }
            }
        }

        writer.println( "    /**" );
        writer.println( "     * Utility class to get the RPC Async interface from client-side code" );
        writer.println( "     */" );
        writer.println( "    public static final class Util " );
        writer.println( "    { " );
        writer.println( "        private static " + className + "Async instance;" );
        writer.println();
        writer.println( "        public static final " + className + "Async getInstance()" );
        writer.println( "        {" );
        writer.println( "            if ( instance == null )" );
        writer.println( "            {" );
        writer.println( "                instance = (" + className + "Async) GWT.create( " + className + ".class );" );
        writer.println( "                ServiceDefTarget target = (ServiceDefTarget) instance;" );
        writer.println( "                target.setServiceEntryPoint( GWT.getModuleBaseURL() + \"" + uri + "\" );" );
        writer.println( "            }" );
        writer.println( "            return instance;" );
        writer.println( "        }" );
        writer.println( "" );
        writer.println( "        private Util()" );
        writer.println( "        {" );
        writer.println( "            // Utility class should not be instanciated" );
        writer.println( "        }" );
        writer.println( "    }" );

        writer.println( "}" );
        writer.close();
    }

    private boolean isEligibleForGeneration( JavaClass javaClass )
    {
        return javaClass.isInterface() && javaClass.isPublic() && javaClass.isA( REMOTE_SERVICE_INTERFACE );
    }

    private JavaDocBuilder createJavaDocBuilder()
        throws MojoExecutionException
    {
        try
        {
            JavaDocBuilder builder = new JavaDocBuilder();
            builder.getClassLibrary().addClassLoader( getProjectClassLoader() );
            for ( String sourceRoot : (List<String>) getProject().getCompileSourceRoots() )
            {
                builder.addSourceTree( new File( sourceRoot ) );
            }
            return builder;
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( "Failed to resolve project classpath", e );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Failed to resolve project classpath", e );
        }
    }

    private String[] getTopLevelClassNames( String[] sourceFiles )
    {
        String[] result = new String[sourceFiles.length];
        for ( int i = 0; i < sourceFiles.length; i++ )
        {
            String className = sourceFiles[i].substring( 0, sourceFiles[i].length() - 5 ); // strip ".java"
            className = className.replace( File.separatorChar, '.' );
            result[i] = className;
        }
        return result;
    }

    /**
     * Fix the class name returned by QDox model (e.g. fix inner class names)
     */
    private String fixClassName( String original )
    {
        return original.replaceAll( "\\$", "." );
    }

    /**
     * @return the project classloader
     * @throws DependencyResolutionRequiredException failed to resolve project dependencies
     * @throws MalformedURLException configuration issue ?
     */
    protected ClassLoader getProjectClassLoader()
        throws DependencyResolutionRequiredException, MalformedURLException
    {
        getLog().debug( "AbstractMojo#getProjectClassLoader()" );

        List<?> compile = getProject().getCompileClasspathElements();
        URL[] urls = new URL[compile.size()];
        int i = 0;
        for ( Object object : compile )
        {
            if ( object instanceof Artifact )
            {
                urls[i] = ( (Artifact) object ).getFile().toURI().toURL();
            }
            else
            {
                urls[i] = new File( (String) object ).toURI().toURL();
            }
            i++;
        }
        return new URLClassLoader( urls, ClassLoader.getSystemClassLoader() );
    }

}
