Jautodoc Maven Plugin
=====================

[![Java CI](https://github.com/hazendaz/jautodoc-maven-plugin/actions/workflows/ci.yaml/badge.svg)](https://github.com/hazendaz/jautodoc-maven-plugin/actions/workflows/ci.yaml)
[![Coverage Status](https://coveralls.io/repos/github/hazendaz/jautodoc-maven-plugin/badge.svg?branch=master)](https://coveralls.io/github/hazendaz/jautodoc-maven-plugin?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.hazendaz.,avem/jautodoc-maven-plugin.svg)](https://central.sonatype.com/artifact/com.github.hazendaz.maven/jautodoc-maven-plugin)
[![Eclipse](https://img.shields.io/badge/license-Eclipse-blue.svg)](https://www.eclipse.org/legal/epl-v10.html)

![hazendaz](src/site/resources/images/hazendaz-banner.jpg)

Jautodoc Maven Plugin provides maven integration for eclipse jautodoc plugin.

JAutodoc is an Eclipse Plugin for automatically adding Javadoc and file headers to your source code.

See [jautodoc](https://github.com/mkesting/jautodoc/)

Example Usage

```xml
<plugin>
    <groupId>com.github.hazendaz.maven</groupId>
    <artifactId>jautodoc-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        -- TBD --
    </configuration>
    <executions>
        <execution>
            <id>jautodoc</id>
            <goals>
                <goal>jautodoc</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

This project is a work in progress based off jautodoc using public eclipse api's.  Parts of code needed rewritten due to jautodoc not being in central and hard requirement on eclipse workspace.  This project does not require the eclipse workspace.

This is under beta until parity between the eclipse plugin and this plugin can be reached.

Outstanding issues
- The auto generated content is not the same currently.
- Adding to existing javadocs currently not supported.
- But with listeners and 'get' styled tests currently do not occur here like with jautodoc but making mention so it doesn't pop up later.
