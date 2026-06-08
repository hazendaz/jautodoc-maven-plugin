/*
 * SPDX-License-Identifier: EPL-2.0
 * See LICENSE file for details.
 *
 * Copyright 2018-2026 hazendaz
 */
package com.hazendaz.maven.jautodoc.core;

import com.hazendaz.maven.jautodoc.core.internal.HeaderProcessor;
import com.hazendaz.maven.jautodoc.core.internal.JavaSourceProcessor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

/**
 * Standalone Jautodoc engine: processes Java source files entirely from the file system using Eclipse JDT AST / text
 * APIs, with no Eclipse workspace or {@code ResourcesPlugin} required.
 * <p>
 * Processing order per file:
 * <ol>
 * <li>Read source as UTF-8 text.
 * <li>Apply header logic ({@link HeaderProcessor}).
 * <li>Apply Javadoc insertion/replacement ({@link JavaSourceProcessor}).
 * <li>Optionally run the Eclipse JDT formatter ({@link CodeFormatter}).
 * <li>Write back only when the content has changed.
 * </ol>
 */
public final class StandaloneJautodocEngine {

    /** The config. */
    private final JautodocConfiguration config;

    /**
     * Instantiates a new standalone jautodoc engine.
     *
     * @param config
     *            the config
     */
    public StandaloneJautodocEngine(final JautodocConfiguration config) {
        this.config = config;
    }

    /**
     * Processes each file in the list and returns an aggregate result.
     *
     * @param files
     *            the files
     *
     * @return the jautodoc result
     */
    public JautodocResult process(final List<Path> files) {
        var success = 0;
        var fail = 0;
        var skipped = 0;
        var readOnly = 0;

        final var sourceProcessor = new JavaSourceProcessor(this.config);

        for (final Path file : files) {
            if (!Files.exists(file)) {
                fail++;
                continue;
            }
            if (!Files.isWritable(file)) {
                readOnly++;
                continue;
            }
            try {
                final var original = Files.readString(file, StandardCharsets.UTF_8);
                final var result = this.processSource(original, sourceProcessor);

                if (!result.equals(original)) {
                    Files.writeString(file, result, StandardCharsets.UTF_8);
                }
                success++;
            } catch (final Exception e) {
                skipped++;
            }
        }

        return new JautodocResult(success, fail, skipped, readOnly);
    }

    /**
     * Processes a source string in memory (useful for testing without touching the file system).
     *
     * @param source
     *            the source
     *
     * @return the string
     */
    public String processSource(final String source) {
        return this.processSource(source, new JavaSourceProcessor(this.config));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Process source.
     *
     * @param source
     *            the source
     * @param sourceProcessor
     *            the source processor
     *
     * @return the string
     */
    private String processSource(final String source, final JavaSourceProcessor sourceProcessor) {
        var result = source;

        // 1. Header
        result = HeaderProcessor.process(result, this.config);

        // 2. Javadoc
        result = sourceProcessor.process(result);

        // 3. Eclipse formatter (optional)
        if (this.config.isUseEclipseFormatter()) {
            result = this.format(result);
        }

        return result;
    }

    /**
     * Format.
     *
     * @param source
     *            the source
     *
     * @return the string
     */
    @SuppressWarnings({ "rawtypes" })
    private String format(final String source) {
        final Map options = Map.of("org.eclipse.jdt.core.compiler.source", "21",
                "org.eclipse.jdt.core.compiler.compliance", "21",
                "org.eclipse.jdt.core.compiler.codegen.targetPlatform", "21");
        final var formatter = ToolFactory.createCodeFormatter(options);
        final var edit = formatter.format(CodeFormatter.K_COMPILATION_UNIT, source, 0, source.length(), 0, null);
        if (edit == null) {
            return source;
        }
        final var doc = new Document(source);
        try {
            edit.apply(doc);
        } catch (final BadLocationException e) {
            return source;
        }
        return doc.get();
    }
}
