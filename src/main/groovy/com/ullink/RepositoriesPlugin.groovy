package com.ullink

import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.plugins.resolver.DependencyResolver
import org.apache.ivy.plugins.resolver.util.ResolvedResource
import org.apache.ivy.plugins.resolver.util.ResourceMDParser
import org.apache.ivy.util.url.ApacheURLLister
import org.apache.ivy.util.url.URLHandler
import org.apache.ivy.util.url.URLHandlerRegistry
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler

class RepositoriesPlugin implements Plugin<Project> {
    // beware that JDK HTTPConnection doesn't handle redirects between HTTP & HTTPS:
    // http://stackoverflow.com/questions/1884230/java-doesnt-follow-redirect-in-urlconnection
    
    void apply(Project project) {
        setupSourceforgeRepositories(project)
        setupGooglecodeRepositories(project)
        setupGithubRepositories(project)
        setupNugetRepositories(project)
    }
    
    static void setupAndAddResolver(Project project, RepositoryHandler del, DependencyResolver resolver, String repoType, String org, List<String> patterns, def closure) {
        def repoName = org ? repoType+'-'+org : repoType
        project.logger.info "Adding ${repoName} repository with pattern: ${patterns}"
        del.add(resolver) {
            name = repoName
            for (String str : patterns) {
                delegate.addArtifactPattern(str)
            }
            if (closure) {
                closure.delegate = delegate
                closure()
            }
        }
    }
    
    static List<String> getAllHrefsPath(URL listUrl) {
        getAllHrefs(listUrl).collect { it.path }
    }
    
    static String getText(URL listUrl) {
        URLHandlerRegistry.getDefault().openStream(listUrl).getText()
    }
    
    static List<URL> getAllHrefs(URL listUrl) {
        String htmlText = getText(listUrl)
        (htmlText =~ /href="([^<"#]+)"/).collect { new URL(listUrl, it[1]) }
    }
    
    static org.apache.ivy.plugins.resolver.URLResolver getResolver(String org) {
        new org.apache.ivy.plugins.resolver.URLResolver() {
            protected ResolvedResource findResourceUsingPattern(ModuleRevisionId mrid, String pattern, Artifact artifact, ResourceMDParser rmdparser, Date date) {
                if (org == null || mrid.organisation == org) {
                    // pattern = pattern.replace('[timestamp]',String.valueOf((long)(System.currentTimeMillis()/1000)))
                    super.findResourceUsingPattern(mrid, pattern, artifact, rmdparser, date)
                }
            }
        }
    }
    
    static org.apache.ivy.plugins.resolver.URLResolver getNoHeadResolver(String org) {
        new org.apache.ivy.plugins.resolver.URLResolver() {
            protected ResolvedResource findResourceUsingPattern(ModuleRevisionId mrid, String pattern, Artifact artifact, ResourceMDParser rmdparser, Date date) {
                if (org == null || mrid.organisation == org) {
                    // work around http://code.google.com/p/support/issues/detail?id=660
                    URLHandlerRegistry.getDefault().requestMethod = URLHandler.REQUEST_METHOD_GET
                    try {
                        super.findResourceUsingPattern(mrid, pattern, artifact, rmdparser, date)
                    } finally {
                        URLHandlerRegistry.getDefault().requestMethod = URLHandler.REQUEST_METHOD_HEAD
                    }
                }
            }
        }
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
                def pat = 'http://downloads.sourceforge.net/project/[organization]/'+subPattern // +'?ts=[timestamp]'
                def resolver = getResolver(org)
                resolver.repository.lister = new ApacheURLLister() {
                    // http://downloads.sourceforge.net/project/ikvm/ikvm/7.1.4532.2/
                    // ->
                    // http://sourceforge.net/projects/ikvm/files/ikvm/
                    public List retrieveListing(URL origUrl, boolean includeFiles, boolean includeDirectories) throws IOException {
                        if (!(origUrl.path =~ "^/project/")) {
                            return []
                        }
                        def listPath = origUrl.path.substring(9)
                        listPath = '/projects/'+listPath.replaceFirst("/", "/files/")
                        URL url = new URL(origUrl, "//sourceforge.net${listPath}")
                        def matches = getAllHrefsPath(url)
                        def ret = []
                        if (includeFiles) {
                            ret += matches.findAll { it ==~ "/projects/.*/download" }
                                .collect { new URL(origUrl, '/project/' + it.substring(10, it.length()-9)) }
                        }
                        if (includeDirectories) {
                            ret += matches.findAll { it.startsWith(listPath) && !it.endsWith('/timeline') }
                                .collect { new URL(url, it)}
                        }
                        ret
                    }
                }
                setupAndAddResolver(project, delegate, resolver, 'sourceforge', org, [pat], closure)
            }
            return true
        }
    }
    
    static boolean setupGooglecodeRepositories(Project project) {
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'googlecode', String, String, Object)) {
            project.logger.debug 'Adding googlecode(String?,String?,Closure?) method to project RepositoryHandler'
            project.repositories.metaClass.googlecode = { String org = null, String subPattern = null, def closure = null ->
                // http://facebook-java-api.googlecode.com/files/facebook-java-api-2.0.0-bin.zip
                // http://mb-unit.googlecode.com/files/GallioBundle-3.3.458.0.zip
                subPattern = subPattern ?: '[artifact]-[revision](-[classifier]).[ext]'
                def pat = 'http://[organization].googlecode.com/files/'+subPattern
                def resolver = getNoHeadResolver(org)
                resolver.repository.lister = new ApacheURLLister() {
                    // http://facebook-java-api.googlecode.com/files/
                    // ->
                    // http://code.google.com/p/facebook-java-api/downloads/list?can=1&colspec=Filename&num=1000
                    public List retrieveListing(URL origUrl, boolean includeFiles, boolean includeDirectories) throws IOException {
                        if (!includeFiles || !(origUrl.path ==~ "/files/?")) {
                            return []
                        }
                        URL url = new URL("http://code.google.com/p/${origUrl.host-'.googlecode.com'}/downloads/list?can=1&colspec=Filename&num=1000")
                        getAllHrefs(url)
                            .findAll { it.protocol == origUrl.protocol && it.host == origUrl.host && it.port == origUrl.port && it.path.startsWith(origUrl.path) }
                    }
                }
                setupAndAddResolver(project, delegate, resolver, 'googlecode', org, [pat], closure)
            }
            return true
        }
    }

    static boolean setupGithubRepositories(Project project) {
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'github', String, String, Object)) {
            project.logger.debug 'Adding github(String?,String?,Closure?) method to project RepositoryHandler'
            project.repositories.metaClass.github = { String org = null, String subPattern = null, def closure = null ->
                // http://cloud.github.com/downloads/RobertFischer/gradle-gaea-plugin/gradle-gaea-plugin-0.0.3.jar
                // http://cloud.github.com/downloads/RobertFischer/gradle-gaea-plugin/gradle-gaea-plugin-0.0.3.pom
                // http://cloud.github.com/downloads/gluck/il-repack/ILRepack_1.18.zip
                subPattern = subPattern ?: '[module]/[artifact]-[revision](-[classifier]).[ext]'
                def pat = 'http://cloud.github.com/downloads/[organisation]/' + subPattern
                def resolver = getResolver(org)
                resolver.repository.lister = new ApacheURLLister() {
                    // http://cloud.github.com/downloads/RobertFischer/gradle-gaea-plugin/
                    // ->
                    // http://github.com/RobertFischer/gradle-gaea-plugin/downloads
                    public List retrieveListing(URL origUrl, boolean includeFiles, boolean includeDirectories) throws IOException {
                        if (!includeFiles || !(origUrl.path =~ "^/downloads/")) {
                            return []
                        }
                        URL url = new URL("https://github.com/${origUrl.path.substring(11)}/downloads")
                        getAllHrefsPath(url)
                            .findAll { it =~ "^/downloads/"}
                            .collect { new URL(origUrl, it) }
                    }
                }
                setupAndAddResolver(project, delegate, resolver, 'github', org, [pat], closure)
            }
            return true
        }
    }

    
    static boolean setupNugetRepositories(Project project) {
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'nuget', String, String, Object)) {
            project.logger.debug 'Adding nuget(String?,Closure?) method to project RepositoryHandler'
            project.repositories.metaClass.nuget = { String org = null, def closure = null ->
                
                // https://nuget.org/packages/ILRepack/1.17
                // http://nuget.org/api/v2/Packages
                // http://nuget.org/api/v2/package/ILRepack/1.17
                
                // HTTPS (instead of HTTP) because of redirect to HTTPS mirror
                def pat = "https://nuget.org/api/v2/package/[module]/[revision]"
                def resolver = getNoHeadResolver(org)
                resolver.repository.lister = new ApacheURLLister() {
                    // http://nuget.org/api/v2/package/ILRepack/
                    // ->
                    // http://nuget.org/api/v2/FindPackagesById()?id='ILRepack'
                    public List retrieveListing(URL origUrl, boolean includeFiles, boolean includeDirectories) throws IOException {
                        if (!includeFiles || !(origUrl.path =~ "^/api/v2/package/")) {
                            return []
                        }
                        URL url = new URL("http://nuget.org/api/v2/FindPackagesById()?id='${origUrl.path.substring(16, origUrl.path.length()-1)}'")
                        def xml = new XmlSlurper().parseText(url.newReader(requestProperties: [accept: 'application/atom+xml']).getText())
                        xml.entry.content.@src.collect { new URL(it.toString()) }
                    }
                }
                setupAndAddResolver(project, delegate, resolver, 'nuget', org, [pat], closure)
            }
        }
    }
}

