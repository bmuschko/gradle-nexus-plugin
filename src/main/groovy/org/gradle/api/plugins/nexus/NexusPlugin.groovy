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
import org.gradle.api.artifacts.maven.MavenDeployment
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
    static final String NEXUS_USERNAME = 'nexusUsername'
    static final String NEXUS_PASSWORD = 'nexusPassword'

    @Override
    void apply(Project project) {
        project.plugins.apply(MavenPlugin)
        project.plugins.apply(SigningPlugin)

        NexusPluginExtension extension = project.extensions.create('nexus', NexusPluginExtension)

        project.afterEvaluate {
            configureTasks(project, extension)
            configureSigning(project, extension)
            configurePom(project)
            configureUpload(project, extension)
        }
    }

    private void configureTasks(Project project, NexusPluginExtension extension) {
        changeInstallTaskConfiguration(project, extension)
        configureSourcesJarTask(project, extension)
        configureTestsJarTask(project, extension)
        configureJavadocJarTask(project, extension)
    }

    private void changeInstallTaskConfiguration(Project project, NexusPluginExtension extension) {
        if(!extension.usesStandardConfiguration()) {
            project.tasks.getByName(MavenPlugin.INSTALL_TASK_NAME).configuration = project.configurations[extension.configuration]
        }
    }

    private void configureSourcesJarTask(Project project, NexusPluginExtension extension) {
        if(extension.attachSources) {
            Jar sourcesJarTask = project.task('sourcesJar', type: Jar) {
                classifier = 'sources'
                group = JAR_TASK_GROUP
                description = 'Assembles a jar archive containing the main sources of this project.'
                from project.sourceSets.main.allSource
            }

            project.artifacts.add(extension.configuration, sourcesJarTask)
        }
    }

    private void configureTestsJarTask(Project project, NexusPluginExtension extension) {
        if(extension.attachTests) {
            Jar testsJarTask = project.task('testsJar', type: Jar) {
                classifier = 'tests'
                group = JAR_TASK_GROUP
                description = 'Assembles a jar archive containing the test sources of this project.'
                from project.sourceSets.test.output
            }

            project.artifacts.add(extension.configuration, testsJarTask)
        }
    }

    private void configureJavadocJarTask(Project project, NexusPluginExtension extension) {
        if(extension.attachJavadoc) {
            Jar javaDocJarTask = project.task('javadocJar', type: Jar) {
                classifier = 'javadoc'
                group = JAR_TASK_GROUP
                description = 'Assembles a jar archive containing the generated Javadoc API documentation of this project.'
            }

            if(hasGroovyPlugin(project)) {
                javaDocJarTask.from project.groovydoc
            }
            else if(hasJavaPlugin(project)) {
                javaDocJarTask.from project.javadoc
            }

            project.artifacts.add(extension.configuration, javaDocJarTask)
        }
    }

    private void configureSigning(Project project, NexusPluginExtension extension) {
        if(extension.sign) {
            project.signing {
                required {
                    project.gradle.taskGraph.hasTask(extension.uploadTaskPath) && !project.version.endsWith('SNAPSHOT')
                }

                sign project.configurations[extension.configuration]


                project.gradle.taskGraph.whenReady {
                    signPomForUpload(project, extension)
                    signInstallPom(project)
                }
            }
        }
    }

    private void signPomForUpload(Project project, NexusPluginExtension extension) {
        def uploadTasks = project.tasks.withType(Upload).matching { it.path == extension.uploadTaskPath }

        uploadTasks.each { task ->
            task.repositories.mavenDeployer() {
                beforeDeployment { MavenDeployment deployment ->
                    project.signing.signPom(deployment)
                }
            }
        }
    }

    private void signInstallPom(Project project) {
        def installTasks = project.tasks.withType(Upload).matching { it.path == ":$MavenPlugin.INSTALL_TASK_NAME" }

        installTasks.each { task ->
            task.repositories.mavenInstaller() {
                beforeDeployment { MavenDeployment deployment ->
                    project.signing.signPom(deployment)
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
    }

    private void configureUpload(Project project, NexusPluginExtension extension) {
        project.tasks.getByName(extension.uploadTaskName).repositories.mavenDeployer() {
            project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                if(taskGraph.hasTask(extension.uploadTaskPath)) {
                    Console console = System.console()

                    String nexusUsername = project.hasProperty(NEXUS_USERNAME) ?
                                           project.property(NEXUS_USERNAME) :
                                           console.readLine('\nPlease specify username: ')

                    String nexusPassword = project.hasProperty(NEXUS_PASSWORD) ?
                                           project.property(NEXUS_PASSWORD) :
                                           new String(console.readPassword('\nPlease specify password: '))

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