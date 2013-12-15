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

import static org.spockframework.util.Assert.fail

/**
 * Nexus plugin install task integration tests.
 *
 * @author Benjamin Muschko
 */
class NexusPluginInstallIntegrationTest extends AbstractIntegrationTest {
    final static M2_HOME_DIR = new File(System.properties['user.home'], '.m2/repository')

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
}
