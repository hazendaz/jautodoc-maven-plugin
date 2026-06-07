/*
 * SPDX-License-Identifier: EPL-2.0
 * See LICENSE file for details.
 *
 * Copyright 2018-2026 hazendaz
 */
package com.example;

public class SimpleClass {

    private String name;

    public SimpleClass(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void doSomething(int count, String label) throws IllegalArgumentException {
        // implementation
    }

    public boolean isActive() {
        return true;
    }
}
