/*
 * I18NMojo.java
 *
 * Created on August 19th, 2008
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
import org.codehaus.mojo.gwt.shell.scripting.I18nScriptConfiguration;
import org.codehaus.mojo.gwt.shell.scripting.ScriptUtil;
import org.codehaus.mojo.gwt.shell.scripting.ScriptWriter;

/**
 * Creates I18N interfaces for constants and messages files.
 * 
 * @goal gwti18n
 * @phase process-resources
 * @requiresDependencyResolution compile
 * @description Creates I18N interfaces for constants and messages files.
 * @author Sascha-Matthias Kulawik <sascha@kulawik.de>
 * @author ccollins
 */
public class I18NMojo
    extends AbstractGwtShellMojo
    implements I18nScriptConfiguration
{

    /**
     * Location on filesystem to output generated i18n Constants and Messages interfaces.
     * 
     * @parameter expression="${basedir}/src/main/java/"
     */
    private File i18nOutputDir;

    /**
     * List of names of properties files that should be used to generate i18n Messages interfaces.
     * 
     * @parameter
     */
    private String[] i18nMessagesNames;

    /**
     * List of names of properties files that should be used to generate i18n Constants interfaces.
     * 
     * @parameter
     */
    private String[] i18nConstantsNames;

    public void doExecute(GwtRuntime runtime)
        throws MojoExecutionException, MojoFailureException
    {

        if (this.getI18nMessagesNames() == null && this.getI18nConstantsNames() == null) {
            throw new MojoExecutionException(
                    "neither i18nConstantsNames nor i18nMessagesNames present, cannot execute i18n goal");
        }

        if (!this.getI18nOutputDir().exists()) {
            if (getLog().isInfoEnabled())
                getLog().info("I18NModule is creating target directory " + getI18nOutputDir().getAbsolutePath());
            this.getI18nOutputDir().mkdirs();
        }

        // build it for the correct platform
        ScriptWriter writer = scriptWriterFactory.getScriptWriter();
        File exec = writer.writeI18nScript( this, runtime );

        // run it
        ScriptUtil.runScript(exec);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getI18nOutputDir()
     */
    public File getI18nOutputDir()
    {
        return this.i18nOutputDir;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getI18nMessagesNames()
     */
    public String[] getI18nMessagesNames()
    {
        return this.i18nMessagesNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.mojo.gwt.shell.scripting.ScriptConfiguration#getI18nConstantsNames()
     */
    public String[] getI18nConstantsNames()
    {
        return this.i18nConstantsNames;
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
