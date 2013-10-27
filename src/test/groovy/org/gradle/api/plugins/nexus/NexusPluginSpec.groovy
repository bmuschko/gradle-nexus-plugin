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

import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Ignore
import spock.lang.Specification

class NexusPluginSpec extends Specification {
    def "nexus plugin with java adds javadocJar and sourcesJar tasks"() {
        when:
        Project project = ProjectBuilder.builder().build()

        project.with {
            apply plugin: 'java'
            apply plugin: 'nexus'
        }

        then:
        assertSourcesJar(project)
        assertJavadocJar(project)
    }

    def "nexus plugin with groovy adds javadocJar and sourcesJar tasks"() {
        when:
        Project project = ProjectBuilder.builder().build()

        project.with {
            apply plugin: 'groovy'
            apply plugin: 'nexus'
        }

        then:
        assertSourcesJar(project)
        assertJavadocJar(project)
    }

    //I've got a question in to help figure out this problem -aaron.klor
    //http://forums.gradle.org/gradle/topics/how_can_i_trigger_the_afterevaluate_lifecycle_event_from_testcase
    @Ignore("The taskgraph is not populated at the right time and causes an exception to be thrown.")
    def "nexus plugin intends to sign artifacts if not SNAPSHOT"() {
        setup:
        Project project = ProjectBuilder.builder().build()

        project.with {
            apply plugin: 'java'
            apply plugin: 'nexus'
            project.version = "test.version"
        }

        when:
        project.evaluate()

        then:
        true == project.signing.required
    }

    private void assertSourcesJar(Project project) {
        assert project.tasks.getByName('sourcesJar') != null
        assert project.tasks.getByName('sourcesJar') instanceof Jar
        Jar sourcesJar =  project.tasks.getByName('sourcesJar')
        assert sourcesJar.classifier == 'sources'
        assert sourcesJar.group == NexusPlugin.JAR_TASK_GROUP
        assert sourcesJar.description == 'Assembles a jar archive containing the main sources of this project.'
    }

    private void assertJavadocJar(Project project) {
        assert project.tasks.getByName('javadocJar') != null
        assert project.tasks.getByName('javadocJar') instanceof Jar
        Jar javadocJar =  project.tasks.getByName('javadocJar')
        assert javadocJar.classifier == 'javadoc'
        assert javadocJar.group == NexusPlugin.JAR_TASK_GROUP
        assert javadocJar.description == 'Assembles a jar archive containing the generated javadoc of this project.'
    }

    @Ignore("The taskgraph is not populated at the right time and causes an exception to be thrown.")
    def "nexus plugin intends to not sign artifacts if SNAPSHOT"() {
        setup:
        Project project = ProjectBuilder.builder().build()

        project.with {
            apply plugin: 'java'
            apply plugin: 'nexus'
            project.version = "test.version-SNAPSHOT"
        }

        when:
        project.evaluate()

        then:
        false == project.signing.required
    }
}
