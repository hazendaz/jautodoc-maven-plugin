/*
 * SPDX-License-Identifier: EPL-2.0
 * See LICENSE file for details.
 *
 * Copyright 2018-2026 hazendaz
 */
package com.hazendaz.maven.jautodoc.core;

import com.hazendaz.maven.jautodoc.core.internal.HeaderProcessor;
import com.hazendaz.maven.jautodoc.core.internal.JavaSourceProcessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

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

    private final JautodocConfiguration config;

    /**
     * Instantiates a new standalone jautodoc engine.
     *
     * @param config
     *            the config
     */
    public StandaloneJautodocEngine(JautodocConfiguration config) {
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
    public JautodocResult process(List<Path> files) {
        int success = 0;
        int fail = 0;
        int skipped = 0;
        int readOnly = 0;

        JavaSourceProcessor sourceProcessor = new JavaSourceProcessor(config);

        for (Path file : files) {
            if (!Files.exists(file)) {
                fail++;
                continue;
            }
            if (!Files.isWritable(file)) {
                readOnly++;
                continue;
            }
            try {
                String original = Files.readString(file, StandardCharsets.UTF_8);
                String result = processSource(original, sourceProcessor);

                if (!result.equals(original)) {
                    Files.writeString(file, result, StandardCharsets.UTF_8);
                }
                success++;
            } catch (IOException e) {
                skipped++;
            } catch (Exception e) {
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
    public String processSource(String source) {
        return processSource(source, new JavaSourceProcessor(config));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String processSource(String source, JavaSourceProcessor sourceProcessor) {
        String result = source;

        // 1. Header
        result = HeaderProcessor.process(result, config);

        // 2. Javadoc
        result = sourceProcessor.process(result);

        // 3. Eclipse formatter (optional)
        if (config.isUseEclipseFormatter()) {
            result = format(result);
        }

        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String format(String source) {
        Map options = Map.of("org.eclipse.jdt.core.compiler.source", "21", "org.eclipse.jdt.core.compiler.compliance",
                "21", "org.eclipse.jdt.core.compiler.codegen.targetPlatform", "21");
        CodeFormatter formatter = ToolFactory.createCodeFormatter(options);
        TextEdit edit = formatter.format(CodeFormatter.K_COMPILATION_UNIT, source, 0, source.length(), 0, null);
        if (edit == null) {
            return source;
        }
        Document doc = new Document(source);
        try {
            edit.apply(doc);
        } catch (BadLocationException e) {
            return source;
        }
        return doc.get();
    }
}
