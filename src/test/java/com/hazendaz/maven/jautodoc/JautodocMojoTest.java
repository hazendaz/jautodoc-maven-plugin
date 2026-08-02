/*
 * SPDX-License-Identifier: EPL-2.0
 * See LICENSE file for details.
 *
 * Copyright 2018-2026 hazendaz
 */
package com.hazendaz.maven.jautodoc;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link JautodocMojo} using the Maven plugin testing harness.
 */
@MojoTest
class JautodocMojoTest {

    /** The temp dir. */
    @TempDir
    Path tempDir;

    /**
     * Configured mojo processes sources from pom-based configuration.
     *
     * @param mojo
     *            the mojo
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @InjectMojo(goal = "jautodoc", pom = "src/test/resources/mojo/jautodoc/pom.xml")
    void configuredMojoProcessesSources(final JautodocMojo mojo) throws Exception {
        final Path basedir = this.copyProject("jautodoc");
        JautodocMojoTest.setField(mojo, "basedir", basedir.toFile());

        final Path javaFile = basedir.resolve("src/main/java/test/Sample.java");

        mojo.execute();

        final String actual = Files.readString(javaFile, StandardCharsets.UTF_8);

        Assertions.assertTrue(actual.contains("The Class Sample"), "Class comment should be added");
        Assertions.assertFalse(actual.contains("The value."), "Field comments should remain disabled by pom");
        Assertions.assertFalse(actual.contains("Do work"), "Method comments should remain disabled by pom");
    }

    /**
     * Skip parameter leaves sources unchanged.
     *
     * @param mojo
     *            the mojo
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @InjectMojo(goal = "jautodoc")
    @MojoParameter(name = "skip", value = "true")
    void skipLeavesSourcesUntouched(final JautodocMojo mojo) throws Exception {
        final Path basedir = this.copyProject("skip");
        JautodocMojoTest.setField(mojo, "basedir", basedir.toFile());

        final Path javaFile = basedir.resolve("src/main/java/test/SkipSample.java");
        final String before = Files.readString(javaFile, StandardCharsets.UTF_8);

        mojo.execute();

        final String after = Files.readString(javaFile, StandardCharsets.UTF_8);
        Assertions.assertEquals(before, after, "Skip should leave source files unchanged");
    }

    /**
     * Copy project into temp dir.
     *
     * @param project
     *            the project
     *
     * @return the path
     *
     * @throws IOException
     *             the io exception
     */
    private Path copyProject(final String project) throws IOException {
        final Path sourceRoot = Path.of("src/test/resources/mojo", project);
        final Path targetRoot = this.tempDir.resolve(project);

        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            stream.forEach(path -> {
                final Path target = targetRoot.resolve(sourceRoot.relativize(path).toString());
                try {
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (final IllegalStateException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }

        return targetRoot;
    }

    /**
     * Sets the field.
     *
     * @param target
     *            the target
     * @param name
     *            the name
     * @param value
     *            the value
     *
     * @throws ReflectiveOperationException
     *             the reflective operation exception
     */
    private static void setField(final Object target, final String name, final Object value)
            throws ReflectiveOperationException {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
