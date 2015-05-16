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
package com.bmuschko.gradle.nexus

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.maven.MavenDeployment
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.Upload
import org.gradle.plugins.signing.SigningPlugin

/**
 * <p>A {@link Plugin} that provides task for configuring and uploading artifacts to Sonatype Nexus.</p>
 *
 * @author Benjamin Muschko
 */
class NexusPlugin implements Plugin<Project> {
    static final String NEXUS_USERNAME = 'nexusUsername'
    static final String NEXUS_PASSWORD = 'nexusPassword'
    static final String SIGNING_KEY_ID = 'signing.keyId'
    static final String SIGNING_KEYRING = 'signing.secretKeyRingFile'
    static final String SIGNING_PASSWORD = 'signing.password'

    @Override
    void apply(Project project) {
        project.plugins.apply(ExtraArchivePlugin)
        project.plugins.apply(MavenPlugin)
        project.plugins.apply(SigningPlugin)

        NexusPluginExtension extension = project.extensions.create('nexus', NexusPluginExtension)

        configureTasks(project, extension)
        configureSigning(project, extension)
        configurePom(project, extension)
        configureUpload(project, extension)
    }

    private void configureTasks(Project project, NexusPluginExtension extension) {
        project.afterEvaluate {
            changeInstallTaskConfiguration(project, extension)
            addArchiveTaskToOutgoingArtifacts(project, extension, ExtraArchivePlugin.SOURCES_JAR_TASK_NAME)
            addArchiveTaskToOutgoingArtifacts(project, extension, ExtraArchivePlugin.TESTS_JAR_TASK_NAME)
            addArchiveTaskToOutgoingArtifacts(project, extension, ExtraArchivePlugin.JAVADOC_JAR_TASK_NAME)
        }
    }

    private void changeInstallTaskConfiguration(Project project, NexusPluginExtension extension) {
        if(!extension.usesStandardConfiguration()) {
            project.tasks.getByName(MavenPlugin.INSTALL_TASK_NAME).configuration = project.configurations[extension.configuration]
        }
    }

    private void addArchiveTaskToOutgoingArtifacts(Project project, NexusPluginExtension extension, String taskName) {
        Task archiveTask = project.tasks.findByName(taskName)

        if(archiveTask) {
            project.artifacts.add(extension.configuration, archiveTask)
        }
    }

    private void configureSigning(Project project, NexusPluginExtension extension) {
        project.afterEvaluate {
            if(extension.sign) {
                project.signing {
                    required {
                        // Gradle allows project.version to be of type Object and always uses the toString() representation.
                        project.gradle.taskGraph.hasTask(extension.getUploadTaskPath(project)) && !project.version.toString().endsWith('SNAPSHOT')
                    }

                    sign project.configurations[extension.configuration]

                    project.gradle.taskGraph.whenReady {
                        if(project.signing.required) {
                            getPrivateKeyForSigning(project)
                        }

                        signPomForUpload(project, extension)
                        signInstallPom(project, extension)
                    }
                }
            }
        }
    }

    private void getPrivateKeyForSigning(Project project) {
        if (!project.hasProperty(SIGNING_KEY_ID)) {
            throw new GradleException("A GnuPG key ID is required for signing. Please set $SIGNING_KEY_ID=xxxxxxxx in <USER_HOME>/.gradle/gradle.properties.")
        }

        String signingKeyId = project.property(SIGNING_KEY_ID)

        File keyringFile = project.hasProperty(SIGNING_KEYRING) ?
                           project.file(project.property(SIGNING_KEYRING)) :
                           new File(new File(System.getProperty('user.home'), '.gnupg'), 'secring.gpg')

        if(keyringFile.exists()) {
            project.ext.set(SIGNING_KEYRING, keyringFile.getPath())
        }
        else {
            throw new GradleException("GnuPG secret key file $keyringFile not found. Please set $SIGNING_KEYRING=/path/to/file.gpg in <USER_HOME>/.gradle/gradle.properties.")
        }

        ConsoleHandler console = new ConsoleHandler()
        console.printf "\nThis release $project.version will be signed with your GnuPG key $signingKeyId in $keyringFile.\n"

        if (!project.hasProperty(SIGNING_PASSWORD)) {
            String password = console.askForPassword('Please enter your passphrase to unlock the secret key')
            project.ext.set(SIGNING_PASSWORD, password)
        }
    }

    private void signPomForUpload(Project project, NexusPluginExtension extension) {
        def uploadTasks = project.tasks.withType(Upload).matching { it.path == extension.getUploadTaskPath(project) }

        uploadTasks.each { task ->
            task.repositories.mavenDeployer() {
                beforeDeployment { MavenDeployment deployment ->
                    project.signing.signPom(deployment)
                }
            }
        }
    }

    private void signInstallPom(Project project, NexusPluginExtension extension) {
        def installTasks = project.tasks.withType(Upload).matching { it.path == extension.getInstallTaskPath(project) }

        installTasks.each { task ->
            task.repositories.mavenInstaller() {
                beforeDeployment { MavenDeployment deployment ->
                    project.signing.signPom(deployment)
                }
            }
        }
    }

    private void configurePom(Project project, NexusPluginExtension extension) {
        project.ext.modifyPom = { Closure modification ->
            project.afterEvaluate {
                project.poms.each {
                    it.whenConfigured { project.configure(it, modification) }
                }
            }
        }

        createPomsProjectProperty(project, extension)
    }

    private void createPomsProjectProperty(Project project, NexusPluginExtension extension) {
        project.afterEvaluate {
            project.ext.poms = []
            Task installTask = project.tasks.findByPath(MavenPlugin.INSTALL_TASK_NAME)

            if (installTask) {
                project.ext.poms << installTask.repositories.mavenInstaller().pom
            }

            project.ext.poms << project.tasks.getByName(extension.uploadTaskName).repositories.mavenDeployer().pom
        }
    }

    private void configureUpload(Project project, NexusPluginExtension extension) {
        project.afterEvaluate {
            project.tasks.getByName(extension.uploadTaskName).repositories.mavenDeployer() {
                project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                    if(taskGraph.hasTask(extension.getUploadTaskPath(project))) {
                        ConsoleHandler consoleHandler = new ConsoleHandler()

                        String nexusUsername = project.hasProperty(NEXUS_USERNAME) ?
                                               project.property(NEXUS_USERNAME) :
                                               consoleHandler.askForUsername('Please enter your Nexus username')

                        String nexusPassword = project.hasProperty(NEXUS_PASSWORD) ?
                                               project.property(NEXUS_PASSWORD) :
                                               consoleHandler.askForPassword('Please enter your Nexus password')

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
    }

    private class ConsoleHandler {
        Console console

        ConsoleHandler() {
            console = System.console()
        }

        void printf(String string, Object... args) {
            if (console) {
                console.printf(string, args)
            }
        }

        String askForUsername(String promptMessage) {
            console ? console.readLine("\n${promptMessage}: ") : null
        }

        String askForPassword(String promptMessage) {
            console ? new String(console.readPassword("\n${promptMessage}: ")) : null
        }
    }
}