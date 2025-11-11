/*
 *     Copyright 2011-2025 the original author or authors.
 *
 *     All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 *     Public License v1.0 which accompanies this distribution, and is available at
 *
 *     https://www.eclipse.org/legal/epl-v10.html.
 */
package com.hazendaz.maven.jautodoc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import net.sf.jautodoc.preferences.Configuration;
import net.sf.jautodoc.source.SourceManipulator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaCore;

/**
 * The Class JautodocMojo.
 */
@Mojo(name = "jautodoc", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresProject = false)
public class JautodocMojo extends AbstractMojo {

    /** The static files comment. */
    private static final String FILE_S = " file(s)";

    /** Skip run of plugin. */
    @Parameter(defaultValue = "false", alias = "skip", property = "skip")
    private boolean skip;

    /** The base directory. */
    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File basedir;

    /** Log the files that are being processed. */
    @Parameter(defaultValue = "false", property = "verbose")
    private boolean verbose;

    /**
     * The mode to use: 'complete' - Complete existing Javadoc, 'keep' - Keep existing Javadoc, 'replace' - Replace
     * existing Javadoc.
     */
    @Parameter(property = "mode")
    private String mode;

    /** Comment public members. */
    @Parameter(defaultValue = "true", property = "commentPublic")
    private boolean commentPublic;

    /** Comment package members. */
    @Parameter(defaultValue = "true", property = "commentPackage")
    private boolean commentPackage;

    /** Comment protected members. */
    @Parameter(defaultValue = "false", property = "commentProtected")
    private boolean commentProtected;

    /** Comment private members. */
    @Parameter(defaultValue = "false", property = "commentPrivate")
    private boolean commentPrivate;

    /** Comment types. */
    @Parameter(defaultValue = "true", property = "commentTypes")
    private boolean commentTypes;

    /** Comment fields. */
    @Parameter(defaultValue = "true", property = "commentFields")
    private boolean commentFields;

    /** Comment methods. */
    @Parameter(defaultValue = "true", property = "commentMethods")
    private boolean commentMethods;

    /** Comment get/set only. */
    @Parameter(defaultValue = "false", property = "commentGetterSetterOnly")
    private boolean commentGetterSetterOnly;

    /** Comment exclude getter/setter. */
    @Parameter(defaultValue = "false", property = "excludeGetterSetter")
    private boolean excludeGetterSetter;

    /** Add 'todo' auto generated javadoc. */
    @Parameter(defaultValue = "false", property = "addTodoForAutodoc")
    private boolean addTodoForAutodoc;

    /** Create comment from element name. */
    @Parameter(defaultValue = "true", property = "createDummyComment")
    private boolean createDummyComment;

    /** Single line field comment. */
    @Parameter(defaultValue = "true", property = "singleLineComment")
    private boolean singleLineComment;

    /** Use Eclipse comment formatter. */
    @Parameter(defaultValue = "false", property = "useEclipseFormatter")
    private boolean useEclipseFormatter;

    /** [G,S]etter from field comment. */
    @Parameter(defaultValue = "false", property = "getterSetterFromField")
    private boolean getterSetterFromField;

    /** First sentence only. */
    @Parameter(defaultValue = "false", property = "getterSetterFromFieldFirst")
    private boolean getterSetterFromFieldFirst;

    /** Replace existing getter/setter. */
    @Parameter(defaultValue = "true", property = "getterSetterFromFieldReplace")
    private boolean getterSetterFromFieldReplace;

    /** Add file header. */
    @Parameter(defaultValue = "false", property = "addHeader")
    private boolean addHeader;

    /** Replace existing header. */
    @Parameter(defaultValue = "false", property = "replaceHeader")
    private boolean replaceHeader;

    /** Multi comment header. */
    @Parameter(defaultValue = "false", property = "multiCommentHeader")
    private boolean multiCommentHeader;

    /** Add header only (No Javadoc created). */
    @Parameter(defaultValue = "false", property = "headerOnly")
    private boolean headerOnly;

    /** Maven ProjectHelper. */
    @Inject
    private MavenProjectHelper projectHelper;

    /** Maven Project. */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Check if plugin run should be skipped
        if (this.skip) {
            getLog().info("Jautodoc is skipped");
            return;
        }

        long startClock = System.currentTimeMillis();

        List<File> files = new ArrayList<>();
        if (this.basedir != null && this.basedir.exists() && this.basedir.isDirectory()) {
            files.addAll(addCollectionFiles(this.basedir));
        }

        int numberOfFiles = files.size();
        Log log = getLog();
        log.info("Number of files to be jautodoc'd: " + numberOfFiles);

        if (numberOfFiles > 0) {
            ResultCollector rc = new ResultCollector();

            // Ensure a minimal Eclipse workspace exists
            Path workspaceDir = Path.of(project.getBuild().getDirectory(), "jautodoc-workspace");
            try {
                ensureMinimalWorkspace(workspaceDir);
            } catch (IOException e) {
                log.error("unable to create Workspace " + e.getMessage());
                return;
            }
            System.setProperty("osgi.instance.area", workspaceDir.toFile().getAbsolutePath());

            // Cause a workspace to be created
            Plugin plugin = new ResourcesPlugin();
            try {
                plugin.start(InternalPlatform.getDefault().getBundleContext());
            } catch (Exception e) {
                log.error("unable to startup plugin " + e.getMessage());
                return;
            }
            plugin.getStateLocation();

            // Get workspace
            IWorkspace ws = ResourcesPlugin.getWorkspace();
            IProject project = ws.getRoot().getProject("External Files");
            if (!project.exists()) {
                try {
                    project.create(null);
                } catch (CoreException e) {
                    log.error("unable to create Workspace " + e.getMessage());
                    return;
                }
            }

            // Load configuration
            Configuration configuration = this.loadConfiguration();

            // Process files
            for (int i = 0, n = files.size(); i < n; i++) {
                File file = files.get(i);
                if (file.exists()) {
                    if (file.canWrite()) {
                        ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom(
                                project.getFile(new org.eclipse.core.runtime.Path(file.getPath())));
                        SourceManipulator source;
                        try {
                            source = new SourceManipulator(compilationUnit, configuration);
                            if (!headerOnly) {
                                source.addJavadoc(null);
                            } else {
                                source.setForceAddHeader(headerOnly);
                                source.addJavadoc(new IMember[0], null);
                            }
                        } catch (IOException e) {
                            getLog().error("", e);
                            rc.skippedCount++;
                        } catch (Exception e) {
                            getLog().error("", e);
                            rc.skippedCount++;
                        }
                    } else {
                        rc.readOnlyCount++;
                    }
                } else {
                    rc.failCount++;
                }
            }

            // Shutdown
            try {
                plugin.stop(InternalPlatform.getDefault().getBundleContext());
            } catch (Exception e) {
                log.error("unable to shutdown plugin " + e.getMessage());
                return;
            }

            // Finish processing
            long endClock = System.currentTimeMillis();

            log.info("Successfully formatted: " + rc.successCount + FILE_S);
            log.info("Fail to format:         " + rc.failCount + FILE_S);
            log.info("Skipped:                " + rc.skippedCount + FILE_S);
            log.info("Read only skipped:      " + rc.readOnlyCount + FILE_S);
            log.info("Approximate time taken: " + ((endClock - startClock) / 1000) + "s");
        }

    }

    /**
     * Ensures a minimal Eclipse workspace exists at the given location. Creates the .metadata directory if missing.
     *
     * @param workspaceDir
     *            the workspace dir
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void ensureMinimalWorkspace(Path workspaceDir) throws IOException {
        if (!Files.exists(workspaceDir)) {
            Files.createDirectories(workspaceDir);
        }
        Path metadataDir = workspaceDir.resolve(".metadata");
        if (!Files.exists(metadataDir)) {
            Files.createDirectories(metadataDir);
        }
    }

    /**
     * Load configuration.
     *
     * @return the configuration
     */
    private Configuration loadConfiguration() {
        final Configuration configuration = new Configuration();
        configuration.setAddHeader(this.addHeader);
        configuration.setAddTodoForAutodoc(this.addTodoForAutodoc);
        configuration.setCommentFields(this.commentFields);
        configuration.setCommentTypes(this.commentTypes);
        if (this.mode != null && this.mode.equals("complete")) {
            configuration.setCompleteExistingJavadoc(true);
        }
        configuration.setCreateDummyComment(this.createDummyComment);
        configuration.setExcludeGetterSetter(this.excludeGetterSetter);
        // configuration.setExcludeOverriding(excludeOverriding);
        // configuration.setGetSetFromFieldReplacements(getterSetterFromFieldReplacements);
        configuration.setGetterSetterFromField(this.getterSetterFromField);
        configuration.setGetterSetterFromFieldFirst(this.getterSetterFromFieldFirst);
        configuration.setGetterSetterFromFieldReplace(this.getterSetterFromFieldReplace);
        configuration.setGetterSetterOnly(this.commentGetterSetterOnly);
        // configuration.setHeaderText(headerText);
        // configuration.setIncludeSubPackages(includeSubPackages);
        if (this.mode != null && this.mode.equals("keep")) {
            configuration.setKeepExistingJavadoc(true);
        }
        configuration.setMultiCommentHeader(this.multiCommentHeader);
        // configuration.setPackageDocText(packageDocText);
        // configuration.setPackageInfoText(packageInfoText);
        // configuration.setProperties(properties);
        if (this.mode != null && this.mode.equals("replace")) {
            configuration.setReplaceExistingJavadoc(true);
        }
        configuration.setReplaceHeader(this.replaceHeader);
        configuration.setSingleLineComment(this.singleLineComment);
        // configuration.setTagOrder(tagOrder);
        configuration.setUseEclipseFormatter(this.useEclipseFormatter);
        // configuration.setUsePackageInfo(usePackageInfo);
        configuration.setVisibilityPackage(this.commentPackage);
        configuration.setVisibilityPrivate(this.commentPrivate);
        configuration.setVisibilityProtected(this.commentProtected);
        configuration.setVisibilityPublic(this.commentPublic);
        return configuration;
    }

    /**
     * Add source files to the files list.
     *
     * @param newBasedir
     *            the new basedir
     *
     * @return the list
     */
    List<File> addCollectionFiles(File newBasedir) {
        List<String> includes = new ArrayList<>();
        includes.add("**/*.java");

        final DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(newBasedir);
        ds.setIncludes(includes.toArray(new String[0]));
        ds.addDefaultExcludes();
        ds.setCaseSensitive(false);
        ds.setFollowSymlinks(false);
        ds.scan();

        List<File> foundFiles = new ArrayList<>();
        for (String filename : ds.getIncludedFiles()) {
            foundFiles.add(newBasedir.toPath().resolve(filename).toFile());
        }
        return foundFiles;
    }

    /**
     * The Class ResultCollector.
     */
    class ResultCollector {

        /** The success count. */
        int successCount;

        /** The fail count. */
        int failCount;

        /** The skipped count. */
        int skippedCount;

        /** The read only count. */
        int readOnlyCount;
    }

}
