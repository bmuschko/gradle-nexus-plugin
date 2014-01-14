/*
 * Copyright 2014 the original author or authors.
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

import org.gradle.tooling.model.GradleProject

import static org.spockframework.util.Assert.fail

/**
 * Nexus plugin upload task integration tests for multi-module projects.
 *
 * @author Benjamin Muschko
 * @author Dirk Moebius
 */
class NexusPluginUploadMultiModuleIntegrationTest extends AbstractIntegrationTest {
    def "Uploads all configured JARs, metadata and signature artifacts for release version with default configuration"() {
        setup:
        def moduleName = 'submodule1'
        File submoduleDir = new File(integTestDir, moduleName)
        if (!submoduleDir.mkdirs()) {
            fail('Unable to create integration submodule test directory.')
        }

        File settingsFile = new File(integTestDir, "settings.gradle")
        if (!settingsFile.createNewFile()) {
            fail('Unable to create settings file')
        }
        settingsFile << """
include ':$moduleName'
"""

        when:
        buildFile << """
allprojects {
    apply plugin: 'java'
    apply plugin: org.gradle.api.plugins.nexus.NexusPlugin

    version = '1.0'
    group = 'org.gradle.mygroup'

    nexus {
        attachTests = true
        repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    }
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadArchives')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup')
        File module1Dir = new File(repoDir, 'integTest/1.0')
        def expectedFilenames1 = ["${project.name}-1.0.jar", "${project.name}-1.0.jar.asc", "${project.name}-1.0.pom",
                "${project.name}-1.0.pom.asc", "${project.name}-1.0-javadoc.jar", "${project.name}-1.0-javadoc.jar.asc",
                "${project.name}-1.0-sources.jar", "${project.name}-1.0-sources.jar.asc", "${project.name}-1.0-tests.jar",
                "${project.name}-1.0-tests.jar.asc"]
        assertExistingFiles(module1Dir, expectedFilenames1)
        File module2Dir = new File(repoDir, "$moduleName/1.0")
        def expectedFilenames2 = ["${moduleName}-1.0.jar", "${moduleName}-1.0.jar.asc", "${moduleName}-1.0.pom",
                "${moduleName}-1.0.pom.asc", "${moduleName}-1.0-javadoc.jar", "${moduleName}-1.0-javadoc.jar.asc",
                "${moduleName}-1.0-sources.jar", "${moduleName}-1.0-sources.jar.asc", "${moduleName}-1.0-tests.jar",
                "${moduleName}-1.0-tests.jar.asc"]
        assertExistingFiles(module2Dir, expectedFilenames2)
    }

}
