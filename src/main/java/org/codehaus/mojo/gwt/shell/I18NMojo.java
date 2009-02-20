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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.GwtRuntime;
import org.codehaus.mojo.gwt.shell.scripting.I18nScriptConfiguration;
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
 * @version $Id$
 */
public class I18NMojo
    extends AbstractGwtShellMojo
    implements I18nScriptConfiguration
{
    /**
     * List of resourceBundles that should be used to generate i18n Messages interfaces.
     * 
     * @parameter
     * @alias i18nMessagesNames
     */
    private String[] i18nMessagesBundles;

    /**
     * Shortcut for a single i18nMessagesBundle
     * 
     * @parameter
     */
    @SuppressWarnings("unused")
    private String i18nMessagesBundle;

    /**
     * List of resourceBundles that should be used to generate i18n Constants interfaces.
     * 
     * @parameter
     * @alias i18nConstantsNames
     */
    private String[] i18nConstantsBundles;

    /**
     * Shortcut for a single i18nConstantsBundle
     * 
     * @parameter
     */
    @SuppressWarnings("unused")
    private String i18nConstantsBundle;

    /**
     * List of resourceBundles that should be used to generate i18n ConstantsWithLookup interfaces.
     * 
     * @parameter
     */
    private String[] i18nConstantsWithLookupBundles;

    /**
     * Shortcut for a single i18nConstantsWithLookupBundle
     * 
     * @parameter
     */
    @SuppressWarnings("unused")
    private String i18nConstantsWithLookupBundle;

    public void doExecute( GwtRuntime runtime )
        throws MojoExecutionException, MojoFailureException
    {

        if ( i18nMessagesBundles == null && i18nConstantsBundles == null && i18nConstantsWithLookupBundles == null )
        {
            throw new MojoExecutionException(
                                              "neither i18nConstantsBundles, i18nMessagesBundles nor i18nConstantsWithLookupBundles present, cannot execute i18n goal" );
        }

        if ( !generateDirectory.exists() )
        {
            getLog().debug( "Creating target directory " + generateDirectory.getAbsolutePath() );
            generateDirectory.mkdirs();
        }

        // build it for the correct platform
        ScriptWriter script = scriptWriterFactory.getScript();
        script.createScript( this, "i18n" );


     // constants
        if ( getI18nConstantsBundles() != null )
        {
            for ( String target : getI18nConstantsBundles() )
            {
                ensureTargetPackageExists( getGenerateDirectory(), target );
                script.executeClass( this, runtime, ScriptWriter.FORKBOOTER, "com.google.gwt.i18n.tools.I18NSync" );
                script.print( " -out " );
                script.print( "\"" + getGenerateDirectory() + "\"" );
                script.print( " " );
                script.print( target );
                script.println();
            }
        }

        // messages
        if ( getI18nMessagesBundles() != null )
        {
            for ( String target : getI18nMessagesBundles() )
            {
                ensureTargetPackageExists( getGenerateDirectory(), target );
                script.executeClass( this, runtime, ScriptWriter.FORKBOOTER, "com.google.gwt.i18n.tools.I18NSync" );
                script.print( " -createMessages " );
                script.print( " -out " );
                script.print( "\"" + getGenerateDirectory() + "\"" );
                script.print( " " );
                script.print( target );
                script.println();
            }
        }

        // run it
        runScript( script.getExecutable() );
    }


    private void ensureTargetPackageExists( File generateDirectory, String targetName )
    {
        targetName = targetName.substring( 0, targetName.lastIndexOf( '.' ) );
        String targetPackage = targetName.replace( '.', File.separatorChar );
        getLog().debug( "ensureTargetPackageExists, targetName : " + targetName + ", targetPackage : " + targetPackage );
        File targetPackageDirectory = new File( generateDirectory, targetPackage );
        if ( !targetPackageDirectory.exists() )
        {
            targetPackageDirectory.mkdirs();
        }
    }


    public void setI18nConstantsWithLookupBundle( String i18nConstantsWithLookupBundle )
    {
        this.i18nConstantsWithLookupBundles = new String[] { i18nConstantsWithLookupBundle };
    }

    public void setI18ConstantsBundle( String i18nConstantsBundle )
    {
        this.i18nConstantsBundles = new String[] { i18nConstantsBundle };
    }

    public void setI18nMessagesBundle( String i18nMessagesBundle )
    {
        this.i18nMessagesBundles = new String[] { i18nMessagesBundle };
    }

    public String[] getI18nMessagesBundles()
    {
        return i18nMessagesBundles;
    }

    public String[] getI18nConstantsBundles()
    {
        return i18nConstantsBundles;
    }

    public String[] getI18nConstantsWithLookupBundles()
    {
        return i18nConstantsWithLookupBundles;
    }
}
