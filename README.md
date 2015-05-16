# Gradle Sonatype Nexus plugin

![Sonatype Logo](http://media.marketwire.com/attachments/200910/580330_sonatype.gif)

The plugin provides support for configuring and uploading artifacts to [Sonatype Nexus](http://www.sonatype.org/nexus/). It can
be configured to deploy to a self-hosted instance of Nexus or Sonatype OSS. The default setup is to publish
to Sonatype OSS. Currently, Java and Groovy project artifact generation is supported. In addition to the JAR and POM file
 artifacts containing the JavaDocs and source files are created. Signing the artifacts is optional.

[![Build Status](https://snap-ci.com/bmuschko/gradle-nexus-plugin/branch/master/build_image)](https://snap-ci.com/bmuschko/gradle-nexus-plugin/branch/master)

## Usage

To use the Sonatype Nexus plugin, include in your build script:

```groovy
apply plugin: 'com.bmuschko.nexus'
```

The plugin JAR needs to be defined in the classpath of your build script. It is directly available on
[Bintray](https://bintray.com/bmuschko/gradle-plugins/com.bmuschko%3Agradle-nexus-plugin).
Alternatively, you can download it from GitHub and deploy it to your local repository. The following code snippet shows an
example on how to retrieve it from Bintray:

```groovy
 buildscript {
     repositories {
         jcenter()
     }
     dependencies {
         classpath 'com.bmuschko:gradle-nexus-plugin:2.3.1'
     }
 }
```

## Tasks

The Nexus plugin can add three tasks to your project:
* `javadocJar`: Assembles a jar archive containing the generated Javadoc API documentation of this project (added by default).
* `sourcesJar`: Assembles a jar archive containing the main sources of this project (added by default).
* `testsJar`: Assembles a jar archive containing the test sources of this project.

The output of all tasks is added to the `archives` configuration. To tell the plugin that any of these tasks should be
added to the project or removed, you will need to set a specific extension property.

Additionally, it applies the [Maven plugin](http://gradle.org/docs/current/userguide/maven_plugin.html) as well
as `signing` in order to leverage maven's `install` and `uploadArchives` tasks.

## Extension properties

The plugin defines the following extension properties in the `extraArchive` closure:

* `javadoc`: Adds Javadoc JAR task to project (defaults to true).
* `sources`: Adds sources JAR task to project (defaults to true).
* `tests`: Adds test sources JAR task to project (defaults to false).

The plugin defines the following extension properties in the `nexus` closure:

* `sign`: Specifies whether to sign the artifacts using the [signing plugin](http://gradle.org/docs/current/userguide/signing_plugin.html) (defaults to true).
* `configuration`: The custom configuration used to publish artifacts (defaults to `archives`).
* `repositoryUrl`: The stable release repository URL (defaults to `https://oss.sonatype.org/service/local/staging/deploy/maven2/`).
* `snapshotRepositoryUrl`: The snapshot repository URL (defaults to `https://oss.sonatype.org/content/repositories/snapshots/`).

## Additional configuration

### POM customization

In addition to the convention properties the automatically generated POM file can be modified. To provide the data for
the POM generation specify the information within the configuration element `modifyPom.project`.

```groovy
 modifyPom {
     project {
         ...
     }
 }
```

### Credentials

In your `~/.gradle/gradle.properties` you will need to set the mandatory Nexus credentials required for uploading your artifacts.

```groovy
 nexusUsername = yourUsername
 nexusPassword = yourPassword
```

If you don't specify one of these properties, the plugin will prompt your for their values in the console.

### Example

```groovy
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
 
 extraArchive {
     sources = false
     tests = true
     javadoc = false
 }

 nexus {
     sign = true
     repositoryUrl = 'http://localhost:8081/nexus/content/repositories/internal/'
     snapshotRepositoryUrl = 'http://localhost:8081/nexus/content/repositories/internal-snapshots/'
 }
```

## FAQ

**How do I publish my artifacts to the Central Repository aka Maven Central aka Sonatype OSS?**

By default the plugin is configured to upload your artifacts to the release and snapshot repository URLs of Sonatype OSS.
There's no additional configuration required. If you want to tweak the automatically generated POM file please you the
`modifyPom` closure. Make sure to stick to the process described in
the [Central Repository Documentation](http://central.sonatype.org/pages/producers.html).
