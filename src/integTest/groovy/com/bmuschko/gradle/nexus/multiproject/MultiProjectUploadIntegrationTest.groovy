/*
 * Copyright 2014 the original author or authors.
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
package com.bmuschko.gradle.nexus.multiproject

/**
 * Nexus plugin upload task integration tests for multi-project builds.
 *
 * @author Benjamin Muschko
 * @author Dirk Moebius
 */
class MultiProjectUploadIntegrationTest extends MultiProjectBuildIntegrationTest {
    def "Uploads all configured JARs, metadata and signature artifacts for release version with default configuration"() {
        when:
        buildFile << """
allprojects {
    version = '1.0'
    group = 'org.gradle.mygroup'
}

subprojects {
    nexus {
        attachTests = true
        repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    }
}
"""
        runTasks(integTestDir, 'uploadArchives')

        then:
        subprojects.each { subproject ->
            File repoDir = new File(integTestDir, "repo/org/gradle/mygroup/$subproject/1.0")
            def expectedFilenames = ["${subproject}-1.0.jar", "${subproject}-1.0.jar.asc", "${subproject}-1.0.pom",
                                     "${subproject}-1.0.pom.asc", "${subproject}-1.0-javadoc.jar", "${subproject}-1.0-javadoc.jar.asc",
                                     "${subproject}-1.0-sources.jar", "${subproject}-1.0-sources.jar.asc", "${subproject}-1.0-tests.jar",
                                     "${subproject}-1.0-tests.jar.asc"]
            assertExistingFiles(repoDir, expectedFilenames)
        }
    }

    def "Uploads all configured JARs, customized metadata and signature artifacts with default configuration"() {
        when:
        buildFile << """
allprojects {
    version = '1.0'
    group = 'org.gradle.mygroup'
}

subprojects {
    nexus {
        attachTests = true
        repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    }

    modifyPom {
        project {
            name 'myapp'
            description 'My application'
            inceptionYear '2012'

            developers {
                developer {
                    id 'bmuschko'
                    name 'Benjamin Muschko'
                    email 'benjamin.muschko@gmail.com'
                }
            }
        }
    }
}
"""
        runTasks(integTestDir, 'uploadArchives')

        then:
        subprojects.each { subproject ->
            File repoDir = new File(integTestDir, "repo/org/gradle/mygroup/$subproject/1.0")
            def expectedFilenames = ["${subproject}-1.0.jar", "${subproject}-1.0.jar.asc", "${subproject}-1.0.pom",
                                     "${subproject}-1.0.pom.asc", "${subproject}-1.0-javadoc.jar", "${subproject}-1.0-javadoc.jar.asc",
                                     "${subproject}-1.0-sources.jar", "${subproject}-1.0-sources.jar.asc", "${subproject}-1.0-tests.jar",
                                     "${subproject}-1.0-tests.jar.asc"]
            assertExistingFiles(repoDir, expectedFilenames)
            assertCorrectPomXml(new File(repoDir, "${subproject}-1.0.pom"))
        }
    }

    def "Uploads all configured JARs, metadata and signature artifacts for release version with custom configuration"() {
        when:
        buildFile << """
allprojects {
    version = '1.0'
    group = 'org.gradle.mygroup'
}

subprojects {
    configurations {
        myConfig.extendsFrom signatures
    }

    artifacts {
        myConfig jar
    }

    nexus {
        attachTests = true
        repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
        configuration = configurations.myConfig
    }
}
"""
        runTasks(integTestDir, 'uploadMyConfig')

        then:
        subprojects.each { subproject ->
            File repoDir = new File(integTestDir, "repo/org/gradle/mygroup/$subproject/1.0")
            def expectedFilenames = ["${subproject}-1.0.jar", "${subproject}-1.0.jar.asc", "${subproject}-1.0.pom",
                                     "${subproject}-1.0.pom.asc", "${subproject}-1.0-javadoc.jar", "${subproject}-1.0-javadoc.jar.asc",
                                     "${subproject}-1.0-sources.jar", "${subproject}-1.0-sources.jar.asc", "${subproject}-1.0-tests.jar",
                                     "${subproject}-1.0-tests.jar.asc"]
            assertExistingFiles(repoDir, expectedFilenames)
        }
    }

    def "Uploads all configured JARs, customized metadata and signature artifacts with custom configuration"() {
        when:
        buildFile << """
allprojects {
    version = '1.0'
    group = 'org.gradle.mygroup'
}

subprojects {
    configurations {
        myConfig.extendsFrom signatures
    }

    artifacts {
        myConfig jar
    }

    nexus {
        attachTests = true
        repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
        configuration = configurations.myConfig
    }

    modifyPom {
        project {
            name 'myapp'
            description 'My application'
            inceptionYear '2012'

            developers {
                developer {
                    id 'bmuschko'
                    name 'Benjamin Muschko'
                    email 'benjamin.muschko@gmail.com'
                }
            }
        }
    }
}
"""
        runTasks(integTestDir, 'uploadMyConfig')

        then:
        subprojects.each { subproject ->
            File repoDir = new File(integTestDir, "repo/org/gradle/mygroup/$subproject/1.0")
            def expectedFilenames = ["${subproject}-1.0.jar", "${subproject}-1.0.jar.asc", "${subproject}-1.0.pom",
                                     "${subproject}-1.0.pom.asc", "${subproject}-1.0-javadoc.jar", "${subproject}-1.0-javadoc.jar.asc",
                                     "${subproject}-1.0-sources.jar", "${subproject}-1.0-sources.jar.asc", "${subproject}-1.0-tests.jar",
                                     "${subproject}-1.0-tests.jar.asc"]
            assertExistingFiles(repoDir, expectedFilenames)
            assertCorrectPomXml(new File(repoDir, "${subproject}-1.0.pom"))
        }
    }

    def "Uploads all configured JARs and metadata without signature artifacts for release version with default configuration"() {
        when:
        buildFile << """
allprojects {
    version = '1.0'
    group = 'org.gradle.mygroup'
}

subprojects {
    nexus {
        attachTests = true
        repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
        sign = false
    }
}
"""
        runTasks(integTestDir, 'uploadArchives')

        then:
        subprojects.each { subproject ->
            File repoDir = new File(integTestDir, "repo/org/gradle/mygroup/$subproject/1.0")
            def expectedFilenames = ["${subproject}-1.0.jar", "${subproject}-1.0.pom", "${subproject}-1.0-javadoc.jar",
                                     "${subproject}-1.0-sources.jar", "${subproject}-1.0-tests.jar"]
            assertExistingFiles(repoDir, expectedFilenames)
            assertNoSignatureFiles(repoDir)
        }
    }

    def "Uploads all configured JARs and metadata without signature artifacts for release version with custom configuration"() {
        when:
        buildFile << """
allprojects {
    version = '1.0'
    group = 'org.gradle.mygroup'
}

subprojects {
    configurations {
        myConfig.extendsFrom signatures
    }

    artifacts {
        myConfig jar
    }

    nexus {
        attachTests = true
        repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
        sign = false
        configuration = configurations.myConfig
    }
}
"""
        runTasks(integTestDir, 'uploadMyConfig')

        then:
        subprojects.each { subproject ->
            File repoDir = new File(integTestDir, "repo/org/gradle/mygroup/$subproject/1.0")
            def expectedFilenames = ["${subproject}-1.0.jar", "${subproject}-1.0.pom", "${subproject}-1.0-javadoc.jar",
                                     "${subproject}-1.0-sources.jar", "${subproject}-1.0-tests.jar"]
            assertExistingFiles(repoDir, expectedFilenames)
            assertNoSignatureFiles(repoDir)
        }
    }

    def "Uploads all configured JARs, metadata and signature artifacts for snapshot version with default configuration"() {
        when:
        buildFile << """
allprojects {
    version = '1.0-SNAPSHOT'
    group = 'org.gradle.mygroup'
}

subprojects {
    nexus {
        attachTests = true
        snapshotRepositoryUrl = 'file://$integTestDir.canonicalPath/repo'
    }
}
"""
        runTasks(integTestDir, 'uploadArchives')

        then:
        subprojects.each { subproject ->
            File repoDir = new File(integTestDir, "repo/org/gradle/mygroup/$subproject/1.0-SNAPSHOT")
            def expectedFilenames = ["${subproject}-1\\.0-\\d+\\.\\d+-1\\.jar", "${subproject}-1\\.0-\\d+\\.\\d+-1\\.jar.asc",
                                     "${subproject}-1\\.0-\\d+\\.\\d+-1\\.pom", "${subproject}-1\\.0-\\d+\\.\\d+-1\\.pom.asc",
                                     "${subproject}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar", "${subproject}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar.asc",
                                     "${subproject}-1\\.0-\\d+\\.\\d+-1\\-sources.jar", "${subproject}-1\\.0-\\d+\\.\\d+-1\\-sources.jar.asc",
                                     "${subproject}-1\\.0-\\d+\\.\\d+-1\\-tests.jar", "${subproject}-1\\.0-\\d+\\.\\d+-1\\-tests.jar.asc"]
            assertExistingFiles(repoDir, expectedFilenames)
        }
    }

    def "Uploads all configured JARs, metadata and signature artifacts for snapshot version with custom configuration"() {
        when:
        buildFile << """
allprojects {
    version = '1.0-SNAPSHOT'
    group = 'org.gradle.mygroup'
}

subprojects {
    configurations {
        myConfig.extendsFrom signatures
    }

    artifacts {
        myConfig jar
    }

    nexus {
        attachTests = true
        snapshotRepositoryUrl = 'file://$integTestDir.canonicalPath/repo'
        configuration = configurations.myConfig
    }
}
"""
        runTasks(integTestDir, 'uploadMyConfig')

        then:
        subprojects.each { subproject ->
            File repoDir = new File(integTestDir, "repo/org/gradle/mygroup/$subproject/1.0-SNAPSHOT")
            def expectedFilenames = ["${subproject}-1\\.0-\\d+\\.\\d+-1\\.jar", "${subproject}-1\\.0-\\d+\\.\\d+-1\\.jar.asc",
                                     "${subproject}-1\\.0-\\d+\\.\\d+-1\\.pom", "${subproject}-1\\.0-\\d+\\.\\d+-1\\.pom.asc",
                                     "${subproject}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar", "${subproject}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar.asc",
                                     "${subproject}-1\\.0-\\d+\\.\\d+-1\\-sources.jar", "${subproject}-1\\.0-\\d+\\.\\d+-1\\-sources.jar.asc",
                                     "${subproject}-1\\.0-\\d+\\.\\d+-1\\-tests.jar", "${subproject}-1\\.0-\\d+\\.\\d+-1\\-tests.jar.asc"]
            assertExistingFiles(repoDir, expectedFilenames)
        }
    }

    def "Uploads all configured JARs and metadata without signature artifacts for snapshot version with default configuration"() {
        when:
        buildFile << """
allprojects {
    version = '1.0-SNAPSHOT'
    group = 'org.gradle.mygroup'
}

subprojects {
    nexus {
        attachTests = true
        snapshotRepositoryUrl = 'file://$integTestDir.canonicalPath/repo'
        sign = false
    }
}
"""
        runTasks(integTestDir, 'uploadArchives')

        then:
        subprojects.each { subproject ->
            File repoDir = new File(integTestDir, "repo/org/gradle/mygroup/$subproject/1.0-SNAPSHOT")
            def expectedFilenames = ["${subproject}-1\\.0-\\d+\\.\\d+-1\\.jar", "${subproject}-1\\.0-\\d+\\.\\d+-1\\.pom",
                                     "${subproject}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar", "${subproject}-1\\.0-\\d+\\.\\d+-1\\-sources.jar",
                                     "${subproject}-1\\.0-\\d+\\.\\d+-1\\-tests.jar"]
            assertExistingFiles(repoDir, expectedFilenames)
            assertNoSignatureFiles(repoDir)
        }
    }

    def "Uploads all configured JARs and metadata without signature artifacts for snapshot version with custom configuration"() {
        when:
        buildFile << """
allprojects {
    version = '1.0-SNAPSHOT'
    group = 'org.gradle.mygroup'
}

subprojects {
    configurations {
        myConfig.extendsFrom signatures
    }

    artifacts {
        myConfig jar
    }

    nexus {
        attachTests = true
        snapshotRepositoryUrl = 'file://$integTestDir.canonicalPath/repo'
        sign = false
        configuration = configurations.myConfig
    }
}
"""
        runTasks(integTestDir, 'uploadMyConfig')

        then:
        subprojects.each { subproject ->
            File repoDir = new File(integTestDir, "repo/org/gradle/mygroup/$subproject/1.0-SNAPSHOT")
            def expectedFilenames = ["${subproject}-1\\.0-\\d+\\.\\d+-1\\.jar", "${subproject}-1\\.0-\\d+\\.\\d+-1\\.pom",
                                     "${subproject}-1\\.0-\\d+\\.\\d+-1\\-javadoc.jar", "${subproject}-1\\.0-\\d+\\.\\d+-1\\-sources.jar",
                                     "${subproject}-1\\.0-\\d+\\.\\d+-1\\-tests.jar"]
            assertExistingFiles(repoDir, expectedFilenames)
            assertNoSignatureFiles(repoDir)
        }
    }
}
