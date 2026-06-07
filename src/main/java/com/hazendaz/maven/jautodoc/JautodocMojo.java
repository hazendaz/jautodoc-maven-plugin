/*
 *     Copyright 2011-2026 the original author or authors.
 *
 *     All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 *     Public License v1.0 which accompanies this distribution, and is available at
 *
 *     https://www.eclipse.org/legal/epl-v10.html.
 */
package com.hazendaz.maven.jautodoc;

import com.hazendaz.maven.jautodoc.core.JautodocConfiguration;
import com.hazendaz.maven.jautodoc.core.JautodocMode;
import com.hazendaz.maven.jautodoc.core.JautodocResult;
import com.hazendaz.maven.jautodoc.core.StandaloneJautodocEngine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

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

    /** Exclude methods annotated with @Override. */
    @Parameter(defaultValue = "true", property = "excludeOverrides")
    private boolean excludeOverrides;

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
            try {
                JautodocConfiguration configuration = this.loadConfiguration();
                StandaloneJautodocEngine engine = new StandaloneJautodocEngine(configuration);
                JautodocResult rc = engine.process(files.stream().map(File::toPath).collect(Collectors.toList()));

                // Finish processing
                long endClock = System.currentTimeMillis();

                log.info("Successfully formatted: " + rc.getSuccessCount() + FILE_S);
                log.info("Fail to format:         " + rc.getFailCount() + FILE_S);
                log.info("Skipped:                " + rc.getSkippedCount() + FILE_S);
                log.info("Read only skipped:      " + rc.getReadOnlyCount() + FILE_S);
                log.info("Approximate time taken: " + ((endClock - startClock) / 1000) + "s");
            } catch (RuntimeException e) {
                throw new MojoExecutionException("Unable to process sources", e);
            }
        }

    }

    /**
     * Load configuration.
     *
     * @return the configuration
     */
    private JautodocConfiguration loadConfiguration() {
        final JautodocConfiguration configuration = new JautodocConfiguration();
        configuration.setAddHeader(this.addHeader);
        configuration.setAddTodoForAutodoc(this.addTodoForAutodoc);
        configuration.setCommentFields(this.commentFields);
        configuration.setCommentMethods(this.commentMethods);
        configuration.setCommentTypes(this.commentTypes);
        configuration.setCreateDummyComment(this.createDummyComment);
        configuration.setExcludeGetterSetter(this.excludeGetterSetter);
        configuration.setExcludeOverrides(this.excludeOverrides);
        configuration.setGetterSetterFromField(this.getterSetterFromField);
        configuration.setGetterSetterFromFieldFirst(this.getterSetterFromFieldFirst);
        configuration.setGetterSetterFromFieldReplace(this.getterSetterFromFieldReplace);
        configuration.setGetterSetterOnly(this.commentGetterSetterOnly);
        configuration.setMultiCommentHeader(this.multiCommentHeader);
        configuration.setReplaceHeader(this.replaceHeader);
        configuration.setSingleLineComment(this.singleLineComment);
        configuration.setUseEclipseFormatter(this.useEclipseFormatter);
        configuration.setVisibilityPackage(this.commentPackage);
        configuration.setVisibilityPrivate(this.commentPrivate);
        configuration.setVisibilityProtected(this.commentProtected);
        configuration.setVisibilityPublic(this.commentPublic);
        configuration.setHeaderOnly(this.headerOnly);
        configuration.setMode(JautodocMode.fromString(this.mode));
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

}
