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
import org.gradle.api.artifacts.maven.MavenDeployment
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.plugins.BasePlugin
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
    static final String JAR_TASK_GROUP = BasePlugin.BUILD_GROUP
    static final String NEXUS_USERNAME = 'nexusUsername'
    static final String NEXUS_PASSWORD = 'nexusPassword'
    static final String SIGNING_KEY_ID = 'signing.keyId'
    static final String SIGNING_KEYRING = 'signing.secretKeyRingFile'
    static final String SIGNING_PASSWORD = 'signing.password'

    @Override
    void apply(Project project) {
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
            configureSourcesJarTask(project, extension)
            configureTestsJarTask(project, extension)
            configureJavadocJarTask(project, extension)
        }
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
        project.afterEvaluate {
            if(extension.sign) {
                project.signing {
                    required {
                        project.gradle.taskGraph.hasTask(extension.getUploadTaskPath(project)) && !project.version.endsWith('SNAPSHOT')
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

        Console console = System.console()
        console.printf "\nThis release $project.version will be signed with your GnuPG key $signingKeyId in $keyringFile.\n"

        if(!project.hasProperty(SIGNING_PASSWORD)) {
            String password = new String(console.readPassword('Please enter your passphrase to unlock the secret key: '))
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

        project.afterEvaluate {
            project.ext.poms = [project.tasks.getByName(MavenPlugin.INSTALL_TASK_NAME).repositories.mavenInstaller(),
                                project.tasks.getByName(extension.uploadTaskName).repositories.mavenDeployer()]*.pom
        }
    }

    private void configureUpload(Project project, NexusPluginExtension extension) {
        project.afterEvaluate {
            project.tasks.getByName(extension.uploadTaskName).repositories.mavenDeployer() {
                project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                    if(taskGraph.hasTask(extension.getUploadTaskPath(project))) {
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
    }

    /**
     * Checks to see if Java plugin got applied to project.
     *
     * @param project Project
     * @return Flag
     */
    private boolean hasJavaPlugin(Project project) {
        hasPlugin(project, JavaPlugin)
    }

    /**
     * Checks to see if Groovy plugin got applied to project.
     *
     * @param project Project
     * @return Flag
     */
    private boolean hasGroovyPlugin(Project project) {
        hasPlugin(project, GroovyPlugin)
    }

    private boolean hasPlugin(Project project, Class<? extends Plugin> pluginClass) {
        project.plugins.hasPlugin(pluginClass)
    }
}