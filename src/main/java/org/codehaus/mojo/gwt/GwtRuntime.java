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
import java.net.URL;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

/**
 * @author ndeloof
 * @version $Id$
 */
public class GwtRuntime
{
    /** The gwt-user jar used at runtime */
    private File gwtUserJar;

    /** The gwt-dev-[platform] jar used at runtime */
    private File gwtDevJar;

    /** The gwt version we are running */
    private GwtVersion version;

    /**
     * @param gwtUserJar gwt user library
     * @param gwtDevJar gwt dev library
     * @param version gwt version
     */
    public GwtRuntime( File gwtUserJar, File gwtDevJar, String version )
    {
        super();
        this.version = GwtVersion.fromMavenVersion( version );
        this.gwtUserJar = gwtUserJar;
        this.gwtDevJar = gwtDevJar;
    }

    /**
     * @param gwtUserJar gwt user library
     * @param gwtDevJar gwt dev library
     */
    public GwtRuntime( File gwtUserJar, File gwtDevJar )
    {
        this( gwtUserJar, gwtDevJar, readGwtDevVersion( gwtDevJar ) );
    }

    /**
     * Read the GWT version from the About class present in gwt-dev JAR
     *
     * @param gwtDevJar gwt platform-dependent developer library
     * @return version declared in dev library
     */
    private static String readGwtDevVersion( File gwtDevJar )
    {
        try
        {
             URL about = new URL( "jar:" + gwtDevJar.toURL() + "!/com/google/gwt/dev/About.class" );
            ClassParser parser = new ClassParser( about.openStream(), "About.class" );
            JavaClass clazz = parser.parse();
            for ( org.apache.bcel.classfile.Field field : clazz.getFields() )
            {
                if ( "GWT_VERSION_NUM".equals( field.getName() ) )
                {
                    // Return the constant value between quotes
                    String constant = field.getConstantValue().toString();
                    return constant.substring( 1, constant.length() - 1 );
                }
            }
            throw new IllegalStateException( "Failed to retrieve GWT_VERSION_NUM in " + gwtDevJar.getName()
                + " 'About' class" );

            // Can't get this to work as expected, always return maven dependency "1.5.3" :'-(
            // ClassLoader cl = new URLClassLoader( new URL[] { gwtDevJar.toURL() }, ClassLoader.getSystemClassLoader()
            // );
            // Class<?> about = cl.loadClass( "com.google.gwt.dev.About" );
            // Field versionNumber = about.getField( "GWT_VERSION_NUM" );
            // String version = versionNumber.get( about ).toString();
            // return version;
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( "Failed to read gwt-dev version from " + gwtDevJar.getAbsolutePath() );
        }
    }

    public File getGwtUserJar()
    {
        return gwtUserJar;
    }

    public File getGwtDevJar()
    {
        return gwtDevJar;
    }

    public GwtVersion getVersion()
    {
        return version;
    }

}
