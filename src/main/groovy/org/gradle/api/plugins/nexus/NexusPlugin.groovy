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

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.SigningPlugin

/**
 * <p>A {@link Plugin} that provides task for configuring and uploading artifacts to Sonatype Nexus.</p>
 *
 * @author Benjamin Muschko
 */
class NexusPlugin implements Plugin<Project> {
    static final String SOURCES_JAR_TASK_NAME = 'sourcesJar'
    static final String TESTS_JAR_TASK_NAME = 'testsJar'
    static final String JAVADOC_JAR_TASK_NAME = 'javadocJar'
    static final String UPLOAD_ARCHIVES_TASK_NAME = 'uploadArchives'
    static final String UPLOAD_ARCHIVES_TASK_GRAPH_NAME = ":$UPLOAD_ARCHIVES_TASK_NAME"
    static final String ARCHIVES_CONFIGURATION_NAME = 'archives'
    static final String NEXUS_USERNAME = 'nexusUsername'
    static final String NEXUS_PASSWORD = 'nexusPassword'

    @Override
    void apply(Project project) {
        project.plugins.apply(MavenPlugin)
        project.plugins.apply(SigningPlugin)

        NexusPluginConvention nexusPluginConvention = new NexusPluginConvention()
        project.convention.plugins.nexus = nexusPluginConvention

        configureSourcesJarTask(project, nexusPluginConvention)
        configureTestsJarTask(project, nexusPluginConvention)
        configureJavadocJarTask(project, nexusPluginConvention)
        configureSigning(project, nexusPluginConvention)
        configurePom(project)
        configureUpload(project, nexusPluginConvention)
    }

    private void configureSourcesJarTask(Project project, NexusPluginConvention nexusPluginConvention) {
        if(nexusPluginConvention.attachSources) {
            Jar sourcesJarTask = project.tasks.add(SOURCES_JAR_TASK_NAME, Jar)
            sourcesJarTask.classifier = 'sources'
            sourcesJarTask.from project.sourceSets.main.allSource
            project.artifacts.add(ARCHIVES_CONFIGURATION_NAME, project.sourcesJar)
        }
    }

    private void configureTestsJarTask(Project project, NexusPluginConvention nexusPluginConvention) {
        if(nexusPluginConvention.attachTests) {
            Jar testsJarTask = project.tasks.add(TESTS_JAR_TASK_NAME, Jar)
            testsJarTask.classifier = 'tests'
            testsJarTask.from project.sourceSets.test.output
            project.artifacts.add(ARCHIVES_CONFIGURATION_NAME, project.testsJar)
        }
    }

    private void configureJavadocJarTask(Project project, NexusPluginConvention nexusPluginConvention) {
        if(nexusPluginConvention.attachJavadoc) {
            Jar javadocJarTask = project.tasks.add(JAVADOC_JAR_TASK_NAME, Jar)
            javadocJarTask.classifier = 'javadoc'

            if(hasGroovyPlugin(project)) {
                javadocJarTask.from project.groovydoc
            }
            else if(hasJavaPlugin(project)) {
                javadocJarTask.from project.javadoc
            }
            project.artifacts.add(ARCHIVES_CONFIGURATION_NAME, project.javadocJar)
        }
    }

    private void configureSigning(Project project, NexusPluginConvention nexusPluginConvention) {
        project.afterEvaluate {
            if(nexusPluginConvention.sign) {
                project.signing {
                    required {
                        project.gradle.taskGraph.hasTask(UPLOAD_ARCHIVES_TASK_GRAPH_NAME) && !project.version.endsWith('SNAPSHOT')
                    }

                    sign project.configurations.archives

                    project.tasks.withType(Upload) {
                        project.tasks.getByName(UPLOAD_ARCHIVES_TASK_NAME).repositories.mavenDeployer() {
                            beforeDeployment {
                                signPom(it)
                            }
                        }
                    }
                }
            }
        }
    }

    private void configurePom(Project project) {
        project.ext.modifyPom = { Closure modification ->
            project.poms.each {
                it.whenConfigured { project.configure(it, modification) }
            }
        }

        project.ext.poms = [project.tasks.getByName(MavenPlugin.INSTALL_TASK_NAME).repositories.mavenInstaller(),
                            project.tasks.getByName(UPLOAD_ARCHIVES_TASK_NAME).repositories.mavenDeployer()]*.pom
    }

    private void configureUpload(Project project, NexusPluginConvention nexusPluginConvention) {
        project.tasks.getByName(UPLOAD_ARCHIVES_TASK_NAME).repositories.mavenDeployer() {
            project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                if(taskGraph.hasTask(UPLOAD_ARCHIVES_TASK_GRAPH_NAME)) {
                    if(!hasNexusCredentials(project)) {
                        throw new InvalidUserDataException("You are trying to upload and do not have credentials set. Please set '$NEXUS_USERNAME' and '$NEXUS_PASSWORD'!")
                    }

                    String nexusUsername = project.property(NEXUS_USERNAME)
                    String nexusPassword = project.property(NEXUS_PASSWORD)

                    if(nexusPluginConvention.repositoryUrl) {
                        repository(url: nexusPluginConvention.repositoryUrl) {
                            authentication(userName: nexusUsername, password: nexusPassword)
                        }
                    }

                    if(nexusPluginConvention.snapshotRepositoryUrl) {
                        snapshotRepository(url: nexusPluginConvention.snapshotRepositoryUrl) {
                            authentication(userName: nexusUsername, password: nexusPassword)
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if Sonatype username and password have been set.
     *
     * @param project Project
     * @return Flag
     */
    private boolean hasNexusCredentials(Project project) {
        project.hasProperty(NEXUS_USERNAME) && project.hasProperty(NEXUS_PASSWORD)
    }

    /**
     * Checks to see if Java plugin got applied to project.
     *
     * @param project Project
     * @return Flag
     */
    private boolean hasJavaPlugin(Project project) {
        project.plugins.hasPlugin(JavaPlugin)
    }

    /**
     * Checks to see if Groovy plugin got applied to project.
     *
     * @param project Project
     * @return Flag
     */
    private boolean hasGroovyPlugin(Project project) {
        project.plugins.hasPlugin(GroovyPlugin)
    }
}