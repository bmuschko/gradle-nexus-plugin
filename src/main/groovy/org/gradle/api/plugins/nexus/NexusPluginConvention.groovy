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

/**
 * Defines Sonatype Nexus plugin convention.
 *
 * @author Benjamin Muschko
 */
class NexusPluginConvention {
    Boolean sign = true
    Boolean attachSources = false
    Boolean attachTests = false
    Boolean attachJavadoc = false
    String repositoryUrl = 'https://oss.sonatype.org/service/local/staging/deploy/maven2/'
    String snapshotRepositoryUrl = 'https://oss.sonatype.org/content/repositories/snapshots/'

    def nexus(Closure closure) {
        closure.delegate = this
        closure()
    }
}
