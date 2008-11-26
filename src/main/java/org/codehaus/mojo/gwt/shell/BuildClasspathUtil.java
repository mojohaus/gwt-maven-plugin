package org.codehaus.mojo.gwt.shell;

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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ActiveProjectArtifact;
import org.codehaus.mojo.gwt.shell.scripting.DependencyScope;
import org.codehaus.mojo.gwt.shell.scripting.RunScriptConfiguration;

/**
 * Util to consolidate classpath manipulation stuff in one place.
 *
 * @author ccollins
 */
public class BuildClasspathUtil {

   /**
    * Build classpath list using either gwtHome (if present) or using *project*
    * dependencies. Note that this is ONLY used for the script/cmd writers (so
    * the scopes are not for the compiler, or war plugins, etc).
    *
    * This is required so that the script writers can get the dependencies they need
    * regardless of the Maven scopes (still want to use the Maven scopes for everything else
    * Maven, but for GWT-Maven we need to access deps differently - directly at times).
    *
    *
    * @param mojo
    * @param scope
    * @return file collection for classpath
    * @throws DependencyResolutionRequiredException
    */
   public static Collection<File> buildClasspathList( final RunScriptConfiguration mojo, final DependencyScope scope )
            throws DependencyResolutionRequiredException, MojoExecutionException {

      mojo.getLog().info("establishing classpath list (buildClaspathList - scope = " + scope + ")");

      File gwtHome = mojo.getGwtHome();
      MavenProject project = mojo.getProject();

      Set<File> items = new LinkedHashSet<File>();

      // inject GWT jars and relative native libs for all scopes
      // (gwt-user and gwt-dev should be scoped provided to keep them out of
      // other maven stuff - not end up in war, etc - this util is only used for GWT-Maven scripts)
      // TODO filter the rest of the stuff so we don't double add these
      if (gwtHome != null) {
         mojo.getLog().info(
                  "google.webtoolkit.home (gwtHome) set, using it for GWT dependencies - " + gwtHome.getAbsolutePath());
         items.addAll(BuildClasspathUtil.injectGwtDepsFromGwtHome(gwtHome, mojo));
      }
      else {
         mojo.getLog().info("google.webtoolkit.home (gwtHome) *not* set, using project POM for GWT dependencies");
         items.addAll(BuildClasspathUtil.injectGwtDepsFromRepo(mojo));
      }

      // add sources
      if (mojo.getSourcesOnPath()) {
         BuildClasspathUtil.addSourcesWithActiveProjects(project, items, DependencyScope.COMPILE);
      }

      // add resources
      if (mojo.getResourcesOnPath()) {
         BuildClasspathUtil.addResourcesWithActiveProjects(project, items, DependencyScope.COMPILE);
      }

      // add classes dir
      items.add(new File(project.getBasedir(), "classes"));

      // if runtime add runtime
      if (scope == DependencyScope.RUNTIME) {
         for (Iterator it = project.getRuntimeClasspathElements().iterator(); it.hasNext();) {
            items.add(new File(it.next().toString()));
         }
      }

      // if test add test
      if (scope == DependencyScope.TEST) {
         for (Iterator it = project.getTestClasspathElements().iterator(); it.hasNext();) {
            items.add(new File(it.next().toString()));
         }

         // add test sources and resources
         BuildClasspathUtil.addSourcesWithActiveProjects(project, items, scope);
         BuildClasspathUtil.addResourcesWithActiveProjects(project, items, scope);
      }

      // add compile (even when scope is other than)
      for (Iterator it = project.getCompileClasspathElements().iterator(); it.hasNext();) {
         items.add(new File(it.next().toString()));
      }

      // add system
      for (Iterator it = project.getSystemClasspathElements().iterator(); it.hasNext();) {
         items.add(new File(it.next().toString()));
      }

      mojo.getLog().debug("SCRIPT INJECTION CLASSPATH LIST");
      for (File f : items) {
         mojo.getLog().debug("   " + f.getAbsolutePath());
      }

      return items;
   }

   /**
    * Helper to inject gwt-user and gwt-dev into classpath from gwtHome, ONLY
    * for compile and run scripts.
    *
    * @param mojo
    * @return
    */
   public static Collection<File> injectGwtDepsFromGwtHome( final File gwtHome, final RunScriptConfiguration mojo )
    {
      mojo
               .getLog()
               .debug("injecting gwt-user and gwt-dev for script classpath from google.webtoolkit.home (and expecting relative native libs)");
      Collection<File> items = new LinkedHashSet<File>();
      File userJar = new File(gwtHome, "gwt-user.jar");
      File devJar = new File(gwtHome, ArtifactNameUtil.guessDevJarName());
      items.add(userJar);
      items.add(devJar);
      return items;
   }

    /**
     * Helper to inject gwt-user and gwt-dev into classpath from repo, ONLY for compile and run scripts.
     * 
     * @param config
     * @return
     */
   public static Collection<File> injectGwtDepsFromRepo( final RunScriptConfiguration config )
        throws MojoExecutionException
    {
      config
               .getLog()
               .debug(
                        "injecting gwt-user and gwt-dev for script classpath from local repository (and expecting relative native libs)");
      Collection<File> items = new LinkedHashSet<File>();

      Artifact gwtUser =
            config.getArtifactFactory().createArtifactWithClassifier( "com.google.gwt", "gwt-user",
                                                                      config.getGwtVersion(), "jar", null );
        Artifact gwtDev =
            config.getArtifactFactory().createArtifactWithClassifier( "com.google.gwt", "gwt-dev",
                                                                      config.getGwtVersion(), "jar",
                                                                      ArtifactNameUtil.getPlatformName() );

      List remoteRepositories = config.getRemoteRepositories();

      try {
         config.getResolver().resolve( gwtUser, remoteRepositories, config.getLocalRepository() );
            config.getResolver().resolve( gwtDev, remoteRepositories, config.getLocalRepository() );
         items.add(gwtUser.getFile());
         items.add(gwtDev.getFile());
      }
      catch (ArtifactNotFoundException e) {
         throw new MojoExecutionException("artifact not found - " + e.getMessage(), e);
      }
      catch (ArtifactResolutionException e) {
         throw new MojoExecutionException("artifact resolver problem - " + e.getMessage(), e);
      }

      return items;
   }

   /**
    * Add all sources and resources also with active (maven reactor active) referenced project sources and resources.
    * Addresses issue no. 147.
    *
    * @param project
    * @param items
    * @param scope
    */
   private static void addSourcesWithActiveProjects(final MavenProject project, final Set<File> items,
            final DependencyScope scope) {
      final List<Artifact> scopeArtifacts = BuildClasspathUtil.getScopeArtifacts(project, scope);

      BuildClasspathUtil.addSources(items, BuildClasspathUtil.getSourceRoots(project, scope));

      for (Artifact artifact : scopeArtifacts) {
         if (artifact instanceof ActiveProjectArtifact) {
            MavenProject refProject = (MavenProject) project.getProjectReferences().get(
                     BuildClasspathUtil.getProjectReferenceId(artifact.getGroupId(), artifact.getArtifactId(), artifact
                              .getVersion()));
            if (refProject != null) {
               BuildClasspathUtil.addSources(items, BuildClasspathUtil.getSourceRoots(refProject, scope));
            }
         }
      }
   }

   /**
    * Add all sources and resources also with active (maven reactor active) referenced project sources and resources.
    * Addresses issue no. 147.
    *
    * @param project
    * @param items
    * @param scope
    */
   private static void addResourcesWithActiveProjects(final MavenProject project, final Set<File> items,
            final DependencyScope scope) {
      final List<Artifact> scopeArtifacts = BuildClasspathUtil.getScopeArtifacts(project, scope);

      BuildClasspathUtil.addResources(items, BuildClasspathUtil.getResources(project, scope));

      for (Artifact artifact : scopeArtifacts) {
         if (artifact instanceof ActiveProjectArtifact) {
            MavenProject refProject = (MavenProject) project.getProjectReferences().get(
                     BuildClasspathUtil.getProjectReferenceId(artifact.getGroupId(), artifact.getArtifactId(), artifact
                              .getVersion()));
            if (refProject != null) {
               BuildClasspathUtil.addResources(items, BuildClasspathUtil.getResources(refProject, scope));
            }
         }
      }
   }

   /**
    * Get artifacts for specific scope.
    * @param project
    * @param scope
    * @return
    */
   @SuppressWarnings("unchecked")
   private static List<Artifact> getScopeArtifacts(final MavenProject project, final DependencyScope scope) {
      if (DependencyScope.COMPILE.equals(scope)) {
         return project.getCompileArtifacts();
      }
      else if (DependencyScope.TEST.equals(scope)) {
         return project.getTestArtifacts();
      }
      else {
         throw new RuntimeException("Not allowed scope " + scope);
      }
   }

   /**
    * Get source roots for specific scope.
    * @param project
    * @param scope
    * @return
    */
   private static List getSourceRoots(final MavenProject project, final DependencyScope scope) {
      if (DependencyScope.COMPILE.equals(scope)) {
         return project.getCompileSourceRoots();
      }
      else if (DependencyScope.TEST.equals(scope)) {
         return project.getTestCompileSourceRoots();
      }
      else {
         throw new RuntimeException("Not allowed scope " + scope);
      }
   }

   /**
    * Get resources for specific scope.
    * @param project
    * @param scope
    * @return
    */
   @SuppressWarnings("unchecked")
   private static List<Artifact> getResources(final MavenProject project, final DependencyScope scope) {
      if (DependencyScope.COMPILE.equals(scope)) {
         return project.getResources();
      }
      else if (DependencyScope.TEST.equals(scope)) {
         return project.getTestResources();
      }
      else {
         throw new RuntimeException("Not allowed scope " + scope);
      }
   }

   /**
    * Add source path and resource paths of the project to the list of classpath items.
    * @param items Classpath items.
    * @param sourceRoots
    */
   private static void addSources(final Set<File> items, final List sourceRoots) {
      for (Iterator it = sourceRoots.iterator(); it.hasNext();) {
         items.add(new File(it.next().toString()));
      }
   }

   /**
    * Add source path and resource paths of the project to the list of classpath items.
    * @param items Classpath items.
    * @param resources
    */
   private static void addResources(final Set<File> items, final List resources) {
      for (Iterator it = resources.iterator(); it.hasNext();) {
         Resource r = (Resource) it.next();
         items.add(new File(r.getDirectory()));
      }
   }

   /**
    * Cut from MavenProject.java
    *
    * @param groupId
    * @param artifactId
    * @param version
    * @return
    */
   private static String getProjectReferenceId(final String groupId, final String artifactId, final String version) {
      return groupId + ":" + artifactId + ":" + version;
   }
}
