/*
 *     Copyright 2011-2026 the original author or authors.
 *
 *     All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 *     Public License v1.0 which accompanies this distribution, and is available at
 *
 *     https://www.eclipse.org/legal/epl-v10.html.
 */
package com.hazendaz.maven.jautodoc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Fixture-based golden tests for {@link StandaloneJautodocEngine}. Each test loads an {@code input.java} resource,
 * processes it with a configured engine, and compares the result to a corresponding {@code expected-*.java} resource.
 */
class StandaloneJautodocEngineTest {

    @TempDir
    Path tempDir;

    // =========================================================================
    // Configuration factory helpers
    // =========================================================================

    private static JautodocConfiguration defaults() {
        JautodocConfiguration c = new JautodocConfiguration();
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
     */
    @Test
    void completeMode_simpleClass_matchesGolden() throws IOException {
        String input = fixture("simple-class/input.java");
        String expected = fixture("simple-class/expected-complete.java");

        StandaloneJautodocEngine engine = new StandaloneJautodocEngine(defaults());
        String actual = engine.processSource(input);

        assertEquals(normalise(expected), normalise(actual), "COMPLETE mode output must match golden for simple-class");
    }

    // =========================================================================
    // KEEP mode – existing Javadoc is preserved
    // =========================================================================

    @Test
    void keepMode_existingJavadocIsUntouched() {
        String source = "package p;\n/** My doc. */\npublic class Foo {}\n";

        JautodocConfiguration cfg = defaults();
        cfg.setMode(JautodocMode.KEEP);

        String result = new StandaloneJautodocEngine(cfg).processSource(source);

        assertTrue(result.contains("/** My doc. */"), "KEEP mode must not alter existing Javadoc");
    }

    @Test
    void keepMode_addsMissingJavadoc() {
        String source = "package p;\npublic class Bar {}\n";

        JautodocConfiguration cfg = defaults();
        cfg.setMode(JautodocMode.KEEP);

        String result = new StandaloneJautodocEngine(cfg).processSource(source);

        assertTrue(result.contains("The Class Bar"), "KEEP mode must add Javadoc where absent");
    }

    // =========================================================================
    // REPLACE mode – existing Javadoc is overwritten
    // =========================================================================

    @Test
    void replaceMode_replacesExistingJavadoc() {
        String source = "package p;\n/** Old comment. */\npublic class Baz {}\n";

        JautodocConfiguration cfg = defaults();
        cfg.setMode(JautodocMode.REPLACE);

        String result = new StandaloneJautodocEngine(cfg).processSource(source);

        assertFalse(result.contains("Old comment"), "REPLACE mode must remove the old comment");
        assertTrue(result.contains("The Class Baz"), "REPLACE mode must insert a new comment");
    }

    // =========================================================================
    // Visibility filters
    // =========================================================================

    @Test
    void visibility_publicAndPackage_matchesGolden() throws IOException {
        String input = fixture("visibility/input.java");
        String expected = fixture("visibility/expected-public-package.java");

        StandaloneJautodocEngine engine = new StandaloneJautodocEngine(defaults());
        String actual = engine.processSource(input);

        assertEquals(normalise(expected), normalise(actual),
                "Default visibility (public+package) output must match golden");
    }

    @Test
    void visibility_privateField_commentedWhenEnabled() {
        String source = "package p;\npublic class C {\n    private int x;\n}\n";

        JautodocConfiguration cfg = defaults();
        cfg.setVisibilityPrivate(true);

        String result = new StandaloneJautodocEngine(cfg).processSource(source);

        assertTrue(result.contains("/** The x. */"), "Private field should be commented when visibilityPrivate=true");
    }

    @Test
    void visibility_privateField_notCommentedByDefault() {
        String source = "package p;\npublic class C {\n    private int x;\n}\n";

        String result = new StandaloneJautodocEngine(defaults()).processSource(source);

        assertFalse(result.contains("/** X. */"), "Private field must not be commented when visibilityPrivate=false");
    }

    // =========================================================================
    // Header
    // =========================================================================

    @Test
    void header_insertedWhenAbsent() throws IOException {
        String input = fixture("header/input.java");
        String expected = fixture("header/expected.java");

        JautodocConfiguration cfg = defaults();
        cfg.setAddHeader(true);
        cfg.setHeaderText("Copyright 2025 Example Corp.");

        String actual = new StandaloneJautodocEngine(cfg).processSource(input);

        assertEquals(normalise(expected), normalise(actual), "Header must be inserted when absent");
    }

    @Test
    void header_notInsertedWhenAddHeaderFalse() {
        String source = "package p;\npublic class X {}\n";
        JautodocConfiguration cfg = defaults();
        cfg.setAddHeader(false);
        cfg.setHeaderText("Some header");

        String result = new StandaloneJautodocEngine(cfg).processSource(source);

        assertFalse(result.startsWith("/*"), "Header must not be inserted when addHeader=false");
    }

    @Test
    void header_keptWhenReplaceHeaderFalse() {
        String source = "/* Original header */\npackage p;\npublic class X {}\n";
        JautodocConfiguration cfg = defaults();
        cfg.setAddHeader(true);
        cfg.setReplaceHeader(false);
        cfg.setHeaderText("New header");

        String result = new StandaloneJautodocEngine(cfg).processSource(source);

        assertTrue(result.contains("Original header"), "Original header must be preserved when replaceHeader=false");
        assertFalse(result.contains("New header"), "New header must not be written when replaceHeader=false");
    }

    @Test
    void header_replacedWhenReplaceHeaderTrue() {
        String source = "/* Original header */\npackage p;\npublic class X {}\n";
        JautodocConfiguration cfg = defaults();
        cfg.setAddHeader(true);
        cfg.setReplaceHeader(true);
        cfg.setHeaderText("New header");

        String result = new StandaloneJautodocEngine(cfg).processSource(source);

        assertFalse(result.contains("Original header"), "Original header must be removed when replaceHeader=true");
        assertTrue(result.contains("New header"), "New header must appear when replaceHeader=true");
    }

    // =========================================================================
    // Header-only mode
    // =========================================================================

    @Test
    void headerOnly_noJavadocAdded() {
        String source = "package p;\npublic class Y {}\n";
        JautodocConfiguration cfg = defaults();
        cfg.setAddHeader(true);
        cfg.setHeaderText("Header only");
        cfg.setHeaderOnly(true);

        String result = new StandaloneJautodocEngine(cfg).processSource(source);

        assertTrue(result.contains("Header only"), "Header must be inserted in header-only mode");
        assertFalse(result.contains("The Class Y"), "No Javadoc should be generated in header-only mode");
    }

    // =========================================================================
    // Getter / setter
    // =========================================================================

    @Test
    void getterSetter_matchesGolden() throws IOException {
        String input = fixture("getter-setter/input.java");
        String expected = fixture("getter-setter/expected.java");

        JautodocConfiguration cfg = defaults();
        cfg.setVisibilityPrivate(false); // the private field already has javadoc; don't touch

        String actual = new StandaloneJautodocEngine(cfg).processSource(input);

        assertEquals(normalise(expected), normalise(actual), "Getter/setter output must match golden");
    }

    @Test
    void getterSetterOnly_skipsRegularMethods() {
        String source = "package p;\npublic class C {\n" + "    public String getFoo() { return null; }\n"
                + "    public void doWork() {}\n" + "}\n";
        JautodocConfiguration cfg = defaults();
        cfg.setGetterSetterOnly(true);

        String result = new StandaloneJautodocEngine(cfg).processSource(source);

        assertTrue(result.contains("Gets the foo"), "Getter must be commented with getterSetterOnly=true");
        assertFalse(result.contains("Do work"), "Regular method must not be commented with getterSetterOnly=true");
    }

    @Test
    void excludeGetterSetter_skipsGettersAndSetters() {
        String source = "package p;\npublic class C {\n" + "    public String getFoo() { return null; }\n"
                + "    public void doWork() {}\n" + "}\n";
        JautodocConfiguration cfg = defaults();
        cfg.setExcludeGetterSetter(true);

        String result = new StandaloneJautodocEngine(cfg).processSource(source);

        assertFalse(result.contains("Gets the foo"), "Getter must not be commented when excludeGetterSetter=true");
        assertTrue(result.contains("Do work"), "Regular method must be commented when excludeGetterSetter=true");
    }

    // =========================================================================
    // createDummyComment=false
    // =========================================================================

    @Test
    void createDummyComment_false_skipsDescriptionForMembersWithNoTags() {
        // A method with no params, void return, no throws → would produce empty Javadoc → should be skipped
        String source = "package p;\npublic class C {\n    public void doWork() {}\n}\n";
        JautodocConfiguration cfg = defaults();
        cfg.setCreateDummyComment(false);

        String result = new StandaloneJautodocEngine(cfg).processSource(source);

        // With createDummyComment=false and no tags, no Javadoc should be written for doWork()
        assertFalse(result.contains("/**"),
                "No Javadoc should be written for tagless method when createDummyComment=false");
    }

    // =========================================================================
    // addTodoForAutodoc
    // =========================================================================

    @Test
    void addTodoForAutodoc_prefixesTodo() {
        String source = "package p;\npublic class D {\n    public int getValue() { return 0; }\n}\n";
        JautodocConfiguration cfg = defaults();
        cfg.setAddTodoForAutodoc(true);

        String result = new StandaloneJautodocEngine(cfg).processSource(source);

        assertTrue(result.contains("TODO"), "Comment must contain TODO when addTodoForAutodoc=true");
    }

    // =========================================================================
    // COMPLETE mode – adds missing tags
    // =========================================================================

    @Test
    void completeMode_addsMissingParamToExistingJavadoc() {
        // Existing Javadoc has no @param
        String source = "package p;\npublic class E {\n" + "    /**\n" + "     * Does work.\n" + "     */\n"
                + "    public void doWork(String task) {}\n" + "}\n";
        JautodocConfiguration cfg = defaults();
        cfg.setMode(JautodocMode.COMPLETE);

        String result = new StandaloneJautodocEngine(cfg).processSource(source);

        assertTrue(result.contains("@param task"), "COMPLETE mode must add missing @param to existing Javadoc");
    }

    // =========================================================================
    // Result counters
    // =========================================================================

    @Test
    void resultCounters_successAndMissing() throws IOException {
        Path existing = tempDir.resolve("Exists.java");
        Files.writeString(existing, "package p;\npublic class Exists {}\n", StandardCharsets.UTF_8);

        Path missing = tempDir.resolve("Missing.java");
        // intentionally not created

        JautodocResult result = new StandaloneJautodocEngine(defaults()).process(java.util.List.of(existing, missing));

        assertEquals(1, result.getSuccessCount(), "One file should succeed");
        assertEquals(1, result.getFailCount(), "One file should fail (missing)");
        assertEquals(0, result.getSkippedCount());
        assertEquals(0, result.getReadOnlyCount());
    }

    // =========================================================================
    // JautodocMode.fromString
    // =========================================================================

    @Test
    void modeFromString_nullDefaultsToComplete() {
        assertEquals(JautodocMode.COMPLETE, JautodocMode.fromString(null, true));
    }

    @Test
    void modeFromString_caseInsensitive() {
        assertEquals(JautodocMode.KEEP, JautodocMode.fromString("KEEP", false));
        assertEquals(JautodocMode.REPLACE, JautodocMode.fromString("Replace", false));
        assertEquals(JautodocMode.COMPLETE, JautodocMode.fromString("complete", false));
    }

    @Test
    void modeFromString_unknownFallsBackToComplete() {
        assertEquals(JautodocMode.COMPLETE, JautodocMode.fromString("bogus", false));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Loads a test fixture from {@code src/test/resources/fixtures/<path>}. */
    private static String fixture(String relativePath) throws IOException {
        String resourcePath = "/fixtures/" + relativePath;
        try (InputStream is = StandaloneJautodocEngineTest.class.getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Fixture not found: " + resourcePath);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Normalises line endings and trims trailing whitespace from each line so that golden-file comparisons are not
     * affected by OS-specific newline differences or editor trailing-whitespace settings.
     */
    private static String normalise(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n")
                // Trim trailing spaces on each line
                .lines().map(String::stripTrailing).collect(java.util.stream.Collectors.joining("\n"))
                // Strip leading/trailing blank lines from the whole file
                .strip();
    }
}
