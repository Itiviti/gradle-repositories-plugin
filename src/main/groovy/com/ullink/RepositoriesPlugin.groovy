package com.ullink

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler

class RepositoriesPlugin implements Plugin<Project> {
    // beware that JDK HTTPConnection doesn't handle redirects between HTTP & HTTPS:
    // http://stackoverflow.com/questions/1884230/java-doesnt-follow-redirect-in-urlconnection
    
    void apply(Project project) {
        setupSourceforgeRepositories(project)
        setupGooglecodeRepositories(project)
        setupNugetRepositories(project)
        setupGithubRepositories(project)
        setupBitbucketRepositories(project)
    }

    static boolean setupSourceforgeRepositories(Project project) {
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'sourceforge', String, String, Object)) {
            project.logger.debug 'Adding sourceforge(String,String,Closure?) method to project RepositoryHandler'
            project.repositories.metaClass.sourceforge = { String org, String subPattern, def closure = null ->
                // no common pattern
                // project/pyqt    /PyQt4   /PyQt-4.9.5      /PyQt-Py2.7-x86-gpl-4.9.5-1.exe
                // project/jxplorer/jxplorer/version%203.3.01/jxplorer-3.3.01-windows-installer.exe
                // project/ikvm    /ikvm    /7.1.4532.2      /ikvmbin-7.1.4532.2.zip
                // project/vlc     /2.0.4   /win32           /vlc-2.0.4-win32.exe
                addRepo(project, delegate, 'sourceforge', org, 'http://downloads.sourceforge.net/project', '[organization]/'+subPattern, closure)
            }
            return true
        }
    }

    static def addRepo(Project project, RepositoryHandler del, String repoType, String org, String baseUrl, String pattern, def closure) {
        def repoName = org ? repoType+'-'+org : repoType
        del.ivy {
            name repoName
            url baseUrl
            layout "pattern", {
                artifact pattern
            }
        }
        if (closure) {
            closure.delegate = del
            closure()
        }
    }

    static boolean setupGooglecodeRepositories(Project project) {
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'googlecode', String, String, Object)) {
            project.logger.debug 'Adding googlecode(String?,String?,Closure?) method to project RepositoryHandler'
            project.repositories.metaClass.googlecode = { String org, String subPattern = null, def closure = null ->
                throw new UnsupportedOperationException("HTTP-based repositories w/o HEAD request support is no longer available in Gradle 2.X")
            }
            return true
        }
    }

    static boolean setupGithubRepositories(Project project) { 
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'github', String, String, Object)) {
            project.logger.debug 'Adding github(String?,String?,Closure?) method to project RepositoryHandler'
            project.repositories.metaClass.github = { String org = null, String subPattern = null, def closure = null ->
                throw new UnsupportedOperationException("HTTP-based repositories w/o HEAD request support is no longer available in Gradle 2.X")
            }

            return true
        }
    }

    static boolean setupNugetRepositories(Project project) {
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'nuget', String, String, Object)) {
            project.logger.debug 'Adding nuget(String?,Closure?) method to project RepositoryHandler'
            project.repositories.metaClass.nuget = { String org = null, def closure = null ->
                throw new UnsupportedOperationException("HTTP-based repositories w/o HEAD request support is no longer available in Gradle 2.X")
            }
        }
    }

    static boolean setupBitbucketRepositories(Project project) {
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'bitbucket', String, String, Object)) {
            project.logger.debug 'Adding bitbucket(String?,Closure?) method to project RepositoryHandler'
            project.repositories.metaClass.bitbucket = { String org = null, String subPattern = null, def closure = null ->
                // https://bitbucket.org/shaunwilde/opencover/downloads/opencover.4.5.1604.zip
                subPattern = subPattern ?: '[artifact]-[revision](-[classifier]).[ext]'
                addRepo(project, delegate, 'bitbucket', org, 'http://cdn.bitbucket.org', '[organization]/[module]/downloads/'+subPattern, closure)
            }
        }
    }
}

