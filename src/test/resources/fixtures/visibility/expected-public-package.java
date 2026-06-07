/*
 * SPDX-License-Identifier: EPL-2.0
 * See LICENSE file for details.
 *
 * Copyright 2018-2026 hazendaz
 */
package com.example;

/**
 * The Class VisibilityClass.
 */
public class VisibilityClass {

    /** The public field. */
    public String publicField;

    protected String protectedField;

    /** The package field. */
    String packageField;

    private String privateField;

    /**
     * Public method.
     */
    public void publicMethod() {
    }

    protected void protectedMethod() {
    }

    /**
     * Package method.
     */
    void packageMethod() {
    }

    private void privateMethod() {
    }
}
