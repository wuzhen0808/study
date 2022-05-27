import issueofbuildfailure.MavenProject;
import issueofbuildfailure.MyUpdateReadmeMojo;

public class TestPlugin {


    public void test() throws Exception{
        String projectDir = "C:\\Users\\wu\\git\\camel\\components\\camel-bean";
        MavenProject project = MavenProject.valueOf(projectDir);
        new MyUpdateReadmeMojo().execute(project,null,null);
    }
}
