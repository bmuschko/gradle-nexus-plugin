package org.gradle.api.plugins.nexus
/**
 * Multi-project integration test.
 *
 * @author Benjamin Muschko
 */
class MultiProjectBuildIntegrationTest extends AbstractIntegrationTest {
    File settingsFile
    List<String> subprojects = ['subproject1', 'subproject2', 'subproject3']

    def setup() {
        settingsFile = createNewFile(integTestDir, 'settings.gradle')

        buildFile << """
subprojects {
    apply plugin: 'java'
    apply plugin: org.gradle.api.plugins.nexus.NexusPlugin
}
"""
        subprojects.each { subproject ->
            settingsFile << "include '$subproject'\n"
            createNewDir(integTestDir, subproject)
        }
    }
}
