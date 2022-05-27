package issueofbuildfailure;

import org.apache.camel.maven.packaging.MvelHelper;
import org.apache.camel.maven.packaging.UpdateReadmeMojo;
import org.apache.camel.tooling.model.*;
import org.apache.camel.tooling.util.PackageHelper;
import org.apache.camel.tooling.util.Strings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.mvel2.templates.TemplateRuntime;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MyUpdateReadmeMojo {

    MavenProject project;
    File buildDir;

    File componentDocDir;
    File dataformatDocDir;
    File languageDocDir;
    File languageDocDir2;

    File eipDocDir;

    Logger log = new Logger();

    public MyUpdateReadmeMojo() {

    }

    private static final Pattern[] MANUAL_ATTRIBUTES = {
            Pattern.compile(":(group): *(.*)"),
            Pattern.compile(":(summary-group): *(.*)")};

    public void execute(MavenProject project,
                        MavenProjectHelper projectHelper,
                        BuildContext buildContext)
            throws MojoFailureException, MojoExecutionException {
        this.project = project;
        buildDir = new File(project.getBuildDirectory());
        componentDocDir = new File(project.getBasedir(), "src/main/docs");
        dataformatDocDir = new File(project.getBasedir(), "src/main/docs");
        languageDocDir = new File(project.getBasedir(), "/src/main/docs");
        languageDocDir2 = new File(project.getBasedir(), "/src/main/docs/modules/languages/pages");
        eipDocDir = new File(project.getBasedir(), "src/main/docs/modules/eips/pages");
        this.project = project;
        execute();

    }

    private void execute() throws MojoExecutionException {
        executeComponent();
    }

    private Logger getLog() {
        return this.log;
    }

    private void executeComponent() throws MojoExecutionException {
        // find the component names
        final String kind = "component";
        List<String> componentNames = listDescriptorNamesOfType(kind);

        final Set<File> jsonFiles = new TreeSet<>();
        PackageHelper.findJsonFiles(buildDir, jsonFiles);

        // only if there is components we should update the documentation files
        if (!componentNames.isEmpty()) {
            getLog().debug("Found " + componentNames.size() + " components");
            for (String componentName : componentNames) {
                String json = loadJsonFrom(jsonFiles, kind, componentName);
                if (json != null) {
                    // special for some components
                    componentName = asComponentName(componentName);

                    File file = new File(componentDocDir, componentName + "-" + kind + ".adoc");
                    getLog().debug("adoc:" + file.getAbsolutePath());
                    boolean exists = file.exists();

                    ComponentModel model = generateComponentModel(json);
                    String title = asComponentTitle(model.getScheme(), model.getTitle());
                    model.setTitle(title);

                    // we only want the first scheme as the alternatives do not
                    // have their own readme file
                    if (!Strings.isEmpty(model.getAlternativeSchemes())) {
                        String first = model.getAlternativeSchemes().split(",")[0];
                        if (!model.getScheme().equals(first)) {
                            continue;
                        }
                    }

                    boolean updated = updateHeader(componentName, file, model, " Component", kind);
                    //ignore more
                }
            }
        }
    }


    private List<String> listDescriptorNamesOfType(final String type) {
        List<String> names = new ArrayList<>();

        File f = new File(project.getBasedir(), "target/classes");
        f = new File(f, "META-INF/services/org/apache/camel/" + type);
        if (f.exists() && f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File file : files) {
                    // skip directories as there may be a sub .resolver
                    // directory
                    if (file.isDirectory()) {
                        continue;
                    }
                    String name = file.getName();
                    if (name.charAt(0) != '.') {
                        names.add(name);
                    }
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    public boolean updateHeader(
            String name, final File file, final BaseModel<? extends BaseOptionModel> model, String titleSuffix,
            String kind)
            throws MojoExecutionException {
        //    getLog().debug("updateHeader " + file);
        final String linkSuffix = "-" + kind;
        if (model == null || !file.exists()) {
            return false;
        }

        boolean updated = false;
        try {
            String text = PackageHelper.loadText(file);

            String[] lines = text.split("\n");

            // check first if it is a standard documentation file, we expect at
            // least five lines
            if (lines.length < 5) {
                return false;
            }

            // find manual attributes
            Map<String, String> manualAttributes = new LinkedHashMap<>();
            for (String line : lines) {
                if (line.length() == 0) {
                    break;
                }
                for (Pattern attrName : MANUAL_ATTRIBUTES) {
                    Matcher m = attrName.matcher(line);
                    if (m.matches()) {
                        manualAttributes.put(m.group(1), m.group(2));
                        break;
                    }
                }
            }

            List<String> newLines = new ArrayList<>(lines.length + 8);

            //link
            newLines.add("[[" + name + linkSuffix + "]]");

            //title
            String title = model.getTitle() + titleSuffix;
            if (model.isDeprecated()) {
                title += " (deprecated)";
            }
            newLines.add("= " + title);
            newLines.add(":docTitle: " + model.getTitle());

            if (model instanceof ArtifactModel<?>) {
                newLines.add(":artifactId: " + ((ArtifactModel<?>) model).getArtifactId());
            }
            newLines.add(":description: " + model.getDescription());
            newLines.add(":since: " + model.getFirstVersionShort());
            //TODO put the deprecation into the actual support level.
            newLines.add(":supportLevel: " + model.getSupportLevel().toString() + (model.isDeprecated() ? "-deprecated" : ""));
            if (model.isDeprecated()) {
                newLines.add(":deprecated: *deprecated*");
            }
            if (model instanceof ComponentModel) {
                newLines.add(":component-header: " + generateComponentHeader((ComponentModel) model));
                if (Arrays.asList(model.getLabel().split(",")).contains("core")) {
                    newLines.add(":core:");
                }
            }

            newLines.add(
                    "include::{cq-version}@camel-quarkus:ROOT:partial$reference/" + kind + "s/" + name
                            + ".adoc[opts=optional]");

            if (!manualAttributes.isEmpty()) {
                newLines.add("//Manually maintained attributes");
                for (Map.Entry<String, String> entry : manualAttributes.entrySet()) {
                    newLines.add(":" + entry.getKey() + ": " + entry.getValue());
                }
            }

            newLines.add("");

            for (int i = 0; i < lines.length; i++) {
                if (i > newLines.size() - 1) {
                    break;
                }
                if (!newLines.get(i).equals(lines[i])) {
                    updated = true;
                    break;
                }
            }

            boolean copy = false;
            if (updated) {
                for (int i = 0; i < lines.length; i++) {
                    if (!copy && lines[i].isEmpty()) {
                        copy = true;
                    } else if (copy) {
                        newLines.add(lines[i]);
                    }
                }
                if (!copy) {
                    throw new MojoFailureException("File " + file + " has unexpected structure with no empty line.");
                }
            }

            if (updated) {
                // build the new updated text
                if (!newLines.get(newLines.size() - 1).isEmpty()) {
                    newLines.add("");
                }
                String newText = String.join("\n", newLines);
                PackageHelper.writeText(file, newText);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error reading file " + file + " Reason: " + e, e);
        }

        return updated;
    }

    private static String generateComponentHeader(final ComponentModel model) {
        final boolean consumerOnly = model.isConsumerOnly();
        final boolean producerOnly = model.isProducerOnly();
        // if we have only producer support
        if (!consumerOnly && producerOnly) {
            return "Only producer is supported";
        }
        // if we have only consumer support
        if (consumerOnly && !producerOnly) {
            return "Only consumer is supported";
        }

        return "Both producer and consumer are supported";
    }


    private ComponentModel generateComponentModel(String json) {
        ComponentModel component = JsonMapper.generateComponentModel(json);
        Stream.concat(component.getComponentOptions().stream(), component.getEndpointOptions().stream())
                .filter(BaseOptionModel::isAutowired).forEach(option -> {
                    String desc = "*Autowired* " + option.getDescription();
                    option.setDescription(desc);
                });
        Stream.concat(component.getComponentOptions().stream(), component.getEndpointOptions().stream())
                .filter(BaseOptionModel::isRequired).forEach(option -> {
                    String desc = "*Required* " + option.getDescription();
                    option.setDescription(desc);
                });
        Stream.concat(component.getComponentOptions().stream(), component.getEndpointOptions().stream())
                .filter(BaseOptionModel::isDeprecated).forEach(option -> {
                    String desc = "*Deprecated* " + option.getDescription();
                    if (!Strings.isEmpty(option.getDeprecationNote())) {
                        if (!desc.endsWith(".")) {
                            desc += ".";
                        }
                        desc = desc + " Deprecation note: " + option.getDeprecationNote();
                    }
                    option.setDescription(desc);
                });
        Stream.concat(component.getComponentOptions().stream(), component.getEndpointOptions().stream())
                .filter(o -> o.getEnums() != null).forEach(option -> {
                    String desc = option.getDescription();
                    if (!desc.endsWith(".")) {
                        desc = desc + ".";
                    }
                    desc = desc + " There are " + option.getEnums().size() + " enums and the value can be one of: "
                            + wrapEnumValues(option.getEnums());
                    option.setDescription(desc);
                });
        return component;
    }


    private static String asComponentTitle(String name, String title) {
        // special for some components which share the same readme file
        if (name.equals("imap") || name.equals("imaps") || name.equals("pop3") || name.equals("pop3s") || name.equals("smtp")
                || name.equals("smtps")) {
            return "Mail";
        }

        return title;
    }

    private String wrapEnumValues(List<String> enumValues) {
        // comma to space so we can wrap words (which uses space)
        return String.join(", ", enumValues);
    }


    private static String loadJsonFrom(Set<File> jsonFiles, String kind, String name) {
        for (File file : jsonFiles) {
            if (file.getName().equals(name + PackageHelper.JSON_SUFIX)) {
                try {
                    String json = PackageHelper.loadText(file);
                    if (Objects.equals(kind, PackageHelper.getSchemaKind(json))) {
                        return json;
                    }
                } catch (IOException ignored) {
                    // ignored
                }
            }
        }

        return null;
    }

    private static String loadJsonFrom(File file, String kind) {
        if (file.getName().endsWith(PackageHelper.JSON_SUFIX)) {
            try {
                String json = PackageHelper.loadText(file);
                if (Objects.equals(kind, PackageHelper.getSchemaKind(json))) {
                    return json;
                }
            } catch (IOException ignored) {
                // ignored
            }
        }

        return null;
    }

    private static String loadEipJson(File file) {
        try {
            String json = PackageHelper.loadText(file);
            if ("model".equals(PackageHelper.getSchemaKind(json))) {
                return json;
            }
        } catch (IOException ignored) {
            // ignore
        }
        return null;
    }

    private static String asComponentName(String name) {
        // special for some components which share the same readme file
        if (name.equals("imap") || name.equals("imaps") || name.equals("pop3") || name.equals("pop3s") || name.equals("smtp")
                || name.equals("smtps")) {
            return "mail";
        }

        return name;
    }


    private void checkComponentHeader(final File file, final ComponentModel model) throws MojoExecutionException {
        if (!file.exists()) {
            return;
        }

        final String headerText = "*{component-header}*";
        String loadedText;

        try {
            loadedText = PackageHelper.loadText(file);

        } catch (Exception e) {
            throw new MojoExecutionException("Error reading file " + file + " Reason: " + e, e);
        }
        if (!loadedText.contains(headerText)) {
            throw new MojoExecutionException("File " + file + " does not contain required string `" + headerText + "'");
        }
    }

    private void checkSince(final File file, final ArtifactModel<?> model) throws MojoExecutionException {
        if (!file.exists()) {
            return;
        }

        final String sinceText = "*Since Camel {since}*";
        String loadedText;

        try {
            loadedText = PackageHelper.loadText(file);
        } catch (Exception e) {
            throw new MojoExecutionException("Error reading file " + file + " Reason: " + e, e);
        }
        if (!loadedText.contains(sinceText)) {
            throw new MojoExecutionException("File " + file + " does not contain required string '" + sinceText + "'");
        }
    }


    private static String evaluateTemplate(final String templateName, final Object model) throws MojoExecutionException {
        try (InputStream templateStream = UpdateReadmeMojo.class.getClassLoader().getResourceAsStream(templateName)) {
            String template = PackageHelper.loadText(templateStream);
            return (String) TemplateRuntime.eval(template, model, Collections.singletonMap("util", MvelHelper.INSTANCE));
        } catch (IOException e) {
            throw new MojoExecutionException("Error processing mvel template `" + templateName + "`", e);
        }
    }
}
