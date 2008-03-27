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
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

/**
 * Goal which compiles a GWT file.
 *
 * @goal generate
 * @phase generate-sources
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class GeneratedDtoMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * The outputDirectory for generated code.
     *
     * @parameter default-value="${project.build.directory}/generated-sources/dto"
     */
    private File outputDirectory;

    /**
     * The package to scan for JPA annotated classes.
     *
     * @parameter
     * @required
     */
    private String packageScan;

    /**
     * The package for generated DTO classes.
     *
     * @parameter
     * @required
     */
    private String dtoPackage;

    /**
     * The parent class for DTO classes
     *
     * @parameter default-value="net.sf.hibernate4gwt.pojo.gwt.LazyGwtPojo"
     */
    private String parentClass;

    /**
     * The suffix to append to generated DTO classes.
     *
     * @parameter default-value="Dto"
     */
    private String dtoSuffix;

    /**
     * {@inheritDoc}
     *
     * @see http://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/
     */
    public void execute()
        throws MojoExecutionException
    {
        getLog().debug( "GeneratedDtoMojo#execute()" );

        try
        {
            getLog().debug( "Search for JPA entities in project classpath" );
            ClassLoader cl = getProjectClassLoader();
            URL[] urls = ClasspathUrlFinder.findResourceBases( packageScan.replace( '.', '/' ), cl );
            AnnotationDB db = new AnnotationDB();
            db.scanArchives( urls );
            Set entityClasses = db.getAnnotationIndex().get( "javax.persistence.Entity" );

            getLog().debug( entityClasses.size() + "JPA entities found" );
            for ( Iterator iterator = entityClasses.iterator(); iterator.hasNext(); )
            {
                String className = (String) iterator.next();
                String generated = dtoPackage.replace( '.', '/' ) + "/" + className + dtoSuffix + ".java";
                PrintWriter out = new PrintWriter( new FileWriter( new File( outputDirectory, generated )) );

                Class entity = cl.loadClass( className );
                out.println( "package " + dtoPackage + ";" );
                out.println( "" );
                out.println( "/*" );
                out.println( " * Data Transfert Object for persistent entity " + className );
                out.println( " * @generated" );
                out.println( " */" );
                out.println( "public class " + className + dtoSuffix );
                out.println( "    extends " + parentClass );
                out.println( "{" );

                Field[] declaredFields = entity.getDeclaredFields();
                for ( int i = 0; i < declaredFields.length; i++ )
                {
                    Field field = declaredFields[i];
                    Type t = field.getGenericType();
                    if (t instanceof ParameterizedType)
                    {
                        out.println( "    /* " );
                        ParameterizedType pt = (ParameterizedType) t;
                        out.println( "     * @gwt.typeArgs " + pt.getActualTypeArguments()[0] );
                        out.println( "     */ " );
                    }
                    out.println( "    public " + field.getType().getName() + " " + field.getName() + ";" );
                    out.println();
                }

                for ( int i = 0; i < declaredFields.length; i++ )
                {
                    Field field = declaredFields[i];
                    String capitalized = StringUtils.capitalize( field.getName() );
                    out.println( "    public " + field.getType().getName() + " get" + capitalized + "()" );
                    out.println( "    {");
                    out.println( "        return this." + field.getName() );
                    out.println( "    }");
                    out.println();
                    out.println( "    public void set" + capitalized + "( " + field.getType().getName() + " " + field.getName() + " )" );
                    out.println( "    {");
                    out.println( "        this." + field.getName() + " = " + field.getName() );
                    out.println( "    }");
                    out.println();
                }

                out.println( "}" );
                out.close();
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to generate DTO from persistent classes", e );
        }

    }

    /**
     * @return the project classloader
     */
    private ClassLoader getProjectClassLoader()
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
}
