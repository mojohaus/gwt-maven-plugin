package org.codehaus.mojo.gwt.shell.scripting;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;

/**
 * @author ndeloof
 *
 */
public interface MavenScriptConfiguration
{

    /**
     * @return
     */
    File getBuildDir();

    /**
     * @return
     */
    MavenProject getProject();

    /**
     * @return
     */
    List<ArtifactRepository> getRemoteRepositories();

    /**
     * @return
     */
    ArtifactRepository getLocalRepository();
}
