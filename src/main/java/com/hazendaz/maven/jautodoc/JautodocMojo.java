/**
 *    Copyright 2011-2019 the original author or authors.
 *
 *     All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 *     Public License v1.0 which accompanies this distribution, and is available at
 *
 *     https://www.eclipse.org/legal/epl-v10.html.
 */
package com.hazendaz.maven.jautodoc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.jautodoc.preferences.Configuration;
import net.sf.jautodoc.source.SourceManipulator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
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
    private Boolean skip;

    /** The base directory. */
    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File basedir;

    /** Log the files that are being processed. */
    @Parameter(defaultValue = "false", property = "verbose")
    private Boolean verbose;

    /**
     * The mode to use:
     *
     * 'complete' - Complete existing Javadoc, 'keep' - Keep existing Javadoc, 'replace' - Replace existing Javadoc
     */
    @Parameter(property = "mode")
    private String mode;

    /** Comment public members. */
    @Parameter(defaultValue = "true", property = "commentPublic")
    private Boolean commentPublic;

    /** Comment package members. */
    @Parameter(defaultValue = "true", property = "commentPackage")
    private Boolean commentPackage;

    /** Comment protected members. */
    @Parameter(defaultValue = "false", property = "commentProtected")
    private Boolean commentProtected;

    /** Comment private members. */
    @Parameter(defaultValue = "false", property = "commentPrivate")
    private Boolean commentPrivate;

    /** Comment types. */
    @Parameter(defaultValue = "true", property = "commentTypes")
    private Boolean commentTypes;

    /** Comment fields. */
    @Parameter(defaultValue = "true", property = "commentFields")
    private Boolean commentFields;

    /** Comment methods. [TODO NOT SET] */
    @Parameter(defaultValue = "true", property = "commentMethods")
    private Boolean commentMethods;

    /** Comment get/set only. */
    @Parameter(defaultValue = "false", property = "commentGetterSetterOnly")
    private Boolean commentGetterSetterOnly;

    /** Comment exclude getter/setter. */
    @Parameter(defaultValue = "false", property = "excludeGetterSetter")
    private Boolean excludeGetterSetter;

    /** Add 'todo' auto generated javadoc. */
    @Parameter(defaultValue = "false", property = "addTodoForAutodoc")
    private Boolean addTodoForAutodoc;

    /** Create comment from element name. */
    @Parameter(defaultValue = "true", property = "createDummyComment")
    private Boolean createDummyComment;

    /** Single line field comment. */
    @Parameter(defaultValue = "true", property = "singleLineComment")
    private Boolean singleLineComment;

    /** Use Eclipse comment formatter. */
    @Parameter(defaultValue = "false", property = "useEclipseFormatter")
    private Boolean useEclipseFormatter;

    /** [G,S]etter from field comment. */
    @Parameter(defaultValue = "false", property = "getterSetterFromField")
    private Boolean getterSetterFromField;

    /** First sentence only. */
    @Parameter(defaultValue = "false", property = "getterSetterFromFieldFirst")
    private Boolean getterSetterFromFieldFirst;

    /** Replace existing getter/setter. */
    @Parameter(defaultValue = "true", property = "getterSetterFromFieldReplace")
    private Boolean getterSetterFromFieldReplace;

    /** Add file header. */
    @Parameter(defaultValue = "false", property = "addHeader")
    private Boolean addHeader;

    /** Replace existing header. */
    @Parameter(defaultValue = "false", property = "replaceHeader")
    private Boolean replaceHeader;

    /** Multi comment header. */
    @Parameter(defaultValue = "false", property = "multiCommentHeader")
    private Boolean multiCommentHeader;

    /** Add header only (No Javadoc created). [TODO Not Set] */
    @Parameter(defaultValue = "false", property = "headerOnly")
    private Boolean headerOnly;

    /** Maven ProjectHelper. */
    @Component
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

            IWorkspace ws = ResourcesPlugin.getWorkspace();
            IProject project = ws.getRoot().getProject("External Files");
            if (!project.exists()) {
                try {
                    project.create(null);
                } catch (CoreException e) {
                    log.error("unable to create Workspace" + e.getMessage());
                    return;
                }
            }

            for (int i = 0, n = files.size(); i < n; i++) {
                File file = files.get(i);
                if (file.exists()) {
                    if (file.canWrite()) {
                        ICompilationUnit compilationUnit = JavaCore
                                .createCompilationUnitFrom(project.getFile(new Path(file.getPath())));
                        SourceManipulator source;
                        try {
                            source = new SourceManipulator(compilationUnit, this.loadConfiguration());
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

            // storeFileHashCache(hashCache);

            long endClock = System.currentTimeMillis();

            log.info("Successfully formatted:          " + rc.successCount + FILE_S);
            log.info("Fail to format:                  " + rc.failCount + FILE_S);
            log.info("Skipped:                         " + rc.skippedCount + FILE_S);
            log.info("Read only skipped:               " + rc.readOnlyCount + FILE_S);
            log.info("Approximate time taken:          " + ((endClock - startClock) / 1000) + "s");
        }

    }

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
     * @param files
     *            the files
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
            foundFiles.add(new File(newBasedir, filename));
        }
        return foundFiles;
    }

    class ResultCollector {

        int successCount;

        int failCount;

        int skippedCount;

        int readOnlyCount;
    }

}
