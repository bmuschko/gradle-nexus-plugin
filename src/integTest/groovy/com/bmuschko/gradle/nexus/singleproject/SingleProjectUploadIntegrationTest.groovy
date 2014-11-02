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
package com.bmuschko.gradle.nexus.singleproject

import org.gradle.tooling.model.GradleProject
import spock.lang.IgnoreIf
import spock.lang.Issue

import static com.bmuschko.gradle.nexus.AbstractIntegrationTest.hasSigningKey

/**
 * Nexus plugin upload task integration tests.
 *
 * @author Benjamin Muschko
 */
class SingleProjectUploadIntegrationTest extends SingleProjectBuildIntegrationTest {
    def setup() {
        buildFile << """
extraArchive {
    tests = true
}
"""
    }

    @IgnoreIf({ !hasSigningKey() })
    def "Uploads all configured JARs, metadata and signature artifacts for release version with default configuration"() {
        when:
        buildFile << """
version = '1.0'
group = 'org.gradle.mygroup'

nexus {
    repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadArchives')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0')
        def expectedFilenames = ["${project.name}-1.0.jar", "${project.name}-1.0.jar.asc", "${project.name}-1.0.pom",
                                 "${project.name}-1.0.pom.asc", "${project.name}-1.0-javadoc.jar", "${project.name}-1.0-javadoc.jar.asc",
                                 "${project.name}-1.0-sources.jar", "${project.name}-1.0-sources.jar.asc", "${project.name}-1.0-tests.jar",
                                 "${project.name}-1.0-tests.jar.asc"]
        assertExistingFiles(repoDir, expectedFilenames)
    }

    @IgnoreIf({ !hasSigningKey() })
    def "Uploads all configured JARs, customized metadata and signature artifacts with default configuration"() {
        when:
        buildFile << """
version = '1.0'
group = 'org.gradle.mygroup'

nexus {
    repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
}
"""
        buildFile << getDefaultPomMetaData()
        GradleProject project = runTasks(integTestDir, 'uploadArchives')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0')
        def expectedFilenames = ["${project.name}-1.0.jar", "${project.name}-1.0.jar.asc", "${project.name}-1.0.pom",
                                 "${project.name}-1.0.pom.asc", "${project.name}-1.0-javadoc.jar", "${project.name}-1.0-javadoc.jar.asc",
                                 "${project.name}-1.0-sources.jar", "${project.name}-1.0-sources.jar.asc", "${project.name}-1.0-tests.jar",
                                 "${project.name}-1.0-tests.jar.asc"]
        assertExistingFiles(repoDir, expectedFilenames)
        assertCorrectPomXml(new File(repoDir, "${project.name}-1.0.pom"))
    }

    @Issue("https://github.com/bmuschko/gradle-nexus-plugin/issues/32")
    def "Uploads all configured JARs and customized metadata for provided configuration"() {
        when:
        buildFile << """
version = '1.0'
group = 'org.gradle.mygroup'

configurations {
    provided
    compile.extendsFrom provided
}

repositories {
    mavenCentral()
}

dependencies {
    compile 'commons-lang:commons-lang:2.6'
    runtime 'mysql:mysql-connector-java:5.1.13'
    provided 'javax.servlet:javax.servlet-api:3.1.0'
}

nexus {
    sign = false
    repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
}

modifyPom {
    project {
        def generatedDeps = dependencies

        dependencies {
            generatedDeps.each { dep ->
                dependency {
                    groupId dep.groupId
                    artifactId dep.artifactId
                    version dep.version
                    scope dep.scope
                }
            }

            project.configurations.provided.allDependencies.each { dep ->
                dependency {
                    groupId dep.group
                    artifactId dep.name
                    version dep.version
                    scope 'provided'
                }
            }
        }
    }
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadArchives')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0')
        def expectedFilenames = ["${project.name}-1.0.jar", "${project.name}-1.0.pom", "${project.name}-1.0-javadoc.jar",
                                 "${project.name}-1.0-sources.jar", "${project.name}-1.0-tests.jar"]
        assertExistingFiles(repoDir, expectedFilenames)
        def pomXml = new XmlSlurper().parse(new File(repoDir, "${project.name}-1.0.pom"))
        assert pomXml.dependencies.dependency.size() == 3
        def mysqlDependency = pomXml.dependencies.dependency[0]
        assert mysqlDependency.groupId.text() == 'mysql'
        assert mysqlDependency.artifactId.text() == 'mysql-connector-java'
        assert mysqlDependency.version.text() == '5.1.13'
        assert mysqlDependency.scope.text() == 'runtime'
        def commonsLangDependency = pomXml.dependencies.dependency[1]
        assert commonsLangDependency.groupId.text() == 'commons-lang'
        assert commonsLangDependency.artifactId.text() == 'commons-lang'
        assert commonsLangDependency.version.text() == '2.6'
        assert commonsLangDependency.scope.text() == 'compile'
        def servletDependency = pomXml.dependencies.dependency[2]
        assert servletDependency.groupId.text() == 'javax.servlet'
        assert servletDependency.artifactId.text() == 'javax.servlet-api'
        assert servletDependency.version.text() == '3.1.0'
        assert servletDependency.scope.text() == 'provided'
    }

    @IgnoreIf({ !hasSigningKey() })
    def "Uploads all configured JARs, metadata and signature artifacts for release version with custom configuration"() {
        when:
        buildFile << """
version = '1.0'
group = 'org.gradle.mygroup'

configurations {
    myConfig.extendsFrom signatures
}

artifacts {
    myConfig jar
}

nexus {
    repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    configuration = configurations.myConfig
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadMyConfig')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0')
        def expectedFilenames = ["${project.name}-1.0.jar", "${project.name}-1.0.jar.asc", "${project.name}-1.0.pom",
                                 "${project.name}-1.0.pom.asc", "${project.name}-1.0-javadoc.jar", "${project.name}-1.0-javadoc.jar.asc",
                                 "${project.name}-1.0-sources.jar", "${project.name}-1.0-sources.jar.asc", "${project.name}-1.0-tests.jar",
                                 "${project.name}-1.0-tests.jar.asc"]
        assertExistingFiles(repoDir, expectedFilenames)
    }

    @IgnoreIf({ !hasSigningKey() })
    def "Uploads all configured JARs, customized metadata and signature artifacts with custom configuration"() {
        when:
        buildFile << """
version = '1.0'
group = 'org.gradle.mygroup'

configurations {
    myConfig.extendsFrom signatures
}

artifacts {
    myConfig jar
}

nexus {
    repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    configuration = configurations.myConfig
}
"""
        buildFile << getDefaultPomMetaData()
        GradleProject project = runTasks(integTestDir, 'uploadMyConfig')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0')
        def expectedFilenames = ["${project.name}-1.0.jar", "${project.name}-1.0.jar.asc", "${project.name}-1.0.pom",
                "${project.name}-1.0.pom.asc", "${project.name}-1.0-javadoc.jar", "${project.name}-1.0-javadoc.jar.asc",
                "${project.name}-1.0-sources.jar", "${project.name}-1.0-sources.jar.asc", "${project.name}-1.0-tests.jar",
                "${project.name}-1.0-tests.jar.asc"]
        assertExistingFiles(repoDir, expectedFilenames)
        assertCorrectPomXml(new File(repoDir, "${project.name}-1.0.pom"))
    }

    def "Uploads all configured JARs and metadata without signature artifacts for release version with default configuration"() {
        when:
        buildFile << """
version = '1.0'
group = 'org.gradle.mygroup'

nexus {
    repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    sign = false
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadArchives')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0')
        def expectedFilenames = ["${project.name}-1.0.jar", "${project.name}-1.0.pom", "${project.name}-1.0-javadoc.jar",
                                 "${project.name}-1.0-sources.jar", "${project.name}-1.0-tests.jar"]
        assertExistingFiles(repoDir, expectedFilenames)
        assertNoSignatureFiles(repoDir)
    }

    def "Uploads all configured JARs and metadata without signature artifacts for release version with custom configuration"() {
        when:
        buildFile << """
version = '1.0'
group = 'org.gradle.mygroup'

configurations {
    myConfig.extendsFrom signatures
}

artifacts {
    myConfig jar
}

nexus {
    repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    sign = false
    configuration = configurations.myConfig
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadMyConfig')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0')
        def expectedFilenames = ["${project.name}-1.0.jar", "${project.name}-1.0.pom", "${project.name}-1.0-javadoc.jar",
                                 "${project.name}-1.0-sources.jar", "${project.name}-1.0-tests.jar"]
        assertExistingFiles(repoDir, expectedFilenames)
        assertNoSignatureFiles(repoDir)
    }

    @IgnoreIf({ !hasSigningKey() })
    def "Uploads all configured JARs, metadata and signature artifacts for snapshot version with default configuration"() {
        when:
        buildFile << """
version = '1.0-SNAPSHOT'
group = 'org.gradle.mygroup'

nexus {
    snapshotRepositoryUrl = 'file://$integTestDir.canonicalPath/repo'
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadArchives')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0-SNAPSHOT')
        def expectedFilenames = ["${project.name}-1\\.0-\\d+\\.\\d+-1\\.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\.jar.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\.pom", "${project.name}-1\\.0-\\d+\\.\\d+-1\\.pom.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-sources.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-sources.jar.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-tests.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-tests.jar.asc"]
        assertExistingFiles(repoDir, expectedFilenames)
    }

    @IgnoreIf({ !hasSigningKey() })
    def "Uploads all configured JARs, metadata and signature artifacts for snapshot version with custom configuration"() {
        when:
        buildFile << """
version = '1.0-SNAPSHOT'
group = 'org.gradle.mygroup'

configurations {
    myConfig.extendsFrom signatures
}

artifacts {
    myConfig jar
}

nexus {
    snapshotRepositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    configuration = configurations.myConfig
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadMyConfig')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0-SNAPSHOT')
        def expectedFilenames = ["${project.name}-1\\.0-\\d+\\.\\d+-1\\.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\.jar.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\.pom", "${project.name}-1\\.0-\\d+\\.\\d+-1\\.pom.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-sources.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-sources.jar.asc",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-tests.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-tests.jar.asc"]
        assertExistingFiles(repoDir, expectedFilenames)
    }

    def "Uploads all configured JARs and metadata without signature artifacts for snapshot version with default configuration"() {
        when:
        buildFile << """
version = '1.0-SNAPSHOT'
group = 'org.gradle.mygroup'

nexus {
    snapshotRepositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    sign = false
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadArchives')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0-SNAPSHOT')
        def expectedFilenames = ["${project.name}-1\\.0-\\d+\\.\\d+-1\\.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\.pom",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-sources.jar",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-tests.jar"]
        assertExistingFiles(repoDir, expectedFilenames)
        assertNoSignatureFiles(repoDir)
    }

    def "Uploads all configured JARs and metadata without signature artifacts for snapshot version with custom configuration"() {
        when:
        buildFile << """
version = '1.0-SNAPSHOT'
group = 'org.gradle.mygroup'

configurations {
    myConfig.extendsFrom signatures
}

artifacts {
    myConfig jar
}

nexus {
    snapshotRepositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    sign = false
    configuration = configurations.myConfig
}
"""
        GradleProject project = runTasks(integTestDir, 'uploadMyConfig')

        then:
        File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0-SNAPSHOT')
        def expectedFilenames = ["${project.name}-1\\.0-\\d+\\.\\d+-1\\.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\.pom",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar", "${project.name}-1\\.0-\\d+\\.\\d+-1\\-sources.jar",
                                 "${project.name}-1\\.0-\\d+\\.\\d+-1\\-tests.jar"]
        assertExistingFiles(repoDir, expectedFilenames)
        assertNoSignatureFiles(repoDir)
    }
}
