package org.gradle.api.plugins.nexus

import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Ignore
import spock.lang.Specification

class NexusPluginSpec extends Specification {

    def "nexus plugin with java adds javadocJar and sourcesJar tasks"() {
        setup:
        Project project = ProjectBuilder.builder().build()

        project.with {
            apply plugin: 'java'
            apply plugin: 'nexus'
        }

        when:
        project.evaluate()

        then:
        project.tasks["javadocJar"] instanceof Jar
        project.tasks["sourcesJar"] instanceof Jar
    }

    def "nexus plugin with groovy adds javadocJar and sourcesJar tasks"() {
        setup:
        Project project = ProjectBuilder.builder().build()

        project.with {
            apply plugin: 'groovy'
            apply plugin: 'nexus'
        }

        when:
        project.evaluate()

        then:
        project.tasks["javadocJar"] instanceof Jar
        project.tasks["sourcesJar"] instanceof Jar
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
