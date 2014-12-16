package com.ullink

import static org.junit.Assert.*
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class RepositoriesPluginTest {
    @Test
    public void repositoriesPluginAppliesToProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'repositories'

        project.repositories.clear()
        project.repositories { sourceforge('ikvm','[module]/[revision]/[artifact]-[revision].[ext]') }
        testArtifact(project, 'ikvm', 'ikvm', '0.46.0.1', artifact: 'ikvmbin', type: 'zip')
    }
    
    def testArtifact(Map others = [:], Project project, String aGroup, String aName, String aVersion) {
        def dep = project.dependencies.create(group: aGroup, name: aName, version: aVersion) {
            artifact {
                name = others.artifact ?: aName
                classifier = others.classifier
                type = others.type ?: 'jar'
            }
        }
        def file = project.configurations.detachedConfiguration(dep).singleFile
        println file
        assertTrue file.isFile()
    }

    @Test
    public void repositoriesSetup() {
        Project project = ProjectBuilder.builder().build()
        assertTrue RepositoriesPlugin.setupSourceforgeRepositories(project)
        assertFalse RepositoriesPlugin.setupSourceforgeRepositories(project)
    }
}
