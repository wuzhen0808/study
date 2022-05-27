package issueofbuildfailure;

import java.io.File;

public class MavenProject {
    File basedir;

    public static MavenProject valueOf(String projectDir) {
        MavenProject project = new MavenProject();
        project.basedir = new File(projectDir);

        return project;
    }

    public File getBasedir() {
        return basedir;
    }

    public String getBuildDirectory() {
        return basedir.getAbsolutePath() + File.separator + "target";
    }
}
