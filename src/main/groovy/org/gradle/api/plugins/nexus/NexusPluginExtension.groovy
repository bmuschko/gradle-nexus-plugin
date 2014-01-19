/*
 * Copyright 2012 the original author or authors.
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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.MavenPlugin

/**
 * Defines Sonatype Nexus plugin convention.
 *
 * @author Benjamin Muschko
 */
class NexusPluginExtension {
    String configuration = Dependency.ARCHIVES_CONFIGURATION
    Boolean sign = true
    Boolean attachSources = true
    Boolean attachTests = false
    Boolean attachJavadoc = true
    String repositoryUrl = 'https://oss.sonatype.org/service/local/staging/deploy/maven2/'
    String snapshotRepositoryUrl = 'https://oss.sonatype.org/content/repositories/snapshots/'

    def nexus(Closure closure) {
        closure.delegate = this
        closure()
    }

    String getUploadTaskName() {
        "upload${configuration.capitalize()}"
    }

    String getUploadTaskPath(Project project) {
        isRootProject(project) ? ":$uploadTaskName" : "$project.path:$uploadTaskName"
    }

    String getInstallTaskPath(Project project) {
        isRootProject(project) ? ":$MavenPlugin.INSTALL_TASK_NAME" : "$project.path:$MavenPlugin.INSTALL_TASK_NAME"
    }

    private boolean isRootProject(Project project) {
        project.rootProject == project
    }

    void setConfiguration(config) {
        configuration = config instanceof Configuration ? config.name : config
    }

    boolean usesStandardConfiguration() {
        configuration == Dependency.ARCHIVES_CONFIGURATION
    }
}
