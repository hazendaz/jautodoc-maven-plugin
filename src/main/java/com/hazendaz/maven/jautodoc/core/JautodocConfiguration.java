/*
 *     Copyright 2011-2026 the original author or authors.
 *
 *     All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 *     Public License v1.0 which accompanies this distribution, and is available at
 *
 *     https://www.eclipse.org/legal/epl-v10.html.
 */
package com.hazendaz.maven.jautodoc.core;

/**
 * Immutable configuration for the standalone Jautodoc engine. All properties mirror Eclipse JAutodoc's preferences to
 * maintain full compatibility.
 */
public final class JautodocConfiguration {

    // ---- Processing mode ----

    /** Javadoc processing mode (complete / keep / replace). */
    private JautodocMode mode = JautodocMode.COMPLETE;

    // ---- Header options ----

    /** Whether to add a file header. */
    private boolean addHeader;

    /** Whether to replace an existing file header. */
    private boolean replaceHeader;

    /** Whether the file header should use multi-line comment style (/* rather than /**). */
    private boolean multiCommentHeader;

    /** When true only header operations are performed; no Javadoc is generated. */
    private boolean headerOnly;

    /** The literal header text to insert (may contain newlines; the engine wraps it in a comment block). */
    private String headerText = "";

    // ---- Visibility filters ----

    /** Comment public members. */
    private boolean visibilityPublic = true;

    /** Comment package-private members. */
    private boolean visibilityPackage = true;

    /** Comment protected members. */
    private boolean visibilityProtected;

    /** Comment private members. */
    private boolean visibilityPrivate;

    // ---- Member-type filters ----

    /** Comment type declarations (classes, interfaces, enums). */
    private boolean commentTypes = true;

    /** Comment field declarations. */
    private boolean commentFields = true;

    /** Comment method declarations. */
    private boolean commentMethods = true;

    /** Only comment getter and setter methods (and constructors). */
    private boolean getterSetterOnly;

    /** Exclude getter and setter methods from commenting. */
    private boolean excludeGetterSetter;

    /** Exclude methods that override or implement a parent/interface method (@Override). */
    private boolean excludeOverrides = true;

    // ---- Comment-generation options ----

    /** Generate a dummy description comment from the element name. */
    private boolean createDummyComment = true;

    /** Emit field comments as a single-line /** comment. */
    private boolean singleLineComment = true;

    /** Prefix generated comments with TODO. */
    private boolean addTodoForAutodoc;

    /** Format the output using the Eclipse JDT formatter after Javadoc insertion. */
    private boolean useEclipseFormatter;

    // ---- Getter/setter-from-field options ----

    /** Derive getter/setter descriptions from the corresponding field's Javadoc. */
    private boolean getterSetterFromField;

    /** Use only the first sentence of the field's Javadoc for getter/setter descriptions. */
    private boolean getterSetterFromFieldFirst;

    /** Replace existing getter/setter Javadoc when deriving from field. */
    private boolean getterSetterFromFieldReplace = true;

    // =========================================================================
    // Getters & setters
    // =========================================================================

    /**
     * Gets the mode.
     *
     * @return the mode
     */
    public JautodocMode getMode() {
        return mode;
    }

    /**
     * Sets the mode.
     *
     * @param mode
     *            the new mode
     */
    public void setMode(JautodocMode mode) {
        this.mode = mode != null ? mode : JautodocMode.COMPLETE;
    }

    /**
     * Checks if is adds the header.
     *
     * @return true, if is adds the header
     */
    public boolean isAddHeader() {
        return addHeader;
    }

    /**
     * Sets the adds the header.
     *
     * @param addHeader
     *            the new adds the header
     */
    public void setAddHeader(boolean addHeader) {
        this.addHeader = addHeader;
    }

    /**
     * Checks if is replace header.
     *
     * @return true, if is replace header
     */
    public boolean isReplaceHeader() {
        return replaceHeader;
    }

    /**
     * Sets the replace header.
     *
     * @param replaceHeader
     *            the new replace header
     */
    public void setReplaceHeader(boolean replaceHeader) {
        this.replaceHeader = replaceHeader;
    }

    /**
     * Checks if is multi comment header.
     *
     * @return true, if is multi comment header
     */
    public boolean isMultiCommentHeader() {
        return multiCommentHeader;
    }

    /**
     * Sets the multi comment header.
     *
     * @param multiCommentHeader
     *            the new multi comment header
     */
    public void setMultiCommentHeader(boolean multiCommentHeader) {
        this.multiCommentHeader = multiCommentHeader;
    }

    /**
     * Checks if is header only.
     *
     * @return true, if is header only
     */
    public boolean isHeaderOnly() {
        return headerOnly;
    }

    /**
     * Sets the header only.
     *
     * @param headerOnly
     *            the new header only
     */
    public void setHeaderOnly(boolean headerOnly) {
        this.headerOnly = headerOnly;
    }

    /**
     * Gets the header text.
     *
     * @return the header text
     */
    public String getHeaderText() {
        return headerText;
    }

    /**
     * Sets the header text.
     *
     * @param headerText
     *            the new header text
     */
    public void setHeaderText(String headerText) {
        this.headerText = headerText != null ? headerText : "";
    }

    /**
     * Checks if is visibility public.
     *
     * @return true, if is visibility public
     */
    public boolean isVisibilityPublic() {
        return visibilityPublic;
    }

    /**
     * Sets the visibility public.
     *
     * @param visibilityPublic
     *            the new visibility public
     */
    public void setVisibilityPublic(boolean visibilityPublic) {
        this.visibilityPublic = visibilityPublic;
    }

    /**
     * Checks if is visibility package.
     *
     * @return true, if is visibility package
     */
    public boolean isVisibilityPackage() {
        return visibilityPackage;
    }

    /**
     * Sets the visibility package.
     *
     * @param visibilityPackage
     *            the new visibility package
     */
    public void setVisibilityPackage(boolean visibilityPackage) {
        this.visibilityPackage = visibilityPackage;
    }

    /**
     * Checks if is visibility protected.
     *
     * @return true, if is visibility protected
     */
    public boolean isVisibilityProtected() {
        return visibilityProtected;
    }

    /**
     * Sets the visibility protected.
     *
     * @param visibilityProtected
     *            the new visibility protected
     */
    public void setVisibilityProtected(boolean visibilityProtected) {
        this.visibilityProtected = visibilityProtected;
    }

    /**
     * Checks if is visibility private.
     *
     * @return true, if is visibility private
     */
    public boolean isVisibilityPrivate() {
        return visibilityPrivate;
    }

    /**
     * Sets the visibility private.
     *
     * @param visibilityPrivate
     *            the new visibility private
     */
    public void setVisibilityPrivate(boolean visibilityPrivate) {
        this.visibilityPrivate = visibilityPrivate;
    }

    /**
     * Checks if is comment types.
     *
     * @return true, if is comment types
     */
    public boolean isCommentTypes() {
        return commentTypes;
    }

    /**
     * Sets the comment types.
     *
     * @param commentTypes
     *            the new comment types
     */
    public void setCommentTypes(boolean commentTypes) {
        this.commentTypes = commentTypes;
    }

    /**
     * Checks if is comment fields.
     *
     * @return true, if is comment fields
     */
    public boolean isCommentFields() {
        return commentFields;
    }

    /**
     * Sets the comment fields.
     *
     * @param commentFields
     *            the new comment fields
     */
    public void setCommentFields(boolean commentFields) {
        this.commentFields = commentFields;
    }

    /**
     * Checks if is comment methods.
     *
     * @return true, if is comment methods
     */
    public boolean isCommentMethods() {
        return commentMethods;
    }

    /**
     * Sets the comment methods.
     *
     * @param commentMethods
     *            the new comment methods
     */
    public void setCommentMethods(boolean commentMethods) {
        this.commentMethods = commentMethods;
    }

    /**
     * Checks if is getter setter only.
     *
     * @return true, if is getter setter only
     */
    public boolean isGetterSetterOnly() {
        return getterSetterOnly;
    }

    /**
     * Sets the getter setter only.
     *
     * @param getterSetterOnly
     *            the new getter setter only
     */
    public void setGetterSetterOnly(boolean getterSetterOnly) {
        this.getterSetterOnly = getterSetterOnly;
    }

    /**
     * Checks if is exclude getter setter.
     *
     * @return true, if is exclude getter setter
     */
    public boolean isExcludeGetterSetter() {
        return excludeGetterSetter;
    }

    /**
     * Sets the exclude getter setter.
     *
     * @param excludeGetterSetter
     *            the new exclude getter setter
     */
    public void setExcludeGetterSetter(boolean excludeGetterSetter) {
        this.excludeGetterSetter = excludeGetterSetter;
    }

    /**
     * Checks if is exclude overrides.
     *
     * @return true, if is exclude overrides
     */
    public boolean isExcludeOverrides() {
        return excludeOverrides;
    }

    /**
     * Sets the exclude overrides.
     *
     * @param excludeOverrides
     *            the new exclude overrides
     */
    public void setExcludeOverrides(boolean excludeOverrides) {
        this.excludeOverrides = excludeOverrides;
    }

    /**
     * Checks if is creates the dummy comment.
     *
     * @return true, if is creates the dummy comment
     */
    public boolean isCreateDummyComment() {
        return createDummyComment;
    }

    /**
     * Sets the creates the dummy comment.
     *
     * @param createDummyComment
     *            the new creates the dummy comment
     */
    public void setCreateDummyComment(boolean createDummyComment) {
        this.createDummyComment = createDummyComment;
    }

    /**
     * Checks if is single line comment.
     *
     * @return true, if is single line comment
     */
    public boolean isSingleLineComment() {
        return singleLineComment;
    }

    /**
     * Sets the single line comment.
     *
     * @param singleLineComment
     *            the new single line comment
     */
    public void setSingleLineComment(boolean singleLineComment) {
        this.singleLineComment = singleLineComment;
    }

    /**
     * Checks if is adds the todo for autodoc.
     *
     * @return true, if is adds the todo for autodoc
     */
    public boolean isAddTodoForAutodoc() {
        return addTodoForAutodoc;
    }

    /**
     * Sets the adds the todo for autodoc.
     *
     * @param addTodoForAutodoc
     *            the new adds the todo for autodoc
     */
    public void setAddTodoForAutodoc(boolean addTodoForAutodoc) {
        this.addTodoForAutodoc = addTodoForAutodoc;
    }

    /**
     * Checks if is use eclipse formatter.
     *
     * @return true, if is use eclipse formatter
     */
    public boolean isUseEclipseFormatter() {
        return useEclipseFormatter;
    }

    /**
     * Sets the use eclipse formatter.
     *
     * @param useEclipseFormatter
     *            the new use eclipse formatter
     */
    public void setUseEclipseFormatter(boolean useEclipseFormatter) {
        this.useEclipseFormatter = useEclipseFormatter;
    }

    /**
     * Checks if is getter setter from field.
     *
     * @return true, if is getter setter from field
     */
    public boolean isGetterSetterFromField() {
        return getterSetterFromField;
    }

    /**
     * Sets the getter setter from field.
     *
     * @param getterSetterFromField
     *            the new getter setter from field
     */
    public void setGetterSetterFromField(boolean getterSetterFromField) {
        this.getterSetterFromField = getterSetterFromField;
    }

    /**
     * Checks if is getter setter from field first.
     *
     * @return true, if is getter setter from field first
     */
    public boolean isGetterSetterFromFieldFirst() {
        return getterSetterFromFieldFirst;
    }

    /**
     * Sets the getter setter from field first.
     *
     * @param getterSetterFromFieldFirst
     *            the new getter setter from field first
     */
    public void setGetterSetterFromFieldFirst(boolean getterSetterFromFieldFirst) {
        this.getterSetterFromFieldFirst = getterSetterFromFieldFirst;
    }

    /**
     * Checks if is getter setter from field replace.
     *
     * @return true, if is getter setter from field replace
     */
    public boolean isGetterSetterFromFieldReplace() {
        return getterSetterFromFieldReplace;
    }

    /**
     * Sets the getter setter from field replace.
     *
     * @param getterSetterFromFieldReplace
     *            the new getter setter from field replace
     */
    public void setGetterSetterFromFieldReplace(boolean getterSetterFromFieldReplace) {
        this.getterSetterFromFieldReplace = getterSetterFromFieldReplace;
    }
}
