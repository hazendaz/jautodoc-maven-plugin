/*
 *     Copyright 2011-2026 the original author or authors.
 *
 *     All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 *     Public License v1.0 which accompanies this distribution, and is available at
 *
 *     https://www.eclipse.org/legal/epl-v10.html.
 */
package com.hazendaz.maven.jautodoc.core;

import java.util.Locale;

/**
 * The Javadoc processing mode, matching Eclipse JAutodoc's three-way mode contract.
 */
public enum JautodocMode {

    /**
     * Complete existing Javadoc: where Javadoc is absent add it; where it exists add any missing tags
     * (@param, @return, @throws).
     */
    COMPLETE,

    /**
     * Keep existing Javadoc unchanged. Only add Javadoc where completely absent.
     */
    KEEP,

    /**
     * Replace all existing Javadoc with freshly generated comments.
     */
    REPLACE;

    /**
     * Resolves a mode from a plugin parameter string. Falls back to {@link #COMPLETE} for null/blank/unknown values.
     *
     * @param mode
     *            the mode string (case-insensitive), may be null
     *
     * @return the resolved JautodocMode, never null
     */
    public static JautodocMode fromString(String mode) {
        if (mode == null || mode.isBlank()) {
            return COMPLETE;
        }
        switch (mode.trim().toLowerCase(Locale.ROOT)) {
            case "complete":
                return COMPLETE;
            case "keep":
                return KEEP;
            case "replace":
                return REPLACE;
            default:
                return COMPLETE;
        }
    }
}
