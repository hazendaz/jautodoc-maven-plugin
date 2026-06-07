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
 * The Class SimpleClass.
 */
public class SimpleClass {

    private String name;

    /**
     * Instantiates a new simple class.
     *
     * @param name the name
     */
    public SimpleClass(String name) {
        this.name = name;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Do something.
     *
     * @param count the count
     * @param label the label
     * @throws IllegalArgumentException the illegal argument exception
     */
    public void doSomething(int count, String label) throws IllegalArgumentException {
        // implementation
    }

    /**
     * Checks if is active.
     *
     * @return true, if successful
     */
    public boolean isActive() {
        return true;
    }
}
