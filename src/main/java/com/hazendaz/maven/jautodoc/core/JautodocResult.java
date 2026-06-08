/*
 * SPDX-License-Identifier: EPL-2.0
 * See LICENSE file for details.
 *
 * Copyright 2018-2026 hazendaz
 */
package com.hazendaz.maven.jautodoc.core;

/**
 * Aggregated result counts from a single Jautodoc processing run.
 */
public final class JautodocResult {

    /** Number of files successfully processed (written back if changed). */
    private final int successCount;

    /** Number of files that could not be found on disk. */
    private final int failCount;

    /** Number of files skipped due to a processing exception. */
    private final int skippedCount;

    /** Number of files skipped because they were read-only. */
    private final int readOnlyCount;

    /**
     * Instantiates a new jautodoc result.
     *
     * @param successCount
     *            the success count
     * @param failCount
     *            the fail count
     * @param skippedCount
     *            the skipped count
     * @param readOnlyCount
     *            the read only count
     */
    public JautodocResult(final int successCount, final int failCount, final int skippedCount,
            final int readOnlyCount) {
        this.successCount = successCount;
        this.failCount = failCount;
        this.skippedCount = skippedCount;
        this.readOnlyCount = readOnlyCount;
    }

    /**
     * Gets the success count.
     *
     * @return the success count
     */
    public int getSuccessCount() {
        return this.successCount;
    }

    /**
     * Gets the fail count.
     *
     * @return the fail count
     */
    public int getFailCount() {
        return this.failCount;
    }

    /**
     * Gets the skipped count.
     *
     * @return the skipped count
     */
    public int getSkippedCount() {
        return this.skippedCount;
    }

    /**
     * Gets the read only count.
     *
     * @return the read only count
     */
    public int getReadOnlyCount() {
        return this.readOnlyCount;
    }
}
