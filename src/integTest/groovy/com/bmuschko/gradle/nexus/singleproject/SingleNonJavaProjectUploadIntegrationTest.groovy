package com.bmuschko.gradle.nexus.singleproject

import org.gradle.tooling.model.GradleProject

import spock.lang.Issue

import com.bmuschko.gradle.nexus.AbstractIntegrationTest

/**
 * 
 * Integration test for nexus gradle plugin that checks if upload
 * task works as expected when executed against project without java plugin applied.
 * 
 * @author orzeh
 *
 */
class SingleNonJavaProjectUploadIntegrationTest extends AbstractIntegrationTest {

	def setup() {
		buildFile << """
apply plugin: com.bmuschko.gradle.nexus.NexusPlugin

extraArchive {
    tests = false
	sources = false
	javadoc = false
}
"""
	}

	@Issue('https://github.com/bmuschko/gradle-nexus-plugin/issues/44')
	def "Uploads EAR to repository when applied to project with ear plugin"() {
		when:
		buildFile << """
version = '1.0'
group = 'org.gradle.mygroup'

apply plugin: 'ear'

repositories { mavenCentral() }

dependencies {
    earlib group: 'log4j', name: 'log4j', version: '1.2.15', ext: 'jar'
}

ear {
    appDirName 'src/main/app'
    libDirName 'APP-INF/lib'
}

nexus {
	sign = false
    repositoryUrl = 'file://$integTestDir.canonicalPath/repo'
}
"""
		GradleProject project = runTasks(integTestDir, 'uploadArchives')

		then:
		File repoDir = new File(integTestDir, 'repo/org/gradle/mygroup/integTest/1.0')
		def expectedFilenames = ["${project.name}-1.0.ear", "${project.name}-1.0.pom"]
		assertExistingFiles(repoDir, expectedFilenames)
	}
}
