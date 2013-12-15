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

import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.GradleProject
import spock.lang.Specification

import static org.spockframework.util.Assert.fail

/**
 * Abstract integregration test using Gradle's tooling API.
 *
 * @author Benjamin Muschko
 */
abstract class AbstractIntegrationTest extends Specification {
    File integTestDir
    File buildFile

    def setup() {
        integTestDir = new File('build/integTest')

        if(!integTestDir.deleteDir()) {
            fail('Unable to delete integration test directory.')
        }

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
    }

    protected void assertExistingFiles(File dir, List<String> requiredFilenames) {
        assertExistingDirectory(dir)
        def dirFileNames = dir.listFiles()*.name

        requiredFilenames.each { filename ->
            assert dirFileNames.find { it ==~ filename }
        }
    }

    protected void assertNoSignatureFiles(File dir) {
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

    protected GradleProject runTasks(File projectDir, String... tasks) {
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
