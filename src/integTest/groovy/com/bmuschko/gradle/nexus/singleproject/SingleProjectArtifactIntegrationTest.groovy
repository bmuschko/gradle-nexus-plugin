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
package com.bmuschko.gradle.nexus.singleproject

import org.gradle.tooling.model.GradleProject

/**
 * Nexus plugin artifact creation integration test.
 *
 * @author Benjamin Muschko
 */
class SingleProjectArtifactIntegrationTest extends SingleProjectBuildIntegrationTest {
    def setup() {
        buildFile << """
extraArchive {
    tests = true
}
"""
    }

    def "Creates all configured JARs for default configuration"() {
        when:
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
}
