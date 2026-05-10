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
import com.hazendaz.maven.jautodoc.core.JautodocMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
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

    private final JautodocConfiguration config;
    private final CommentTextGenerator generator;

    /**
     * Instantiates a new java source processor.
     *
     * @param config
     *            the config
     */
    public JavaSourceProcessor(JautodocConfiguration config) {
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
    public String process(String source) {
        if (config.isHeaderOnly()) {
            return source; // header-only mode: skip all Javadoc changes
        }

        @SuppressWarnings("deprecation")
        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        Map<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_SOURCE, "21");
        options.put(JavaCore.COMPILER_COMPLIANCE, "21");
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "21");
        parser.setCompilerOptions(options);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        // Pre-build field-name → existing-Javadoc-text map for getterSetterFromField feature
        Map<String, String> fieldJavadocMap = buildFieldJavadocMap(cu, source);

        List<JavadocEdit> edits = new ArrayList<>();
        cu.accept(new JavadocVisitor(source, config, generator, fieldJavadocMap, edits));

        if (edits.isEmpty()) {
            return source;
        }

        // Apply in descending offset order to preserve positions
        edits.sort(Comparator.comparingInt((JavadocEdit e) -> e.offset).reversed());

        StringBuilder sb = new StringBuilder(source);
        for (JavadocEdit edit : edits) {
            sb.replace(edit.offset, edit.offset + edit.length, edit.text);
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Pre-pass: field javadoc map
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, String> buildFieldJavadocMap(CompilationUnit cu, String source) {
        Map<String, String> map = new HashMap<>();
        if (!config.isGetterSetterFromField()) {
            return map;
        }
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(FieldDeclaration node) {
                Javadoc jdoc = node.getJavadoc();
                if (jdoc != null) {
                    String text = source.substring(jdoc.getStartPosition(), jdoc.getStartPosition() + jdoc.getLength());
                    for (Object obj : node.fragments()) {
                        VariableDeclarationFragment frag = (VariableDeclarationFragment) obj;
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

    private static final class JavadocVisitor extends ASTVisitor {

        private final String source;
        private final JautodocConfiguration config;
        private final CommentTextGenerator generator;
        private final Map<String, String> fieldJavadocMap;
        private final List<JavadocEdit> edits;

        JavadocVisitor(String source, JautodocConfiguration config, CommentTextGenerator generator,
                Map<String, String> fieldJavadocMap, List<JavadocEdit> edits) {
            this.source = source;
            this.config = config;
            this.generator = generator;
            this.fieldJavadocMap = fieldJavadocMap;
            this.edits = edits;
        }

        // ---- Type declarations ----

        @Override
        public boolean visit(TypeDeclaration node) {
            if (config.isCommentTypes() && shouldCommentByVisibility(node.getModifiers())) {
                String name = node.getName().getIdentifier();
                String desc = generator.generateTypeComment(name, node.isInterface(), false, false);
                addJavadocEdit(node, desc, List.of());
            }
            return true; // always recurse into body
        }

        @Override
        public boolean visit(EnumDeclaration node) {
            if (config.isCommentTypes() && shouldCommentByVisibility(node.getModifiers())) {
                String name = node.getName().getIdentifier();
                String desc = generator.generateTypeComment(name, false, true, false);
                addJavadocEdit(node, desc, List.of());
            }
            return true;
        }

        @Override
        public boolean visit(AnnotationTypeDeclaration node) {
            if (config.isCommentTypes() && shouldCommentByVisibility(node.getModifiers())) {
                String name = node.getName().getIdentifier();
                String desc = generator.generateTypeComment(name, false, false, true);
                addJavadocEdit(node, desc, List.of());
            }
            return true;
        }

        // ---- Field declarations ----

        @SuppressWarnings("unchecked")
        @Override
        public boolean visit(FieldDeclaration node) {
            if (!config.isCommentFields() || !shouldCommentByVisibility(node.getModifiers())) {
                return false;
            }
            if (node.fragments().isEmpty()) {
                return false;
            }
            VariableDeclarationFragment first = (VariableDeclarationFragment) node.fragments().get(0);
            String fieldName = first.getName().getIdentifier();
            String desc = generator.generateFieldComment(fieldName);
            if (config.isAddTodoForAutodoc()) {
                desc = "TODO " + desc;
            }
            addJavadocEdit(node, desc, List.of());
            return false;
        }

        // ---- Method / constructor declarations ----

        @SuppressWarnings("unchecked")
        @Override
        public boolean visit(MethodDeclaration node) {
            if (!config.isCommentMethods() || !shouldCommentByVisibility(node.getModifiers())) {
                return false;
            }

            String name = node.getName().getIdentifier();
            int paramCount = node.parameters().size();
            boolean isGetter = generator.isGetter(name, paramCount);
            boolean isSetter = generator.isSetter(name, paramCount);

            // Apply getter/setter filters
            if (config.isGetterSetterOnly() && !isGetter && !isSetter && !node.isConstructor()) {
                return false;
            }
            if (config.isExcludeGetterSetter() && (isGetter || isSetter)) {
                return false;
            }

            // Build description
            String desc = buildMethodDescription(node, name, isGetter, isSetter);
            if (config.isAddTodoForAutodoc()) {
                desc = "TODO " + desc;
            }

            // Build tag lines
            List<String> tags = buildMethodTags(node, isGetter);

            addJavadocEdit(node, desc, tags);
            return false;
        }

        // -------------------------------------------------------------------------
        // Description builders
        // -------------------------------------------------------------------------

        private String buildMethodDescription(MethodDeclaration node, String name, boolean isGetter, boolean isSetter) {
            if (node.isConstructor()) {
                ASTNode parent = node.getParent();
                String className = (parent instanceof TypeDeclaration)
                        ? ((TypeDeclaration) parent).getName().getIdentifier()
                        : name;
                return generator.generateConstructorComment(className);
            }
            if (isGetter) {
                return buildGetterDesc(name);
            }
            if (isSetter) {
                return buildSetterDesc(name);
            }
            return generator.generateMethodComment(name);
        }

        private String buildGetterDesc(String methodName) {
            if (config.isGetterSetterFromField()) {
                String fieldName = generator.getFieldFromGetter(methodName);
                String fieldDoc = fieldName != null ? fieldJavadocMap.get(fieldName) : null;
                if (fieldDoc != null) {
                    String raw = extractMainDescFromJavadocText(fieldDoc, config.isGetterSetterFromFieldFirst());
                    return "Gets the " + lcFirst(raw);
                }
            }
            return generator.generateGetterComment(methodName);
        }

        private String buildSetterDesc(String methodName) {
            if (config.isGetterSetterFromField()) {
                String fieldName = generator.getFieldFromSetter(methodName);
                String fieldDoc = fieldName != null ? fieldJavadocMap.get(fieldName) : null;
                if (fieldDoc != null) {
                    String raw = extractMainDescFromJavadocText(fieldDoc, config.isGetterSetterFromFieldFirst());
                    return "Sets the " + lcFirst(raw);
                }
            }
            return generator.generateSetterComment(methodName);
        }

        /** Returns {@code s} with the first character lower-cased. */
        private static String lcFirst(String s) {
            if (s == null || s.isEmpty()) {
                return s;
            }
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }

        /**
         * Extracts the main (non-tag) description from a raw Javadoc comment string. Strips the surrounding delimiters
         * and leading {@code *} characters from each line.
         */
        private static String extractMainDescFromJavadocText(String javadocText, boolean firstSentenceOnly) {
            // Strip /** ... */ delimiters
            String stripped = javadocText.replaceAll("^/\\*+", "").replaceAll("\\*/$", "").trim();
            String[] lines = stripped.split("\r?\n");
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                String trimmed = line.trim();
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
            String result = sb.length() > 0 ? sb.toString() : "field";
            if (firstSentenceOnly) {
                int dot = result.indexOf('.');
                if (dot >= 0) {
                    result = result.substring(0, dot + 1);
                }
            }
            return result;
        }

        // -------------------------------------------------------------------------
        // Tag builders
        // -------------------------------------------------------------------------

        @SuppressWarnings("unchecked")
        private List<String> buildMethodTags(MethodDeclaration node, boolean isGetter) {
            List<String> tags = new ArrayList<>();

            // @param
            for (Object obj : node.parameters()) {
                SingleVariableDeclaration param = (SingleVariableDeclaration) obj;
                String pName = param.getName().getIdentifier();
                tags.add("@param " + pName + " " + generator.generateParamComment(pName));
            }

            // @return (non-void, non-constructor)
            if (!node.isConstructor() && node.getReturnType2() != null) {
                String retType = node.getReturnType2().toString();
                if (!"void".equals(retType)) {
                    String returnDesc;
                    if (isGetter) {
                        // Boolean getters use "true, if successful" unconditionally
                        if ("boolean".equals(retType) || "Boolean".equals(retType)) {
                            returnDesc = generator.generateReturnComment(retType);
                        } else {
                            // For other getters derive @return text from field name
                            String methodName = node.getName().getIdentifier();
                            String fieldName = generator.getFieldFromGetter(methodName);
                            returnDesc = fieldName != null ? generator.generateParamComment(fieldName)
                                    : generator.generateReturnComment(retType);
                        }
                    } else {
                        returnDesc = generator.generateReturnComment(retType);
                    }
                    tags.add("@return " + returnDesc);
                }
            }

            // @throws
            for (Object obj : node.thrownExceptionTypes()) {
                Type exType = (Type) obj;
                String exName = exType.toString();
                tags.add("@throws " + exName + " " + generator.generateThrowsComment(exName));
            }

            return tags;
        }

        // -------------------------------------------------------------------------
        // Edit builders
        // -------------------------------------------------------------------------

        private void addJavadocEdit(BodyDeclaration node, String description, List<String> tagLines) {
            // Optionally suppress description
            String desc = config.isCreateDummyComment() ? description : "";

            // If nothing to write, skip
            if (desc.isEmpty() && tagLines.isEmpty()) {
                return;
            }

            boolean isField = node instanceof FieldDeclaration;

            Javadoc existing = node.getJavadoc();
            JautodocMode mode = config.getMode();

            if (existing != null) {
                switch (mode) {
                    case KEEP:
                        return; // leave as-is
                    case REPLACE:
                        if (!config.isGetterSetterFromField() || config.isGetterSetterFromFieldReplace()
                                || (!isGetterOrSetter(node))) {
                            int off = existing.getStartPosition();
                            int len = existing.getLength();
                            String indent = computeIndent(existing.getStartPosition());
                            edits.add(new JavadocEdit(off, len, buildJavadocText(desc, tagLines, indent, isField)));
                        }
                        return;
                    case COMPLETE:
                        completeMissingTags(node, existing, tagLines);
                        return;
                    default:
                        return;
                }
            }

            // No existing Javadoc → insert new one
            String indent = computeIndent(node.getStartPosition());
            String javadocText = buildJavadocText(desc, tagLines, indent, isField);
            int insertOffset = lineStartOffset(node.getStartPosition());
            edits.add(new JavadocEdit(insertOffset, 0, javadocText + "\n"));
        }

        /** Returns true when the body declaration is a getter or setter method. */
        private boolean isGetterOrSetter(BodyDeclaration node) {
            if (!(node instanceof MethodDeclaration)) {
                return false;
            }
            MethodDeclaration md = (MethodDeclaration) node;
            String name = md.getName().getIdentifier();
            int params = md.parameters().size();
            return generator.isGetter(name, params) || generator.isSetter(name, params);
        }

        // ---- COMPLETE-mode tag completion ----

        @SuppressWarnings("unchecked")
        private void completeMissingTags(BodyDeclaration node, Javadoc existing, List<String> requiredTags) {
            if (requiredTags.isEmpty() || !(node instanceof MethodDeclaration)) {
                return;
            }

            // Collect already-present tags
            Set<String> presentParams = new HashSet<>();
            Set<String> presentThrows = new HashSet<>();
            boolean hasReturn = false;

            for (Object obj : existing.tags()) {
                TagElement tag = (TagElement) obj;
                String tagName = tag.getTagName();
                if (tagName == null) {
                    continue;
                }
                if ("@param".equals(tagName) && !tag.fragments().isEmpty()) {
                    Object first = tag.fragments().get(0);
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
            List<String> missing = new ArrayList<>();
            for (String tagLine : requiredTags) {
                if (tagLine.startsWith("@param ")) {
                    String pName = tagLine.substring(7, tagLine.indexOf(' ', 7));
                    if (!presentParams.contains(pName)) {
                        missing.add(tagLine);
                    }
                } else if (tagLine.startsWith("@throws ")) {
                    String exName = tagLine.substring(8, tagLine.indexOf(' ', 8));
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
            String indent = computeIndent(existing.getStartPosition());
            int closePos = existing.getStartPosition() + existing.getLength() - 2; // points at '*' of '*/'
            StringBuilder sb = new StringBuilder();
            for (String line : missing) {
                sb.append('\n').append(indent).append(" * ").append(line);
            }
            sb.append('\n').append(indent).append(' ');
            edits.add(new JavadocEdit(closePos, 0, sb.toString()));
        }

        // -------------------------------------------------------------------------
        // Formatting helpers
        // -------------------------------------------------------------------------

        private String buildJavadocText(String description, List<String> tagLines, String indent, boolean isField) {
            boolean hasDesc = !description.isEmpty();
            // Single-line format is only used for fields (not types, methods, constructors)
            if (isField && config.isSingleLineComment() && tagLines.isEmpty() && hasDesc) {
                return indent + "/** " + description + " */";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(indent).append("/**\n");
            if (hasDesc) {
                sb.append(indent).append(" * ").append(description).append('\n');
            }
            if (!tagLines.isEmpty()) {
                if (hasDesc) {
                    sb.append(indent).append(" *\n");
                }
                for (String tag : tagLines) {
                    sb.append(indent).append(" * ").append(tag).append('\n');
                }
            }
            sb.append(indent).append(" */");
            return sb.toString();
        }

        /**
         * Computes the whitespace-only indentation for the line that contains {@code sourceOffset}.
         */
        private String computeIndent(int sourceOffset) {
            int lineBegin = lineStartOffset(sourceOffset);
            StringBuilder indent = new StringBuilder();
            for (int i = lineBegin; i < sourceOffset && i < source.length()
                    && Character.isWhitespace(source.charAt(i)); i++) {
                indent.append(source.charAt(i));
            }
            return indent.toString();
        }

        /**
         * Returns the offset of the first character on the line that contains {@code pos}.
         */
        private int lineStartOffset(int pos) {
            int p = pos - 1;
            while (p >= 0 && source.charAt(p) != '\n') {
                p--;
            }
            return p + 1; // character after the '\n', or 0 if no '\n' found
        }

        // ---- Visibility filter ----

        private boolean shouldCommentByVisibility(int modifiers) {
            if (Modifier.isPublic(modifiers)) {
                return config.isVisibilityPublic();
            }
            if (Modifier.isProtected(modifiers)) {
                return config.isVisibilityProtected();
            }
            if (Modifier.isPrivate(modifiers)) {
                return config.isVisibilityPrivate();
            }
            return config.isVisibilityPackage(); // package-private
        }
    }
}
