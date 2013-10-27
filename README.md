# Gradle Sonatype Nexus plugin

![Sonatype Logo](http://media.marketwire.com/attachments/200910/580330_sonatype.gif)

The plugin provides support for configuring and uploading artifacts to [Sonatype Nexus](http://www.sonatype.org/nexus/). It can
be configured to deploy to a self-hosted instance of Nexus or Sonatype OSS. The default setup is to publish
to Sonatype OSS. Currently, Java and Groovy project artifact generation is supported. In addition to the JAR and POM file
 artifacts containing the JavaDocs and source files are created. Signing the artifacts is optional.

## Usage

To use the Sonatype Nexus plugin, include in your build script:

    apply plugin: 'nexus'

The plugin JAR needs to be defined in the classpath of your build script. It is directly available on
[Maven Central](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.gradle.api.plugins%22%20AND%20a%3A%22gradle-nexus-plugin%22).
Alternatively, you can download it from GitHub and deploy it to your local repository. The following code snippet shows an
example on how to retrieve it from Maven Central:

    buildscript {
        repositories {
            mavenCentral()
        }

        dependencies {
            classpath 'org.gradle.api.plugins:gradle-nexus-plugin:0.4'
        }
    }

## Tasks

The Nexus plugin can add three tasks to your project:
* `javadocJar`: Assembles a jar archive containing the generated Javadoc API documentation of this project (added by default).
* `sourcesJar`: Assembles a jar archive containing the main sources of this project (added by default).
* `testsJar`: Assembles a jar archive containing the test sources of this project.

The output of all tasks is added to the `archives` configuration. To tell the plugin that any of these tasks should be
added to the project or removed, you will need to set a specific extension property.

Additionally, it applies the [Maven plugin](http://gradle.org/docs/current/userguide/maven_plugin.html) plugin as well
as `signing` in order to leverage maven's `install` and `uploadArchives` tasks.

## Convention properties

The Nexus plugin defines the following convention properties in the `nexus` closure:

* `attachJavadoc`: Adds Javadoc JAR task to project (defaults to true).
* `attachSources`: Adds sources JAR task to project (defaults to true).
* `attachTests`: Adds test sources JAR task to project (defaults to false).
* `sign`: Specifies whether to sign the artifacts using the [signing plugin](http://gradle.org/docs/current/userguide/signing_plugin.html) (defaults to true).
* `repositoryUrl`: The stable release repository URL (defaults to `https://oss.sonatype.org/service/local/staging/deploy/maven2/`).
* `snapshotRepositoryUrl`: The stable release repository URL (defaults to `https://oss.sonatype.org/content/repositories/snapshots/`).

## Additional configuration

### POM customization

In addition to the convention properties the automatically generated POM file can be modified. To provide the data for
the POM generation specify the information within the configuration element `modifyPom.project`.

    modifyPom {
        project {
            ...
        }
    }

### Credentials

In your `~/.gradle/gradle.properties` you will need to set the mandatory Nexus credentials required for uploading your artifacts.

    nexusUsername = yourUsername
    nexusPassword = yourPassword

If you don't specify one of these properties, the plugin will prompt your for their values in the console.

### Example

    modifyPom {
        project {
            name 'Gradle Sonatype Nexus plugin'
            description 'Gradle plugin that provides tasks for configuring and uploading artifacts to Sonatype Nexus.'
            url 'https://github.com/bmuschko/gradle-nexus-plugin'
            inceptionYear '2012'

            scm {
                url 'https://github.com/bmuschko/gradle-nexus-plugin'
                connection 'scm:https://bmuschko@github.com/bmuschko/gradle-nexus-plugin.git'
                developerConnection 'scm:git://github.com/bmuschko/gradle-nexus-plugin.git'
            }

            licenses {
                license {
                    name 'The Apache Software License, Version 2.0'
                    url 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                    distribution 'repo'
                }
            }

            developers {
                developer {
                    id 'bmuschko'
                    name 'Benjamin Muschko'
                    email 'benjamin.muschko@gmail.com'
                }
            }
        }
    }

    nexus {
        attachSources = false
        attachTests = true
        attachJavadoc = false
        sign = true
        repositoryUrl = 'http://localhost:8081/nexus/content/repositories/internal/'
        snapshotRepositoryUrl = 'http://localhost:8081/nexus/content/repositories/internal-snapshots/'
    }

## FAQ

**How do I publish my artifacts to Maven Central aka Sonatype OSS?**

By default the plugin is configured to upload your artifacts to the release and snapshot repository URLs of Sonatype OSS.
There's no additional configuration required. If you want to tweak the automatically generated POM file please you the
`modifyPom` closure. Make sure to stick to the process described in the [Sonatype OSS Maven Repository usage guide](https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide).