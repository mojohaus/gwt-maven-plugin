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
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.gwt.shell.AbstractGwtShellMojo;
import org.codehaus.mojo.gwt.support.ExitException;
import org.codehaus.mojo.gwt.support.GwtWebInfProcessor;

/**
 * Merges GWT servlet elements into deployment descriptor (and non GWT servlets into shell).
 * 
 * @goal mergewebxml
 * @phase process-resources
 * @requiresDependencyResolution compile
 * @description Merges GWT servlet elements into deployment descriptor (and non GWT servlets into shell).
 * @author cooper
 */
public class MergeWebXmlMojo
    extends AbstractGwtShellMojo
{

    /** Creates a new instance of MergeWebXmlMojo */
    public MergeWebXmlMojo() {
        super();
    }

    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            this.getLog().info("copy source web.xml - " + this.getWebXml() + " to build dir (source web.xml required if mergewebxml execution is enabled)");
            File destination = new File(this.getBuildDir(), "web.xml");
            if (!destination.exists()) {
                destination.getParentFile().mkdirs();
                destination.createNewFile();
            }

            FileUtils.copyFile( this.getWebXml(), destination );

            for (int i = 0; i < this.getCompileTarget().length; i++) {
                File moduleFile = null;
                for (Iterator it = this.getProject().getCompileSourceRoots().iterator(); it.hasNext()
                        && moduleFile == null;) {
                    File check = new File(it.next().toString() + "/" + this.getCompileTarget()[i].replace('.', '/')
                            + ".gwt.xml");
                    getLog().debug("Looking for file: " + check.getAbsolutePath());
                    if (check.exists()) {
                        moduleFile = check;
                    }
                }
                for (Iterator it = this.getProject().getResources().iterator(); it.hasNext();) {
                    Resource r = (Resource) it.next();
                    File check = new File(r.getDirectory() + "/" + this.getCompileTarget()[i].replace('.', '/')
                            + ".gwt.xml");
                    getLog().debug("Looking for file: " + check.getAbsolutePath());
                    if (check.exists()) {
                        moduleFile = check;
                    }
                }

                this.fixThreadClasspath();

                GwtWebInfProcessor processor = null;
                try {
                    if (moduleFile != null) {
                        getLog().info("Module file: " + moduleFile.getAbsolutePath());
                        processor = new GwtWebInfProcessor(this.getCompileTarget()[i], moduleFile, destination
                                .getAbsolutePath(), destination.getAbsolutePath(), this.isWebXmlServletPathAsIs());
                    } else {
                        throw new MojoExecutionException("module file null");
                    }
                } catch (ExitException e) {
                    this.getLog().warn(e.getMessage());
                    return;
                }
                processor.process();
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to merge web.xml", e);
        }
    }

}
