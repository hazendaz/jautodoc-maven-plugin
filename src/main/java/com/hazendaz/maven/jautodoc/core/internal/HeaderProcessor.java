/*
 *     Copyright 2011-2026 the original author or authors.
 *
 *     All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 *     Public License v1.0 which accompanies this distribution, and is available at
 *
 *     https://www.eclipse.org/legal/epl-v10.html.
 */
package com.hazendaz.maven.jautodoc.core.internal;

import com.hazendaz.maven.jautodoc.core.JautodocConfiguration;

/**
 * Handles file-header insertion and replacement without requiring an Eclipse workspace.
 * <p>
 * A "header" is a block comment that appears before the package declaration. Only the very first block comment in the
 * file is treated as a potential header; Javadoc on the package declaration is left untouched.
 */
public final class HeaderProcessor {

    private HeaderProcessor() {
    }

    /**
     * Processes the header section of a Java source string according to {@code config}.
     * <p>
     * No-ops when:
     * <ul>
     * <li>{@code addHeader} is false, or</li>
     * <li>{@code headerText} is blank.
     * </ul>
     *
     * @param source
     *            the source text
     * @param config
     *            the configuration
     *
     * @return the source text with header applied; identical to the input when no action is needed
     */
    public static String process(String source, JautodocConfiguration config) {
        if (!config.isAddHeader() || config.getHeaderText() == null || config.getHeaderText().isBlank()) {
            return source;
        }

        String newHeader = buildHeaderComment(config);
        int existingEnd = findExistingHeaderEnd(source);

        if (existingEnd >= 0) {
            if (!config.isReplaceHeader()) {
                return source; // keep existing header
            }
            // Replace existing header, stripping any leading blank lines between it and the rest
            String remainder = source.substring(existingEnd);
            // Strip only the immediately following newline(s) so we can re-add one
            int skip = 0;
            while (skip < remainder.length() && remainder.charAt(skip) == '\n'
                    || skip < remainder.length() && remainder.charAt(skip) == '\r') {
                if (remainder.charAt(skip) == '\r' && skip + 1 < remainder.length()
                        && remainder.charAt(skip + 1) == '\n') {
                    skip += 2;
                } else {
                    skip++;
                }
                break; // only skip one newline
            }
            return newHeader + "\n" + remainder.substring(skip);
        }

        // No existing header - prepend
        return newHeader + "\n" + source;
    }

    // -------------------------------------------------------------------------
    // Package-visible helpers (used by tests and JavaSourceProcessor)
    // -------------------------------------------------------------------------

    /**
     * Returns the character offset immediately after the closing {@code *
     /
    }

    of an
    existing header comment,or*
    {@code -1}if
    the file
    does not
    begin with
    a block comment.*<p>*
    Only the
    first non-whitespace token
    is inspected;if
    it is not
    {@code /*} the method returns {@code -1}.
     *
     * @param source
     *            the source
     *
     * @return the character offset after the closing delimiter, or -1
     */
    static int findExistingHeaderEnd(String source) {
        int pos = 0;
        // Skip leading whitespace
        while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
            pos++;
        }
        if (pos + 1 >= source.length()) {
            return -1;
        }
        if (source.charAt(pos) != '/' || source.charAt(pos + 1) != '*') {
            return -1; // file doesn't start with a block comment
        }
        int closeIdx = source.indexOf("*/", pos + 2);
        if (closeIdx < 0) {
            return -1;
        }
        return closeIdx + 2;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String buildHeaderComment(JautodocConfiguration config) {
        String text = config.getHeaderText().trim();
        String open = config.isMultiCommentHeader() ? "/**" : "/*";
        StringBuilder sb = new StringBuilder(open).append('\n');
        for (String line : text.split("\r?\n", -1)) {
            if (line.isEmpty()) {
                sb.append(" *\n");
            } else {
                sb.append(" * ").append(line).append('\n');
            }
        }
        sb.append(" */");
        return sb.toString();
    }
}
