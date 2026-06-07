/*
 *     Copyright 2011-2026 the original author or authors.
 *
 *     All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 *     Public License v1.0 which accompanies this distribution, and is available at
 *
 *     https://www.eclipse.org/legal/epl-v10.html.
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
