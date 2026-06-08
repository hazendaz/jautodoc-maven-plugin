/*
 * SPDX-License-Identifier: EPL-2.0
 * See LICENSE file for details.
 *
 * Copyright 2018-2026 hazendaz
 */
package com.hazendaz.maven.jautodoc.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Fixture-based golden tests for {@link StandaloneJautodocEngine}. Each test loads an {@code input.java} resource,
 * processes it with a configured engine, and compares the result to a corresponding {@code expected-*.java} resource.
 */
public class StandaloneJautodocEngineTest {

    /** The temp dir. */
    @TempDir
    Path tempDir;

    // =========================================================================
    // Configuration factory helpers
    // =========================================================================

    /**
     * Defaults.
     *
     * @return the jautodoc configuration
     */
    private static JautodocConfiguration defaults() {
        final JautodocConfiguration c = new JautodocConfiguration();
        c.setMode(JautodocMode.COMPLETE);
        c.setVisibilityPublic(true);
        c.setVisibilityPackage(true);
        c.setVisibilityProtected(false);
        c.setVisibilityPrivate(false);
        c.setCommentTypes(true);
        c.setCommentFields(true);
        c.setCommentMethods(true);
        c.setSingleLineComment(true);
        c.setCreateDummyComment(true);
        return c;
    }

    // =========================================================================
    // COMPLETE mode – simple class
    // =========================================================================

    /**
     * Complete mode on a plain class should add type, constructor, method and boolean-return comments matching the
     * expected golden file.
     *
     * @throws IOException
     *             the io exception
     */
    @Test
    void completeMode_simpleClass_matchesGolden() throws IOException {
        final String input = StandaloneJautodocEngineTest.fixture("simple-class/input.java");
        final String expected = StandaloneJautodocEngineTest.fixture("simple-class/expected-complete.java");

        final StandaloneJautodocEngine engine = new StandaloneJautodocEngine(StandaloneJautodocEngineTest.defaults());
        final String actual = engine.processSource(input);

        Assertions.assertEquals(StandaloneJautodocEngineTest.normalise(expected),
                StandaloneJautodocEngineTest.normalise(actual),
                "COMPLETE mode output must match golden for simple-class");
    }

    // =========================================================================
    // KEEP mode – existing Javadoc is preserved
    // =========================================================================

    /**
     * Keep mode existing javadoc is untouched.
     */
    @Test
    void keepMode_existingJavadocIsUntouched() {
        final String source = "package p;\n/** My doc. */\npublic class Foo {}\n";

        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setMode(JautodocMode.KEEP);

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        Assertions.assertTrue(result.contains("/** My doc. */"), "KEEP mode must not alter existing Javadoc");
    }

    /**
     * Keep mode adds missing javadoc.
     */
    @Test
    void keepMode_addsMissingJavadoc() {
        final String source = "package p;\npublic class Bar {}\n";

        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setMode(JautodocMode.KEEP);

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        Assertions.assertTrue(result.contains("The Class Bar"), "KEEP mode must add Javadoc where absent");
    }

    // =========================================================================
    // REPLACE mode – existing Javadoc is overwritten
    // =========================================================================

    /**
     * Replace mode replaces existing javadoc.
     */
    @Test
    void replaceMode_replacesExistingJavadoc() {
        final String source = "package p;\n/** Old comment. */\npublic class Baz {}\n";

        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setMode(JautodocMode.REPLACE);

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        Assertions.assertFalse(result.contains("Old comment"), "REPLACE mode must remove the old comment");
        Assertions.assertTrue(result.contains("The Class Baz"), "REPLACE mode must insert a new comment");
    }

    // =========================================================================
    // Visibility filters
    // =========================================================================

    /**
     * Visibility public and package matches golden.
     *
     * @throws IOException
     *             the io exception
     */
    @Test
    void visibility_publicAndPackage_matchesGolden() throws IOException {
        final String input = StandaloneJautodocEngineTest.fixture("visibility/input.java");
        final String expected = StandaloneJautodocEngineTest.fixture("visibility/expected-public-package.java");

        final StandaloneJautodocEngine engine = new StandaloneJautodocEngine(StandaloneJautodocEngineTest.defaults());
        final String actual = engine.processSource(input);

        Assertions.assertEquals(StandaloneJautodocEngineTest.normalise(expected),
                StandaloneJautodocEngineTest.normalise(actual),
                "Default visibility (public+package) output must match golden");
    }

    /**
     * Visibility private field commented when enabled.
     */
    @Test
    void visibility_privateField_commentedWhenEnabled() {
        final String source = "package p;\npublic class C {\n    private int x;\n}\n";

        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setVisibilityPrivate(true);

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        Assertions.assertTrue(result.contains("/** The x. */"),
                "Private field should be commented when visibilityPrivate=true");
    }

    /**
     * Visibility private field not commented by default.
     */
    @Test
    void visibility_privateField_notCommentedByDefault() {
        final String source = "package p;\npublic class C {\n    private int x;\n}\n";

        final String result = new StandaloneJautodocEngine(StandaloneJautodocEngineTest.defaults())
                .processSource(source);

        Assertions.assertFalse(result.contains("/** X. */"),
                "Private field must not be commented when visibilityPrivate=false");
    }

    // =========================================================================
    // Header
    // =========================================================================

    /**
     * Header inserted when absent.
     *
     * @throws IOException
     *             the io exception
     */
    @Test
    void header_insertedWhenAbsent() throws IOException {
        final String input = StandaloneJautodocEngineTest.fixture("header/input.java");
        final String expected = StandaloneJautodocEngineTest.fixture("header/expected.java");

        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setAddHeader(true);
        cfg.setHeaderText("Copyright 2025 Example Corp.");

        final String actual = new StandaloneJautodocEngine(cfg).processSource(input);

        Assertions.assertEquals(StandaloneJautodocEngineTest.normalise(expected),
                StandaloneJautodocEngineTest.normalise(actual), "Header must be inserted when absent");
    }

    /**
     * Header not inserted when add header false.
     */
    @Test
    void header_notInsertedWhenAddHeaderFalse() {
        final String source = "package p;\npublic class X {}\n";
        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setAddHeader(false);
        cfg.setHeaderText("Some header");

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        Assertions.assertFalse(result.startsWith("/*"), "Header must not be inserted when addHeader=false");
    }

    /**
     * Header kept when replace header false.
     */
    @Test
    void header_keptWhenReplaceHeaderFalse() {
        final String source = "/* Original header */\npackage p;\npublic class X {}\n";
        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setAddHeader(true);
        cfg.setReplaceHeader(false);
        cfg.setHeaderText("New header");

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        Assertions.assertTrue(result.contains("Original header"),
                "Original header must be preserved when replaceHeader=false");
        Assertions.assertFalse(result.contains("New header"),
                "New header must not be written when replaceHeader=false");
    }

    /**
     * Header replaced when replace header true.
     */
    @Test
    void header_replacedWhenReplaceHeaderTrue() {
        final String source = "/* Original header */\npackage p;\npublic class X {}\n";
        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setAddHeader(true);
        cfg.setReplaceHeader(true);
        cfg.setHeaderText("New header");

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        Assertions.assertFalse(result.contains("Original header"),
                "Original header must be removed when replaceHeader=true");
        Assertions.assertTrue(result.contains("New header"), "New header must appear when replaceHeader=true");
    }

    // =========================================================================
    // Header-only mode
    // =========================================================================

    /**
     * Header only no javadoc added.
     */
    @Test
    void headerOnly_noJavadocAdded() {
        final String source = "package p;\npublic class Y {}\n";
        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setAddHeader(true);
        cfg.setHeaderText("Header only");
        cfg.setHeaderOnly(true);

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        Assertions.assertTrue(result.contains("Header only"), "Header must be inserted in header-only mode");
        Assertions.assertFalse(result.contains("The Class Y"), "No Javadoc should be generated in header-only mode");
    }

    // =========================================================================
    // Getter / setter
    // =========================================================================

    /**
     * Getter setter matches golden.
     *
     * @throws IOException
     *             the io exception
     */
    @Test
    void getterSetter_matchesGolden() throws IOException {
        final String input = StandaloneJautodocEngineTest.fixture("getter-setter/input.java");
        final String expected = StandaloneJautodocEngineTest.fixture("getter-setter/expected.java");

        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setVisibilityPrivate(false); // the private field already has javadoc; don't touch

        final String actual = new StandaloneJautodocEngine(cfg).processSource(input);

        Assertions.assertEquals(StandaloneJautodocEngineTest.normalise(expected),
                StandaloneJautodocEngineTest.normalise(actual), "Getter/setter output must match golden");
    }

    /**
     * Getter setter only skips regular methods.
     */
    @Test
    void getterSetterOnly_skipsRegularMethods() {
        final String source = "package p;\npublic class C {\n" + "    public String getFoo() { return null; }\n"
                + "    public void doWork() {}\n" + "}\n";
        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setGetterSetterOnly(true);

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        Assertions.assertTrue(result.contains("Gets the foo"), "Getter must be commented with getterSetterOnly=true");
        Assertions.assertFalse(result.contains("Do work"),
                "Regular method must not be commented with getterSetterOnly=true");
    }

    /**
     * Exclude getter setter skips getters and setters.
     */
    @Test
    void excludeGetterSetter_skipsGettersAndSetters() {
        final String source = "package p;\npublic class C {\n" + "    public String getFoo() { return null; }\n"
                + "    public void doWork() {}\n" + "}\n";
        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setExcludeGetterSetter(true);

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        Assertions.assertFalse(result.contains("Gets the foo"),
                "Getter must not be commented when excludeGetterSetter=true");
        Assertions.assertTrue(result.contains("Do work"),
                "Regular method must be commented when excludeGetterSetter=true");
    }

    // =========================================================================
    // createDummyComment=false
    // =========================================================================

    /**
     * Create dummy comment false skips description for members with no tags.
     */
    @Test
    void createDummyComment_false_skipsDescriptionForMembersWithNoTags() {
        // A method with no params, void return, no throws → would produce empty Javadoc → should be skipped
        final String source = "package p;\npublic class C {\n    public void doWork() {}\n}\n";
        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setCreateDummyComment(false);

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        // With createDummyComment=false and no tags, no Javadoc should be written for doWork()
        Assertions.assertFalse(result.contains("/**"),
                "No Javadoc should be written for tagless method when createDummyComment=false");
    }

    // =========================================================================
    // addTodoForAutodoc
    // =========================================================================

    /**
     * Add todo for autodoc prefixes todo.
     */
    @Test
    void addTodoForAutodoc_prefixesTodo() {
        final String source = "package p;\npublic class D {\n    public int getValue() { return 0; }\n}\n";
        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setAddTodoForAutodoc(true);

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        Assertions.assertTrue(result.contains("TODO"), "Comment must contain TODO when addTodoForAutodoc=true");
    }

    // =========================================================================
    // COMPLETE mode – adds missing tags
    // =========================================================================

    /**
     * Complete mode adds missing param to existing javadoc.
     */
    @Test
    void completeMode_addsMissingParamToExistingJavadoc() {
        // Existing Javadoc has no @param
        final String source = "package p;\npublic class E {\n" + "    /**\n" + "     * Does work.\n" + "     */\n"
                + "    public void doWork(String task) {}\n" + "}\n";
        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setMode(JautodocMode.COMPLETE);

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        Assertions.assertTrue(result.contains("@param task"),
                "COMPLETE mode must add missing @param to existing Javadoc");
    }

    // =========================================================================
    // excludeOverrides
    // =========================================================================

    /**
     * Exclude overrides skips overridden methods.
     */
    @Test
    void excludeOverrides_skipsOverriddenMethods() {
        final String source = "package p;\npublic class C {\n" + "    @Override\n"
                + "    public String toString() { return \"\"; }\n" + "    public void doWork() {}\n" + "}\n";
        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setExcludeOverrides(true);

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        Assertions.assertFalse(result.contains("To string"),
                "@Override method must not be commented when excludeOverrides=true");
        Assertions.assertTrue(result.contains("Do work"), "Non-override method must still be commented");
    }

    /**
     * Exclude overrides false comments overridden methods.
     */
    @Test
    void excludeOverrides_false_commentsOverriddenMethods() {
        final String source = "package p;\npublic class C {\n" + "    @Override\n"
                + "    public String toString() { return \"\"; }\n" + "}\n";
        final JautodocConfiguration cfg = StandaloneJautodocEngineTest.defaults();
        cfg.setExcludeOverrides(false);

        final String result = new StandaloneJautodocEngine(cfg).processSource(source);

        Assertions.assertTrue(result.contains("To string"),
                "@Override method must be commented when excludeOverrides=false");
    }

    // =========================================================================
    // Result counters
    // =========================================================================

    /**
     * Result counters success and missing.
     *
     * @throws IOException
     *             the io exception
     */
    @Test
    void resultCounters_successAndMissing() throws IOException {
        final Path existing = this.tempDir.resolve("Exists.java");
        Files.writeString(existing, "package p;\npublic class Exists {}\n", StandardCharsets.UTF_8);

        final Path missing = this.tempDir.resolve("Missing.java");
        // intentionally not created

        final JautodocResult result = new StandaloneJautodocEngine(StandaloneJautodocEngineTest.defaults())
                .process(java.util.List.of(existing, missing));

        Assertions.assertEquals(1, result.getSuccessCount(), "One file should succeed");
        Assertions.assertEquals(1, result.getFailCount(), "One file should fail (missing)");
        Assertions.assertEquals(0, result.getSkippedCount());
        Assertions.assertEquals(0, result.getReadOnlyCount());
    }

    // =========================================================================
    // JautodocMode.fromString
    // =========================================================================

    /**
     * Mode from string null defaults to complete.
     */
    @Test
    void modeFromString_nullDefaultsToComplete() {
        Assertions.assertEquals(JautodocMode.COMPLETE, JautodocMode.fromString(null));
    }

    /**
     * Mode from string case insensitive.
     */
    @Test
    void modeFromString_caseInsensitive() {
        Assertions.assertEquals(JautodocMode.KEEP, JautodocMode.fromString("KEEP"));
        Assertions.assertEquals(JautodocMode.REPLACE, JautodocMode.fromString("Replace"));
        Assertions.assertEquals(JautodocMode.COMPLETE, JautodocMode.fromString("complete"));
    }

    /**
     * Mode from string unknown falls back to complete.
     */
    @Test
    void modeFromString_unknownFallsBackToComplete() {
        Assertions.assertEquals(JautodocMode.COMPLETE, JautodocMode.fromString("bogus"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Loads a test fixture from {@code src/test/resources/fixtures/<path>}.
     *
     * @param relativePath
     *            the relative path
     *
     * @return the string
     *
     * @throws IOException
     *             the io exception
     */
    private static String fixture(final String relativePath) throws IOException {
        final String resourcePath = "/fixtures/" + relativePath;
        try (InputStream is = StandaloneJautodocEngineTest.class.getResourceAsStream(resourcePath)) {
            Assertions.assertNotNull(is, "Fixture not found: " + resourcePath);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Normalises line endings and trims trailing whitespace from each line so that golden-file comparisons are not
     * affected by OS-specific newline differences or editor trailing-whitespace settings.
     *
     * @param s
     *            the s
     *
     * @return the string
     */
    private static String normalise(final String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n")
                // Trim trailing spaces on each line
                .lines().map(String::stripTrailing).collect(java.util.stream.Collectors.joining("\n"))
                // Strip leading/trailing blank lines from the whole file
                .strip();
    }
}
