Jautodoc Maven Plugin
=====================

[![Build Status](https://travis-ci.org/hazendaz/jautodoc-maven-plugin.svg?branch=master)](https://travis-ci.org/hazendaz/jautodoc-maven-plugin)
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
                    <archiveDir>distro</archiveDir>
                    <fileName>installDistro.sh</fileName>
                    <label>Distro Self Extraction</label>
                    <startupScript>./runDistroScript.sh</startupScript>
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
