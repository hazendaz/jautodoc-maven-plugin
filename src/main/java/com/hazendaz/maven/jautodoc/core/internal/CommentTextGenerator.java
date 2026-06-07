/*
 * SPDX-License-Identifier: EPL-2.0
 * See LICENSE file for details.
 *
 * Copyright 2018-2026 hazendaz
 */
package com.hazendaz.maven.jautodoc.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates Javadoc description strings from Java element names using Eclipse JAutodoc-compatible camelCase splitting.
 * <p>
 * All generated text matches the patterns produced by the Eclipse JAutodoc plugin so that migrating from the IDE plugin
 * to the Maven plugin produces identical output.
 */
final class CommentTextGenerator {

    // -------------------------------------------------------------------------
    // Type-level generators
    // -------------------------------------------------------------------------

    /**
     * Generate type comment.
     *
     * @param name
     *            the name
     * @param isInterface
     *            the is interface
     * @param isEnum
     *            the is enum
     * @param isAnnotation
     *            the is annotation
     *
     * @return the string
     */
    String generateTypeComment(String name, boolean isInterface, boolean isEnum, boolean isAnnotation) {
        if (isInterface) {
            return "The Interface " + name + ".";
        }
        if (isEnum) {
            return "The Enum " + name + ".";
        }
        if (isAnnotation) {
            return "The Annotation " + name + ".";
        }
        return "The Class " + name + ".";
    }

    // -------------------------------------------------------------------------
    // Field-level generators
    // -------------------------------------------------------------------------

    /**
     * Generate field comment.
     *
     * @param fieldName
     *            the field name
     *
     * @return the string
     */
    String generateFieldComment(String fieldName) {
        return "The " + splitCamelCaseLower(fieldName) + ".";
    }

    // -------------------------------------------------------------------------
    // Method/constructor-level generators
    // -------------------------------------------------------------------------

    /**
     * Generate constructor comment.
     *
     * @param className
     *            the class name
     *
     * @return the string
     */
    String generateConstructorComment(String className) {
        return "Instantiates a new " + splitCamelCaseLower(className) + ".";
    }

    /**
     * Returns true when the method name and zero-parameter count indicate a getter.
     *
     * @param methodName
     *            the method name
     * @param paramCount
     *            the param count
     *
     * @return true, if is getter
     */
    boolean isGetter(String methodName, int paramCount) {
        if (paramCount != 0) {
            return false;
        }
        return (methodName.startsWith("get") && methodName.length() > 3 && Character.isUpperCase(methodName.charAt(3)))
                || (methodName.startsWith("is") && methodName.length() > 2
                        && Character.isUpperCase(methodName.charAt(2)));
    }

    /**
     * Returns true when the method name and single-parameter count indicate a setter.
     *
     * @param methodName
     *            the method name
     * @param paramCount
     *            the param count
     *
     * @return true, if is setter
     */
    boolean isSetter(String methodName, int paramCount) {
        return paramCount == 1 && methodName.startsWith("set") && methodName.length() > 3
                && Character.isUpperCase(methodName.charAt(3));
    }

    /**
     * Extracts the field name from a getter method name ({@code getFoo} → {@code foo}, {@code isBar} → {@code bar}).
     *
     * @param methodName
     *            the method name
     *
     * @return the field from getter
     */
    String getFieldFromGetter(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        }
        return null;
    }

    /**
     * Extracts the field name from a setter method name ({@code setFoo} → {@code foo}).
     *
     * @param methodName
     *            the method name
     *
     * @return the field from setter
     */
    String getFieldFromSetter(String methodName) {
        if (methodName.startsWith("set") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        return null;
    }

    /**
     * Generate getter comment.
     *
     * @param methodName
     *            the method name
     *
     * @return the string
     */
    String generateGetterComment(String methodName) {
        if (methodName.startsWith("is")) {
            String field = getFieldFromGetter(methodName);
            String target = field != null ? splitCamelCaseLower(field) : splitCamelCaseLower(methodName);
            return "Checks if is " + target + ".";
        }
        String field = getFieldFromGetter(methodName);
        String target = field != null ? splitCamelCaseLower(field) : splitCamelCaseLower(methodName);
        return "Gets the " + target + ".";
    }

    /**
     * Generate setter comment.
     *
     * @param methodName
     *            the method name
     *
     * @return the string
     */
    String generateSetterComment(String methodName) {
        String field = getFieldFromSetter(methodName);
        String target = field != null ? splitCamelCaseLower(field) : splitCamelCaseLower(methodName);
        return "Sets the " + target + ".";
    }

    /**
     * Generate method comment.
     *
     * @param methodName
     *            the method name
     *
     * @return the string
     */
    String generateMethodComment(String methodName) {
        return capitalize(splitCamelCaseLower(methodName)) + ".";
    }

    // -------------------------------------------------------------------------
    // Tag text generators
    // -------------------------------------------------------------------------

    /**
     * Generate param comment.
     *
     * @param paramName
     *            the param name
     *
     * @return the string
     */
    String generateParamComment(String paramName) {
        return "the " + splitCamelCaseLower(paramName);
    }

    /**
     * Generate return comment.
     *
     * @param returnTypeName
     *            the return type name
     *
     * @return the string
     */
    String generateReturnComment(String returnTypeName) {
        // Special-case booleans to match JAutodoc Eclipse plugin output
        if ("boolean".equals(returnTypeName) || "Boolean".equals(returnTypeName)) {
            return "true, if successful";
        }
        // Strip generic parameters for the description, e.g. "List<String>" -> "list"
        String baseType = returnTypeName.contains("<") ? returnTypeName.substring(0, returnTypeName.indexOf('<'))
                : returnTypeName;
        return "the " + splitCamelCaseLower(baseType);
    }

    /**
     * Generate throws comment.
     *
     * @param exceptionName
     *            the exception name
     *
     * @return the string
     */
    String generateThrowsComment(String exceptionName) {
        // Strip package prefix if present
        String simple = exceptionName.contains(".") ? exceptionName.substring(exceptionName.lastIndexOf('.') + 1)
                : exceptionName;
        return "the " + splitCamelCaseLower(simple);
    }

    // -------------------------------------------------------------------------
    // CamelCase splitting helpers
    // -------------------------------------------------------------------------

    /**
     * Split camel case lower.
     *
     * @param name
     *            the name
     *
     * @return the string
     */
    String splitCamelCaseLower(String name) {
        List<String> words = splitWords(name);
        if (words.isEmpty()) {
            return name != null ? name : "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(words.get(i).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Splits an identifier into its constituent words by camelCase transitions and underscore separators.
     * <p>
     * Examples:
     * <ul>
     * <li>{@code myField} → [my, Field]
     * <li>{@code XMLParser} → [XML, Parser]
     * <li>{@code _privateVar} → [private, Var]
     * </ul>
     */
    private List<String> splitWords(String name) {
        List<String> words = new ArrayList<>();
        if (name == null || name.isEmpty()) {
            return words;
        }

        // Skip leading underscores/dollars
        int start = 0;
        while (start < name.length() && (name.charAt(start) == '_' || name.charAt(start) == '$')) {
            start++;
        }
        if (start == name.length()) {
            words.add(name);
            return words;
        }

        StringBuilder word = new StringBuilder();
        for (int i = start; i < name.length(); i++) {
            char c = name.charAt(i);

            // Underscore or dollar sign is a word separator
            if (c == '_' || c == '$') {
                if (word.length() > 0) {
                    words.add(word.toString());
                    word = new StringBuilder();
                }
                continue;
            }

            if (Character.isUpperCase(c) && word.length() > 0) {
                char prev = word.charAt(word.length() - 1);
                if (!Character.isUpperCase(prev)) {
                    // lowerToUpper transition: "myFoo" -> split before F
                    words.add(word.toString());
                    word = new StringBuilder();
                } else if (i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1))) {
                    // uppercase run followed by lower: "XMLParser" -> split between L and P
                    words.add(word.toString());
                    word = new StringBuilder();
                }
            }
            word.append(c);
        }
        if (word.length() > 0) {
            words.add(word.toString());
        }
        return words;
    }
}
