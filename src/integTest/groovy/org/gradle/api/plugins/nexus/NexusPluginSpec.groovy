/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.plugins.nexus

import org.gradle.api.plugins.MavenPlugin
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.Task
import spock.lang.Specification

import static org.spockframework.util.Assert.fail

class NexusPluginSpec extends Specification {
    final static M2_HOME_DIR = new File(System.properties['user.home'], '.m2/repository')
    File integTestDir
    File buildFile

    def setup() {
        integTestDir = new File('build/integTest')

        if(!integTestDir.mkdirs()) {
            fail('Unable to create integration test directory.')
        }

        buildFile = new File(integTestDir, 'build.gradle')

        if(!buildFile.createNewFile()) {
            fail('Unable to create Gradle build script.')
        }

        buildFile << """
buildscript {
    dependencies {
        classpath files('../classes/main')
    }
}

apply plugin: 'java'
apply plugin: org.gradle.api.plugins.nexus.NexusPlugin

"""
    }

    def cleanup() {
        if(!buildFile.delete()) {
            fail('Unable to delete Gradle build script.')
        }

        if(!integTestDir.deleteDir()) {
            fail('Unable to delete integration test directory.')
        }
    }

    def "Adds sources and Javadoc JAR tasks by default for Java project"() {
        when:
        GradleProject project = runTasks(integTestDir, 'tasks')

        then:
        Task sourcesJarTask = project.tasks.find { task -> task.name == 'sourcesJar' }
        sourcesJarTask
        sourcesJarTask.description == 'Assembles a jar archive containing the main sources of this project.'
        Task javadocJarTask = project.tasks.find { task -> task.name == 'javadocJar' }
        javadocJarTask
        javadocJarTask.description == 'Assembles a jar archive containing the generated Javadoc API documentation of this project.'
        !project.tasks.find { task -> task.name == 'testsJar' }
    }

    def "Adds sources and Javadoc JAR tasks by default for Groovy project"() {
        when:
        GradleProject project = runTasks(integTestDir, 'tasks')

        then:
        Task sourcesJarTask = project.tasks.find { task -> task.name == 'sourcesJar' }
        sourcesJarTask
        sourcesJarTask.description == 'Assembles a jar archive containing the main sources of this project.'
        Task javadocJarTask = project.tasks.find { task -> task.name == 'javadocJar' }
        javadocJarTask
        javadocJarTask.description == 'Assembles a jar archive containing the generated Javadoc API documentation of this project.'
        !project.tasks.find { task -> task.name == 'testsJar' }
    }

    def "Adds tests JAR task if configured"() {
        when:
        buildFile << """
nexus {
    attachTests = true
}
"""
        GradleProject project = runTasks(integTestDir, 'tasks')

        then:
        Task sourcesJarTask = project.tasks.find { task -> task.name == 'sourcesJar' }
        sourcesJarTask
        sourcesJarTask.description == 'Assembles a jar archive containing the main sources of this project.'
        Task javadocJarTask = project.tasks.find { task -> task.name == 'javadocJar' }
        javadocJarTask
        javadocJarTask.description == 'Assembles a jar archive containing the generated Javadoc API documentation of this project.'
        Task testsJarTask = project.tasks.find { task -> task.name == 'testsJar'}
        testsJarTask
        testsJarTask.description == 'Assembles a jar archive containing the test sources of this project.'
    }

    def "Disables additional JAR creation"() {
        when:
        buildFile << """
nexus {
    attachSources = false
    attachJavadoc = false
}
"""
        GradleProject project = runTasks(integTestDir, 'tasks')

        then:
        !project.tasks.find { task -> task.name == 'sourcesJar'}
        !project.tasks.find { task -> task.name == 'javadocJar'}
        !project.tasks.find { task -> task.name == 'testsJar'}
    }

    def "Creates all configured JARs for default configuration"() {
        when:
        buildFile << """
nexus {
    attachTests = true
}
"""
        GradleProject project = runTasks(integTestDir, 'assemble')

        then:
        File libsDir = new File(integTestDir, 'build/libs')
        def expectedFilenames = ["${project.name}.jar", "${project.name}-javadoc.jar", "${project.name}-sources.jar",
                                 "${project.name}-tests.jar"]
        assertExistingFiles(libsDir, expectedFilenames)
    }

    def "Creates all configured JARs for custom configuration as Configuration type"() {
        when:
        buildFile << """
configurations {
    myConfig
}

nexus {
    attachTests = true
    configuration = configurations.myConfig
}
"""
        GradleProject project = runTasks(integTestDir, 'assemble')

        then:
        File libsDir = new File(integTestDir, 'build/libs')
        def expectedFilenames = ["${project.name}.jar", "${project.name}-javadoc.jar", "${project.name}-sources.jar",
                                 "${project.name}-tests.jar"]
        assertExistingFiles(libsDir, expectedFilenames)
    }

    def "Creates all configured JARs for custom configuration defined as String type"() {
        when:
        buildFile << """
configurations {
    myConfig
}

nexus {
    attachTests = true
    configuration = 'myConfig'
}
"""
        GradleProject project = runTasks(integTestDir, 'assemble')

        then:
        File libsDir = new File(integTestDir, 'build/libs')
        def expectedFilenames = ["${project.name}.jar", "${project.name}-javadoc.jar", "${project.name}-sources.jar",
                                 "${project.name}-tests.jar"]
        assertExistingFiles(libsDir, expectedFilenames)
    }

    def "Uploads all configured JARs, metadata and signature artifacts for release version with default configuration"() {
        when:
        buildFile << """
version = '1.0'
group = 'org.gradle.mygroup'

nexus {
    attachTests = true
    repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadArchives')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0')
        def expectedFilenames = ["${project.name}-1.0.jar", "${project.name}-1.0.jar.asc", "${project.name}-1.0.pom",
                                 "${project.name}-1.0.pom.asc", "${project.name}-1.0-javadoc.jar", "${project.name}-1.0-javadoc.jar.asc",
                                 "${project.name}-1.0-sources.jar", "${project.name}-1.0-sources.jar.asc", "${project.name}-1.0-tests.jar",
                                 "${project.name}-1.0-tests.jar.asc"]
        assertExistingFiles(repoDir, expectedFilenames)
    }

    def "Uploads all configured JARs, metadata and signature artifacts for release version with custom configuration"() {
        when:
        buildFile << """
version = '1.0'
group = 'org.gradle.mygroup'

configurations {
    myConfig.extendsFrom signatures
}

artifacts {
    myConfig jar
}

nexus {
    attachTests = true
    repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    configuration = configurations.myConfig
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadMyConfig')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0')
        def expectedFilenames = ["${project.name}-1.0.jar", "${project.name}-1.0.jar.asc", "${project.name}-1.0.pom",
                                 "${project.name}-1.0.pom.asc", "${project.name}-1.0-javadoc.jar", "${project.name}-1.0-javadoc.jar.asc",
                                 "${project.name}-1.0-sources.jar", "${project.name}-1.0-sources.jar.asc", "${project.name}-1.0-tests.jar",
                                 "${project.name}-1.0-tests.jar.asc"]
        assertExistingFiles(repoDir, expectedFilenames)
    }

    def "Uploads all configured JARs and metadata without signature artifacts for release version with default configuration"() {
        when:
        buildFile << """
version = '1.0'
group = 'org.gradle.mygroup'

nexus {
    attachTests = true
    repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    sign = false
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadArchives')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0')
        def expectedFilenames = ["${project.name}-1.0.jar", "${project.name}-1.0.pom", "${project.name}-1.0-javadoc.jar",
                                 "${project.name}-1.0-sources.jar", "${project.name}-1.0-tests.jar"]
        assertExistingFiles(repoDir, expectedFilenames)
        assertNoSignatureFiles(repoDir)
    }

    def "Uploads all configured JARs and metadata without signature artifacts for release version with custom configuration"() {
        when:
        buildFile << """
version = '1.0'
group = 'org.gradle.mygroup'

configurations {
    myConfig.extendsFrom signatures
}

artifacts {
    myConfig jar
}

nexus {
    attachTests = true
    repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    sign = false
    configuration = configurations.myConfig
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadMyConfig')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0')
        def expectedFilenames = ["${project.name}-1.0.jar", "${project.name}-1.0.pom", "${project.name}-1.0-javadoc.jar",
                                 "${project.name}-1.0-sources.jar", "${project.name}-1.0-tests.jar"]
        assertExistingFiles(repoDir, expectedFilenames)
        assertNoSignatureFiles(repoDir)
    }

    def "Uploads all configured JARs, metadata and signature artifacts for snapshot version with default configuration"() {
        when:
        buildFile << """
version = '1.0-SNAPSHOT'
group = 'org.gradle.mygroup'

nexus {
    attachTests = true
    snapshotRepositoryUrl = 'file://$integTestDir.canonicalPath/repo'
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadArchives')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0-SNAPSHOT')
        def expectedFilenames = ["${project.name}-1\\.0-\\d+\\.\\d+-1\\.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\.jar.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\.pom", "${project.name}-1\\.0-\\d+\\.\\d+-1\\.pom.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-sources.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-sources.jar.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-tests.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-tests.jar.asc"]
        assertExistingFiles(repoDir, expectedFilenames)
    }

    def "Uploads all configured JARs, metadata and signature artifacts for snapshot version with custom configuration"() {
        when:
        buildFile << """
version = '1.0-SNAPSHOT'
group = 'org.gradle.mygroup'

configurations {
    myConfig.extendsFrom signatures
}

artifacts {
    myConfig jar
}

nexus {
    attachTests = true
    snapshotRepositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    configuration = configurations.myConfig
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadMyConfig')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0-SNAPSHOT')
        def expectedFilenames = ["${project.name}-1\\.0-\\d+\\.\\d+-1\\.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\.jar.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\.pom", "${project.name}-1\\.0-\\d+\\.\\d+-1\\.pom.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-sources.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-sources.jar.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-tests.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-tests.jar.asc"]
        assertExistingFiles(repoDir, expectedFilenames)
    }

    def "Uploads all configured JARs and metadata without signature artifacts for snapshot version with default configuration"() {
        when:
        buildFile << """
version = '1.0-SNAPSHOT'
group = 'org.gradle.mygroup'

nexus {
    attachTests = true
    snapshotRepositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    sign = false
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadArchives')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0-SNAPSHOT')
        def expectedFilenames = ["${project.name}-1\\.0-\\d+\\.\\d+-1\\.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\.pom",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-sources.jar",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-tests.jar"]
        assertExistingFiles(repoDir, expectedFilenames)
        assertNoSignatureFiles(repoDir)
    }

    def "Uploads all configured JARs and metadata without signature artifacts for snapshot version with custom configuration"() {
        when:
        buildFile << """
version = '1.0-SNAPSHOT'
group = 'org.gradle.mygroup'

configurations {
    myConfig.extendsFrom signatures
}

artifacts {
    myConfig jar
}

nexus {
    attachTests = true
    snapshotRepositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    sign = false
    configuration = configurations.myConfig
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadMyConfig')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0-SNAPSHOT')
        def expectedFilenames = ["${project.name}-1\\.0-\\d+\\.\\d+-1\\.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\.pom",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-sources.jar",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-tests.jar"]
        assertExistingFiles(repoDir, expectedFilenames)
        assertNoSignatureFiles(repoDir)
    }

    def "Installs all configured JARs, metadata and signature artifacts for release version with default configuration"() {
        setup:
        def projectCoordinates = [group: 'org.gradle.mygroup', name: 'integTest', version: '1.0']
        File installationDir = new File(M2_HOME_DIR, createInstallationDir(projectCoordinates))
        deleteMavenLocalInstallationDir(installationDir)

        when:
        buildFile << """
version = '$projectCoordinates.version'
group = '$projectCoordinates.group'

nexus {
    attachTests = true
}
"""
        runTasks(integTestDir, MavenPlugin.INSTALL_TASK_NAME)

        then:
        def expectedFilenames = ["${projectCoordinates.name}-${projectCoordinates.version}.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}.jar.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}.pom",
                                 "${projectCoordinates.name}-${projectCoordinates.version}.pom.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-javadoc.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-javadoc.jar.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-sources.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-sources.jar.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-tests.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-tests.jar.asc"]
        assertExistingFiles(installationDir, expectedFilenames)
    }

    def "Installs all configured JARs, metadata and signature artifacts for release version with custom configuration"() {
        setup:
        def projectCoordinates = [group: 'org.gradle.mygroup', name: 'integTest', version: '1.0']
        File installationDir = new File(M2_HOME_DIR, createInstallationDir(projectCoordinates))
        deleteMavenLocalInstallationDir(installationDir)

        when:
        buildFile << """
version = '$projectCoordinates.version'
group = '$projectCoordinates.group'

configurations {
    myConfig.extendsFrom signatures
}

artifacts {
    myConfig jar
}

nexus {
    attachTests = true
    configuration = configurations.myConfig
}
"""
        runTasks(integTestDir, MavenPlugin.INSTALL_TASK_NAME)

        then:
        def expectedFilenames = ["${projectCoordinates.name}-${projectCoordinates.version}.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}.jar.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}.pom",
                                 "${projectCoordinates.name}-${projectCoordinates.version}.pom.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-javadoc.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-javadoc.jar.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-sources.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-sources.jar.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-tests.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-tests.jar.asc"]
        assertExistingFiles(installationDir, expectedFilenames)
    }

    def "Installs all configured JARs, metadata and signature artifacts for snapshot version with default configuration"() {
        setup:
        def projectCoordinates = [group: 'org.gradle.mygroup', name: 'integTest', version: '1.0-SNAPSHOT']
        File installationDir = new File(M2_HOME_DIR, createInstallationDir(projectCoordinates))
        deleteMavenLocalInstallationDir(installationDir)

        when:
        buildFile << """
version = '$projectCoordinates.version'
group = '$projectCoordinates.group'

nexus {
    attachTests = true
}
"""
        runTasks(integTestDir, MavenPlugin.INSTALL_TASK_NAME)

        then:
        def expectedFilenames = ["${projectCoordinates.name}-${projectCoordinates.version}.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}.jar.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}.pom",
                                 "${projectCoordinates.name}-${projectCoordinates.version}.pom.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-javadoc.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-javadoc.jar.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-sources.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-sources.jar.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-tests.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-tests.jar.asc"]
        assertExistingFiles(installationDir, expectedFilenames)
    }

    def "Installs all configured JARs, metadata and signature artifacts for snapshot version with custom configuration"() {
        setup:
        def projectCoordinates = [group: 'org.gradle.mygroup', name: 'integTest', version: '1.0-SNAPSHOT']
        File installationDir = new File(M2_HOME_DIR, createInstallationDir(projectCoordinates))
        deleteMavenLocalInstallationDir(installationDir)

        when:
        buildFile << """
version = '$projectCoordinates.version'
group = '$projectCoordinates.group'

configurations {
    myConfig.extendsFrom signatures
}

artifacts {
    myConfig jar
}

nexus {
    attachTests = true
    configuration = configurations.myConfig
}
"""
        runTasks(integTestDir, MavenPlugin.INSTALL_TASK_NAME)

        then:
        def expectedFilenames = ["${projectCoordinates.name}-${projectCoordinates.version}.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}.jar.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}.pom",
                                 "${projectCoordinates.name}-${projectCoordinates.version}.pom.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-javadoc.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-javadoc.jar.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-sources.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-sources.jar.asc",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-tests.jar",
                                 "${projectCoordinates.name}-${projectCoordinates.version}-tests.jar.asc"]
        assertExistingFiles(installationDir, expectedFilenames)
    }

    private void deleteMavenLocalInstallationDir(File installationDir) {
        if(installationDir.exists()) {
            boolean success = installationDir.deleteDir()

            if(!success) {
                fail("Unable to delete existing Maven Local repository directory '$installationDir.canonicalPath'.")
            }
        }
    }

    private String createInstallationDir(projectCoordinates) {
        "${projectCoordinates.group.replaceAll('\\.', '/')}/$projectCoordinates.name/$projectCoordinates.version"
    }

    private void assertExistingFiles(File dir, List<String> requiredFilenames) {
        assertExistingDirectory(dir)
        def dirFileNames = dir.listFiles()*.name

        requiredFilenames.each { filename ->
            assert dirFileNames.find { it ==~ filename }
        }
    }

    private void assertNoSignatureFiles(File dir) {
        assertExistingDirectory(dir)
        def dirFileNames = dir.listFiles()*.name

        dirFileNames.each { filename ->
            assert !filename.endsWith('.asc')
        }
    }

    private void assertExistingDirectory(File dir) {
        if(!dir || !dir.exists()) {
            fail("Unable to check target directory '${dir?.canonicalPath}' for files.")
        }
    }

    private GradleProject runTasks(File projectDir, String... tasks) {
        ProjectConnection connection = GradleConnector.newConnector().forProjectDirectory(projectDir).connect()

        try {
            BuildLauncher builder = connection.newBuild()
            builder.forTasks(tasks).run()
            return connection.getModel(GradleProject)
        }
        finally {
            connection?.close()
        }
    }
}