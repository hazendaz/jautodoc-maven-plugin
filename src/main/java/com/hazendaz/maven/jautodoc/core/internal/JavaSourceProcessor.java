/*
 * SPDX-License-Identifier: EPL-2.0
 * See LICENSE file for details.
 *
 * Copyright 2018-2026 hazendaz
 */
package com.hazendaz.maven.jautodoc.core.internal;

import com.hazendaz.maven.jautodoc.core.JautodocConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Parses a Java source string with the Eclipse JDT AST (no workspace required), then inserts or replaces Javadoc
 * comments according to the supplied {@link JautodocConfiguration}.
 * <p>
 * Processing strategy:
 * <ol>
 * <li>Parse the source with {@link ASTParser} ({@code K_COMPILATION_UNIT}, no bindings needed).
 * <li>Walk the AST and collect {@link JavadocEdit} objects.
 * <li>Sort edits by descending offset and apply them to the source {@link StringBuilder} so that earlier offsets are
 * not disturbed.
 * </ol>
 */
public final class JavaSourceProcessor {

    /** The config. */
    private final JautodocConfiguration config;
    /** The generator. */
    private final CommentTextGenerator generator;

    /**
     * Instantiates a new java source processor.
     *
     * @param config
     *            the config
     */
    public JavaSourceProcessor(final JautodocConfiguration config) {
        this.config = config;
        this.generator = new CommentTextGenerator();
    }

    /**
     * Processes the given Java source string and returns the (possibly modified) result.
     *
     * @param source
     *            the source
     *
     * @return the string
     */
    public String process(final String source) {
        if (this.config.isHeaderOnly()) {
            return source; // header-only mode: skip all Javadoc changes
        }

        final var parser = ASTParser.newParser(AST.JLS21);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final Map<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_SOURCE, "21");
        options.put(JavaCore.COMPILER_COMPLIANCE, "21");
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "21");
        parser.setCompilerOptions(options);

        final var cu = (CompilationUnit) parser.createAST(null);

        // Pre-build field-name → existing-Javadoc-text map for getterSetterFromField feature
        final var fieldJavadocMap = this.buildFieldJavadocMap(cu, source);

        final List<JavadocEdit> edits = new ArrayList<>();
        cu.accept(new JavadocVisitor(source, this.config, this.generator, fieldJavadocMap, edits));

        if (edits.isEmpty()) {
            return source;
        }

        // Apply in descending offset order to preserve positions
        edits.sort(Comparator.comparingInt((final JavadocEdit e) -> e.offset).reversed());

        final var sb = new StringBuilder(source);
        for (final JavadocEdit edit : edits) {
            sb.replace(edit.offset, edit.offset + edit.length, edit.text);
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Pre-pass: field javadoc map
    // -------------------------------------------------------------------------

    /**
     * Build field javadoc map.
     *
     * @param cu
     *            the cu
     * @param source
     *            the source
     *
     * @return the map
     */
    private Map<String, String> buildFieldJavadocMap(final CompilationUnit cu, final String source) {
        final Map<String, String> map = new HashMap<>();
        if (!this.config.isGetterSetterFromField()) {
            return map;
        }
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(final FieldDeclaration node) {
                final var jdoc = node.getJavadoc();
                if (jdoc != null) {
                    final var text = source.substring(jdoc.getStartPosition(),
                            jdoc.getStartPosition() + jdoc.getLength());
                    for (final Object obj : node.fragments()) {
                        final var frag = (VariableDeclarationFragment) obj;
                        map.put(frag.getName().getIdentifier(), text);
                    }
                }
                return true;
            }
        });
        return map;
    }

    // =========================================================================
    // Inner visitor
    // =========================================================================

    /**
     * The Class JavadocVisitor.
     */
    private static final class JavadocVisitor extends ASTVisitor {

        /** The source. */
        private final String source;
        /** The config. */
        private final JautodocConfiguration config;
        /** The generator. */
        private final CommentTextGenerator generator;
        /** The field javadoc map. */
        private final Map<String, String> fieldJavadocMap;
        /** The edits. */
        private final List<JavadocEdit> edits;

        /**
         * Instantiates a new javadoc visitor.
         *
         * @param source
         *            the source
         * @param config
         *            the config
         * @param generator
         *            the generator
         * @param fieldJavadocMap
         *            the field javadoc map
         * @param edits
         *            the edits
         */
        JavadocVisitor(final String source, final JautodocConfiguration config, final CommentTextGenerator generator,
                final Map<String, String> fieldJavadocMap, final List<JavadocEdit> edits) {
            this.source = source;
            this.config = config;
            this.generator = generator;
            this.fieldJavadocMap = fieldJavadocMap;
            this.edits = edits;
        }

        // ---- Type declarations ----

        @Override
        public boolean visit(final TypeDeclaration node) {
            if (this.config.isCommentTypes() && this.shouldCommentByVisibility(node.getModifiers())) {
                final var name = node.getName().getIdentifier();
                final var desc = this.generator.generateTypeComment(name, node.isInterface(), false, false);
                this.addJavadocEdit(node, desc, List.of());
            }
            return true; // always recurse into body
        }

        @Override
        public boolean visit(final EnumDeclaration node) {
            if (this.config.isCommentTypes() && this.shouldCommentByVisibility(node.getModifiers())) {
                final var name = node.getName().getIdentifier();
                final var desc = this.generator.generateTypeComment(name, false, true, false);
                this.addJavadocEdit(node, desc, List.of());
            }
            return true;
        }

        @Override
        public boolean visit(final AnnotationTypeDeclaration node) {
            if (this.config.isCommentTypes() && this.shouldCommentByVisibility(node.getModifiers())) {
                final var name = node.getName().getIdentifier();
                final var desc = this.generator.generateTypeComment(name, false, false, true);
                this.addJavadocEdit(node, desc, List.of());
            }
            return true;
        }

        // ---- Field declarations ----

        @Override
        public boolean visit(final FieldDeclaration node) {
            if (!this.config.isCommentFields() || !this.shouldCommentByVisibility(node.getModifiers())
                    || node.fragments().isEmpty()) {
                return false;
            }
            final var first = (VariableDeclarationFragment) node.fragments().get(0);
            final var fieldName = first.getName().getIdentifier();
            var desc = this.generator.generateFieldComment(fieldName);
            if (this.config.isAddTodoForAutodoc()) {
                desc = "TODO " + desc;
            }
            this.addJavadocEdit(node, desc, List.of());
            return false;
        }

        // ---- Method / constructor declarations ----

        @Override
        public boolean visit(final MethodDeclaration node) {
            // Skip methods that override/implement a parent or interface method
            if (!this.config.isCommentMethods() || !this.shouldCommentByVisibility(node.getModifiers())
                    || this.config.isExcludeOverrides() && JavadocVisitor.hasOverrideAnnotation(node)) {
                return false;
            }

            final var name = node.getName().getIdentifier();
            final var paramCount = node.parameters().size();
            final var isGetter = this.generator.isGetter(name, paramCount);
            final var isSetter = this.generator.isSetter(name, paramCount);

            // Apply getter/setter filters
            if (this.config.isGetterSetterOnly() && !isGetter && !isSetter && !node.isConstructor()) {
                return false;
            }
            if (this.config.isExcludeGetterSetter() && (isGetter || isSetter)) {
                return false;
            }

            // Build description
            var desc = this.buildMethodDescription(node, name, isGetter, isSetter);
            if (this.config.isAddTodoForAutodoc()) {
                desc = "TODO " + desc;
            }

            // Build tag lines
            final var tags = this.buildMethodTags(node, isGetter);

            this.addJavadocEdit(node, desc, tags);
            return false;
        }

        // -------------------------------------------------------------------------
        // Description builders
        // -------------------------------------------------------------------------

        /**
         * Build method description.
         *
         * @param node
         *            the node
         * @param name
         *            the name
         * @param isGetter
         *            the is getter
         * @param isSetter
         *            the is setter
         *
         * @return the string
         */
        private String buildMethodDescription(final MethodDeclaration node, final String name, final boolean isGetter,
                final boolean isSetter) {
            if (node.isConstructor()) {
                final var parent = node.getParent();
                final var className = parent instanceof TypeDeclaration
                        ? ((TypeDeclaration) parent).getName().getIdentifier()
                        : name;
                return this.generator.generateConstructorComment(className);
            }
            if (isGetter) {
                return this.buildGetterDesc(name);
            }
            if (isSetter) {
                return this.buildSetterDesc(name);
            }
            return this.generator.generateMethodComment(name);
        }

        /**
         * Build getter desc.
         *
         * @param methodName
         *            the method name
         *
         * @return the string
         */
        private String buildGetterDesc(final String methodName) {
            if (this.config.isGetterSetterFromField()) {
                final var fieldName = this.generator.getFieldFromGetter(methodName);
                final var fieldDoc = fieldName != null ? this.fieldJavadocMap.get(fieldName) : null;
                if (fieldDoc != null) {
                    final var raw = JavadocVisitor.extractMainDescFromJavadocText(fieldDoc,
                            this.config.isGetterSetterFromFieldFirst());
                    return "Gets the " + JavadocVisitor.lcFirst(raw);
                }
            }
            return this.generator.generateGetterComment(methodName);
        }

        /**
         * Build setter desc.
         *
         * @param methodName
         *            the method name
         *
         * @return the string
         */
        private String buildSetterDesc(final String methodName) {
            if (this.config.isGetterSetterFromField()) {
                final var fieldName = this.generator.getFieldFromSetter(methodName);
                final var fieldDoc = fieldName != null ? this.fieldJavadocMap.get(fieldName) : null;
                if (fieldDoc != null) {
                    final var raw = JavadocVisitor.extractMainDescFromJavadocText(fieldDoc,
                            this.config.isGetterSetterFromFieldFirst());
                    return "Sets the " + JavadocVisitor.lcFirst(raw);
                }
            }
            return this.generator.generateSetterComment(methodName);
        }

        /**
         * Returns {@code s} with the first character lower-cased.
         *
         * @param s
         *            the s
         *
         * @return the string
         */
        private static String lcFirst(final String s) {
            if (s == null || s.isEmpty()) {
                return s;
            }
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }

        /**
         * Extracts the main (non-tag) description from a raw Javadoc comment string. Strips the surrounding delimiters
         * and leading {@code *} characters from each line.
         *
         * @param javadocText
         *            the javadoc text
         * @param firstSentenceOnly
         *            the first sentence only
         *
         * @return the string
         */
        private static String extractMainDescFromJavadocText(final String javadocText,
                final boolean firstSentenceOnly) {
            // Strip /** ... */ delimiters
            final var stripped = javadocText.replaceAll("^/\\*+", "").replaceAll("\\*/$", "").trim();
            final var lines = stripped.split("\r?\n");
            final var sb = new StringBuilder();
            for (final String line : lines) {
                var trimmed = line.trim();
                if (trimmed.startsWith("*")) {
                    trimmed = trimmed.substring(1).trim();
                }
                if (trimmed.startsWith("@")) {
                    break; // stop before tags
                }
                if (!trimmed.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(trimmed);
                }
            }
            var result = sb.length() > 0 ? sb.toString() : "field";
            if (firstSentenceOnly) {
                final var dot = result.indexOf('.');
                if (dot >= 0) {
                    result = result.substring(0, dot + 1);
                }
            }
            return result;
        }

        // -------------------------------------------------------------------------
        // Tag builders
        // -------------------------------------------------------------------------

        /**
         * Build method tags.
         *
         * @param node
         *            the node
         * @param isGetter
         *            the is getter
         *
         * @return the list
         */
        private List<String> buildMethodTags(final MethodDeclaration node, final boolean isGetter) {
            final List<String> tags = new ArrayList<>();

            // @param
            for (final Object obj : node.parameters()) {
                final var param = (SingleVariableDeclaration) obj;
                final var pName = param.getName().getIdentifier();
                tags.add("@param " + pName + " " + this.generator.generateParamComment(pName));
            }

            // @return (non-void, non-constructor)
            if (!node.isConstructor() && node.getReturnType2() != null) {
                final var retType = node.getReturnType2().toString();
                if (!"void".equals(retType)) {
                    String returnDesc;
                    // Boolean getters use "true, if successful" unconditionally
                    if (!isGetter || "boolean".equals(retType) || "Boolean".equals(retType)) {
                        returnDesc = this.generator.generateReturnComment(retType);
                    } else {
                        // For other getters derive @return text from field name
                        final var methodName = node.getName().getIdentifier();
                        final var fieldName = this.generator.getFieldFromGetter(methodName);
                        returnDesc = fieldName != null ? this.generator.generateParamComment(fieldName)
                                : this.generator.generateReturnComment(retType);
                    }
                    tags.add("@return " + returnDesc);
                }
            }

            // @throws
            for (final Object obj : node.thrownExceptionTypes()) {
                final var exType = (Type) obj;
                final var exName = exType.toString();
                tags.add("@throws " + exName + " " + this.generator.generateThrowsComment(exName));
            }

            return tags;
        }

        // -------------------------------------------------------------------------
        // Edit builders
        // -------------------------------------------------------------------------

        /**
         * Add javadoc edit.
         *
         * @param node
         *            the node
         * @param description
         *            the description
         * @param tagLines
         *            the tag lines
         */
        private void addJavadocEdit(final BodyDeclaration node, final String description, final List<String> tagLines) {
            // Optionally suppress description
            final var desc = this.config.isCreateDummyComment() ? description : "";

            // If nothing to write, skip
            if (desc.isEmpty() && tagLines.isEmpty()) {
                return;
            }

            final var isField = node instanceof FieldDeclaration;

            final var existing = node.getJavadoc();
            final var mode = this.config.getMode();

            if (existing != null) {
                switch (mode) {
                    case KEEP:
                        return; // leave as-is
                    case REPLACE:
                        if (!this.config.isGetterSetterFromField() || this.config.isGetterSetterFromFieldReplace()
                                || !this.isGetterOrSetter(node)) {
                            final var off = existing.getStartPosition();
                            final var len = existing.getLength();
                            final var indent = this.computeIndent(existing.getStartPosition());
                            this.edits.add(
                                    new JavadocEdit(off, len, this.buildJavadocText(desc, tagLines, indent, isField)));
                        }
                        return;
                    case COMPLETE:
                        this.completeMissingTags(node, existing, tagLines);
                        return;
                    default:
                        return;
                }
            }

            // No existing Javadoc → insert new one
            final var indent = this.computeIndent(node.getStartPosition());
            final var javadocText = this.buildJavadocText(desc, tagLines, indent, isField);
            final var insertOffset = this.lineStartOffset(node.getStartPosition());
            this.edits.add(new JavadocEdit(insertOffset, 0, javadocText + "\n"));
        }

        /**
         * Returns true when the body declaration is a getter or setter method.
         *
         * @param node
         *            the node
         *
         * @return true, if successful
         */
        private boolean isGetterOrSetter(final BodyDeclaration node) {
            if (!(node instanceof MethodDeclaration)) {
                return false;
            }
            final var md = (MethodDeclaration) node;
            final var name = md.getName().getIdentifier();
            final var params = md.parameters().size();
            return this.generator.isGetter(name, params) || this.generator.isSetter(name, params);
        }

        /**
         * Returns true when the method declaration has an {@code @Override} annotation.
         *
         * @param node
         *            the node
         *
         * @return true, if successful
         */
        private static boolean hasOverrideAnnotation(final MethodDeclaration node) {
            for (final Object mod : node.modifiers()) {
                if (mod instanceof MarkerAnnotation) {
                    final var name = ((MarkerAnnotation) mod).getTypeName().getFullyQualifiedName();
                    if ("Override".equals(name) || "java.lang.Override".equals(name)) {
                        return true;
                    }
                }
            }
            return false;
        }

        // ---- COMPLETE-mode tag completion ----

        /**
         * Complete missing tags.
         *
         * @param node
         *            the node
         * @param existing
         *            the existing
         * @param requiredTags
         *            the required tags
         */
        private void completeMissingTags(final BodyDeclaration node, final Javadoc existing,
                final List<String> requiredTags) {
            if (requiredTags.isEmpty() || !(node instanceof MethodDeclaration)) {
                return;
            }

            // Collect already-present tags
            final Set<String> presentParams = new HashSet<>();
            final Set<String> presentThrows = new HashSet<>();
            var hasReturn = false;

            for (final Object obj : existing.tags()) {
                final var tag = (TagElement) obj;
                final var tagName = tag.getTagName();
                if (tagName == null) {
                    continue;
                }
                if ("@param".equals(tagName) && !tag.fragments().isEmpty()) {
                    final var first = tag.fragments().get(0);
                    if (first instanceof SimpleName) {
                        presentParams.add(((SimpleName) first).getIdentifier());
                    }
                } else if ("@throws".equals(tagName) || "@exception".equals(tagName)) {
                    if (!tag.fragments().isEmpty()) {
                        presentThrows.add(tag.fragments().get(0).toString().trim());
                    }
                } else if ("@return".equals(tagName)) {
                    hasReturn = true;
                }
            }

            // Determine which required tags are missing
            final List<String> missing = new ArrayList<>();
            for (final String tagLine : requiredTags) {
                if (tagLine.startsWith("@param ")) {
                    final var spaceAt = tagLine.indexOf(' ', 7);
                    final var pName = spaceAt >= 0 ? tagLine.substring(7, spaceAt) : tagLine.substring(7);
                    if (!presentParams.contains(pName)) {
                        missing.add(tagLine);
                    }
                } else if (tagLine.startsWith("@throws ")) {
                    final var spaceAt = tagLine.indexOf(' ', 8);
                    final var exName = spaceAt >= 0 ? tagLine.substring(8, spaceAt) : tagLine.substring(8);
                    if (!presentThrows.contains(exName)) {
                        missing.add(tagLine);
                    }
                } else if (tagLine.startsWith("@return ") && !hasReturn) {
                    missing.add(tagLine);
                }
            }

            if (missing.isEmpty()) {
                return;
            }

            // Insert missing tags just before the closing */
            final var indent = this.computeIndent(existing.getStartPosition());
            final var closePos = existing.getStartPosition() + existing.getLength() - 2; // points at '*' of '*/'
            final var sb = new StringBuilder();
            for (final String line : missing) {
                sb.append('\n').append(indent).append(" * ").append(line);
            }
            sb.append('\n').append(indent).append(' ');
            this.edits.add(new JavadocEdit(closePos, 0, sb.toString()));
        }

        // -------------------------------------------------------------------------
        // Formatting helpers
        // -------------------------------------------------------------------------

        /**
         * Build javadoc text.
         *
         * @param description
         *            the description
         * @param tagLines
         *            the tag lines
         * @param indent
         *            the indent
         * @param isField
         *            the is field
         *
         * @return the string
         */
        private String buildJavadocText(final String description, final List<String> tagLines, final String indent,
                final boolean isField) {
            final var hasDesc = !description.isEmpty();
            // Single-line format is only used for fields (not types, methods, constructors)
            if (isField && this.config.isSingleLineComment() && tagLines.isEmpty() && hasDesc) {
                return indent + "/** " + description + " */";
            }

            final var sb = new StringBuilder();
            sb.append(indent).append("/**\n");
            if (hasDesc) {
                sb.append(indent).append(" * ").append(description).append('\n');
            }
            if (!tagLines.isEmpty()) {
                if (hasDesc) {
                    sb.append(indent).append(" *\n");
                }
                for (final String tag : tagLines) {
                    sb.append(indent).append(" * ").append(tag).append('\n');
                }
            }
            sb.append(indent).append(" */");
            return sb.toString();
        }

        /**
         * Computes the whitespace-only indentation for the line that contains {@code sourceOffset}.
         *
         * @param sourceOffset
         *            the source offset
         *
         * @return the string
         */
        private String computeIndent(final int sourceOffset) {
            final var lineBegin = this.lineStartOffset(sourceOffset);
            final var indent = new StringBuilder();
            for (var i = lineBegin; i < sourceOffset && i < this.source.length()
                    && Character.isWhitespace(this.source.charAt(i)); i++) {
                indent.append(this.source.charAt(i));
            }
            return indent.toString();
        }

        /**
         * Returns the offset of the first character on the line that contains {@code pos}.
         *
         * @param pos
         *            the pos
         *
         * @return the int
         */
        private int lineStartOffset(final int pos) {
            var p = pos - 1;
            while (p >= 0 && this.source.charAt(p) != '\n') {
                p--;
            }
            return p + 1; // character after the '\n', or 0 if no '\n' found
        }

        // ---- Visibility filter ----

        /**
         * Should comment by visibility.
         *
         * @param modifiers
         *            the modifiers
         *
         * @return true, if successful
         */
        private boolean shouldCommentByVisibility(final int modifiers) {
            if (Modifier.isPublic(modifiers)) {
                return this.config.isVisibilityPublic();
            }
            if (Modifier.isProtected(modifiers)) {
                return this.config.isVisibilityProtected();
            }
            if (Modifier.isPrivate(modifiers)) {
                return this.config.isVisibilityPrivate();
            }
            return this.config.isVisibilityPackage(); // package-private
        }
    }
}
