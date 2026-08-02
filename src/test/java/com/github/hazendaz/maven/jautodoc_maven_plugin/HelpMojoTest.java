/*
 * SPDX-License-Identifier: EPL-2.0
 * See LICENSE file for details.
 *
 * Copyright 2018-2026 hazendaz
 */
package com.github.hazendaz.maven.jautodoc_maven_plugin;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for generated {@link HelpMojo} using the Maven plugin testing harness.
 */
@MojoTest
class HelpMojoTest {

    /**
     * Help mojo lists available goals.
     *
     * @param mojo
     *            the mojo
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @InjectMojo(goal = "help")
    void helpListsAvailableGoals(final HelpMojo mojo) throws Exception {
        final var log = new CapturingLog();
        mojo.setLog(log);

        mojo.execute();

        final String info = log.info();
        Assertions.assertTrue(info.contains("jautodoc:help"), "Help goal should be listed");
        Assertions.assertTrue(info.contains("jautodoc:jautodoc"), "Plugin goal should be listed");
    }

    /**
     * Detailed help shows main mojo parameters.
     *
     * @param mojo
     *            the mojo
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @InjectMojo(goal = "help")
    @MojoParameter(name = "detail", value = "true")
    @MojoParameter(name = "goal", value = "jautodoc")
    void detailedHelpShowsMainMojoParameters(final HelpMojo mojo) throws Exception {
        final var log = new CapturingLog();
        mojo.setLog(log);

        mojo.execute();

        final String info = log.info();
        Assertions.assertTrue(info.contains("Available parameters:"), "Detailed help should show parameters");
        Assertions.assertTrue(info.contains("commentFields"), "Main mojo parameters should be listed");
        Assertions.assertTrue(info.contains("skip"), "Skip parameter should be listed");
    }

    /**
     * Capturing log.
     */
    private static final class CapturingLog implements Log {

        /** The info lines. */
        private final List<String> infoLines = new ArrayList<>();

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void debug(final CharSequence content) {
            // no-op
        }

        @Override
        public void debug(final CharSequence content, final Throwable error) {
            // no-op
        }

        @Override
        public void debug(final Throwable error) {
            // no-op
        }

        @Override
        public boolean isInfoEnabled() {
            return true;
        }

        @Override
        public void info(final CharSequence content) {
            this.infoLines.add(String.valueOf(content));
        }

        @Override
        public void info(final CharSequence content, final Throwable error) {
            this.info(content);
        }

        @Override
        public void info(final Throwable error) {
            this.info(error.getMessage());
        }

        @Override
        public boolean isWarnEnabled() {
            return false;
        }

        @Override
        public void warn(final CharSequence content) {
            // no-op
        }

        @Override
        public void warn(final CharSequence content, final Throwable error) {
            // no-op
        }

        @Override
        public void warn(final Throwable error) {
            // no-op
        }

        @Override
        public boolean isErrorEnabled() {
            return false;
        }

        @Override
        public void error(final CharSequence content) {
            // no-op
        }

        @Override
        public void error(final CharSequence content, final Throwable error) {
            // no-op
        }

        @Override
        public void error(final Throwable error) {
            // no-op
        }

        /**
         * Info.
         *
         * @return the string
         */
        private String info() {
            return String.join(System.lineSeparator(), this.infoLines);
        }
    }
}
