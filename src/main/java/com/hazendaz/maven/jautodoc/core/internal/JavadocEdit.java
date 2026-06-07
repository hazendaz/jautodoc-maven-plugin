/*
 * SPDX-License-Identifier: EPL-2.0
 * See LICENSE file for details.
 *
 * Copyright 2018-2026 hazendaz
 */
package com.hazendaz.maven.jautodoc.core.internal;

/**
 * A pending text replacement or insertion within a Java source string.
 * <p>
 * Edits are applied in descending offset order so that earlier offsets remain valid as later ones are applied first.
 */
final class JavadocEdit {

    /** Start character offset in the source string. */
    final int offset;

    /** Number of characters to replace (0 = pure insert). */
    final int length;

    /** Replacement text to write at {@code offset}. */
    final String text;

    /**
     * Instantiates a new javadoc edit.
     *
     * @param offset
     *            the offset
     * @param length
     *            the length
     * @param text
     *            the text
     */
    JavadocEdit(int offset, int length, String text) {
        this.offset = offset;
        this.length = length;
        this.text = text;
    }
}
