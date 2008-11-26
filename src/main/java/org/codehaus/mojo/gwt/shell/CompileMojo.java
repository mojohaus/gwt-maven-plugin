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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.shell.scripting.CompileScriptConfiguration;
import org.codehaus.mojo.gwt.shell.scripting.ScriptUtil;
import org.codehaus.mojo.gwt.shell.scripting.ScriptWriter;

/**
 * Invokes the GWTCompiler for the project source.
 * 
 * @goal gwtcompile
 * @phase process-classes
 * @requiresDependencyResolution compile
 * @description Invokes the GWTCompiler for the project source.
 * @author cooper
 * @author ccollins
 */
public class CompileMojo
    extends AbstractGwtShellMojo
    implements CompileScriptConfiguration
{

    /** Creates a new instance of CompileMojo */
    public CompileMojo() {
        super();
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!this.getOutput().exists()) {
            this.getOutput().mkdirs();
        }

        // build it for the correct platform
        ScriptWriter writer = scriptWriterFactory.getScriptWriter();
        File exec = writer.writeCompileScript( this );

        // run it
        ScriptUtil.runScript(exec);
    }
}
