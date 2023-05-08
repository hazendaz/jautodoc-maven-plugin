Jautodoc Maven Plugin
=====================

[![Java CI](https://github.com/hazendaz/jautodoc-maven-plugin/workflows/Java%20CI/badge.svg)](https://github.com/hazendaz/jautodoc-maven-plugin/actions?query=workflow%3A%22Java+CI%22)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.github.hazendaz.maven/jautodoc-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.hazendaz.maven/jautodoc-maven-plugin)
[![Eclipse](https://img.shields.io/badge/license-Eclipse-blue.svg)](https://www.eclipse.org/legal/epl-v10.html)

![hazendaz](src/site/resources/images/hazendaz-banner.jpg)

Jautodoc Maven Plugin provides maven integration for eclipse jautodoc plugin.

JAutodoc is an Eclipse Plugin for automatically adding Javadoc and file headers to your source code.

See [jautodoc](http://jautodoc.sourceforge.net/)

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

WIP - This project is a work in progress.  To even begin to work, needs jautodoc deployed to central.  Code written in a number of years old and needs to be looked at again.  Point of pushing this up is to finally suggest work might happen here.
