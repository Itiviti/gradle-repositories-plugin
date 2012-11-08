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
        project.repositories { nuget('foo') }
        testArtifact(project, 'foo', 'ILRepack', '1.17+', type: 'nupkg')
        testArtifact(project, 'foo', 'ILRepack', '1.18', type: 'nupkg')
        
        project.repositories.clear()
        project.repositories { googlecode('facebook-java-api') }
        testArtifact(project, 'facebook-java-api', 'facebook-java-api', '2.0.+', classifier: 'bin', type: 'zip')
        testArtifact(project, 'facebook-java-api', 'facebook-java-api', '2.0.5', classifier: 'bin', type: 'zip')
        
        project.repositories.clear()
        project.repositories { sourceforge('ikvm','[module]/[revision]/[artifact]-[revision].[ext]') }
        testArtifact(project, 'ikvm', 'ikvm', '0.46.0.+', artifact: 'ikvmbin', type: 'zip')
        testArtifact(project, 'ikvm', 'ikvm', '0.46.0.1', artifact: 'ikvmbin', type: 'zip')
        
        project.repositories.clear()
        project.repositories { github('gluck','[module]/[artifact]_[revision].[ext]') }
        testArtifact(project, 'gluck', 'il-repack', '1.17+', artifact: 'ILRepack', type: 'zip')
        testArtifact(project, 'gluck', 'il-repack', '1.18+', artifact: 'ILRepack', type: 'zip')
        
        project.repositories.clear()
        project.repositories { github('RobertFischer') }
        testArtifact(project, 'RobertFischer', 'gradle-gaea-plugin', '0.0.+')
        testArtifact(project, 'RobertFischer', 'gradle-gaea-plugin', '0.0.3')
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
