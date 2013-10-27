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
    static final String JAR_TASK_GROUP = 'Build'
    static final String UPLOAD_ARCHIVES_TASK_NAME = 'uploadArchives'
    static final String UPLOAD_ARCHIVES_TASK_GRAPH_NAME = ":$UPLOAD_ARCHIVES_TASK_NAME"
    static final String ARCHIVES_CONFIGURATION_NAME = 'archives'
    static final String NEXUS_USERNAME = 'nexusUsername'
    static final String NEXUS_PASSWORD = 'nexusPassword'

    @Override
    void apply(Project project) {
        project.plugins.apply(MavenPlugin)
        project.plugins.apply(SigningPlugin)

        NexusPluginExtension extension = project.extensions.create('nexus', NexusPluginExtension)

        configureTasks(project, extension)
        configureSigning(project, extension)
        configurePom(project)
        configureUpload(project, extension)
    }

    private void configureTasks(Project project, NexusPluginExtension extension) {
        project.afterEvaluate {
            configureSourcesJarTask(project, extension)
            configureTestsJarTask(project, extension)
            configureJavadocJarTask(project, extension)
        }
    }

    private void configureSourcesJarTask(Project project, NexusPluginExtension extension) {
        if(extension.attachSources) {
            Jar sourcesJarTask = project.task('sourcesJar', type: Jar)
            sourcesJarTask.classifier = 'sources'
            sourcesJarTask.group = JAR_TASK_GROUP
            sourcesJarTask.description = 'Assembles a jar archive containing the main sources of this project.'
            sourcesJarTask.from project.sourceSets.main.allSource
            project.artifacts.add(ARCHIVES_CONFIGURATION_NAME, project.sourcesJar)
        }
    }

    private void configureTestsJarTask(Project project, NexusPluginExtension extension) {
        if(extension.attachTests) {
            Jar testsJarTask = project.task('testsJar', type: Jar)
            testsJarTask.classifier = 'tests'
            testsJarTask.group = JAR_TASK_GROUP
            testsJarTask.description = 'Assembles a jar archive containing the test sources of this project.'
            testsJarTask.from project.sourceSets.test.output
            project.artifacts.add(ARCHIVES_CONFIGURATION_NAME, project.testsJar)
        }
    }

    private void configureJavadocJarTask(Project project, NexusPluginExtension extension) {
        if(extension.attachJavadoc) {
            Jar javaDocJarTask = project.task('javadocJar', type: Jar)
            javaDocJarTask.classifier = 'javadoc'
            javaDocJarTask.group = JAR_TASK_GROUP
            javaDocJarTask.description = 'Assembles a jar archive containing the generated Javadoc API documentation of this project.'

            if(hasGroovyPlugin(project)) {
                javaDocJarTask.from project.groovydoc
            }
            else if(hasJavaPlugin(project)) {
                javaDocJarTask.from project.javadoc
            }

            project.artifacts.add(ARCHIVES_CONFIGURATION_NAME, project.javadocJar)
        }
    }

    private void configureSigning(Project project, NexusPluginExtension extension) {
        project.afterEvaluate {
            if(extension.sign) {
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

    private void configureUpload(Project project, NexusPluginExtension extension) {
        project.tasks.getByName(UPLOAD_ARCHIVES_TASK_NAME).repositories.mavenDeployer() {
            project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                if(taskGraph.hasTask(UPLOAD_ARCHIVES_TASK_GRAPH_NAME)) {
                    Console console = System.console()

                    String nexusUsername = project.hasProperty(NEXUS_USERNAME) ?
                                           project.property(NEXUS_USERNAME) :
                                           console.readLine('\nPlease specify Nexus username: ')

                    String nexusPassword = project.hasProperty(NEXUS_PASSWORD) ?
                                           project.property(NEXUS_PASSWORD) :
                                           new String(console.readPassword('\nPlease specify Nexus password: '))

                    if(extension.repositoryUrl) {
                        repository(url: extension.repositoryUrl) {
                            authentication(userName: nexusUsername, password: nexusPassword)
                        }
                    }

                    if(extension.snapshotRepositoryUrl) {
                        snapshotRepository(url: extension.snapshotRepositoryUrl) {
                            authentication(userName: nexusUsername, password: nexusPassword)
                        }
                    }
                }
            }
        }
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