# Gradle Repositories Plugin [![Build Status](https://buildhive.cloudbees.com/job/Ullink/job/gradle-repositories-plugin/badge/icon)](https://buildhive.cloudbees.com/job/gluck/job/gradle-repositories-plugin/)

This plugin extends the gradle repository class with some helpers to access download pages from various projects as artifact repositories.
It supports download pages from the following sites:
- Sourceforge
- Googlecode
- GitHub

It also adds NuGet service as artifact repository.

## Usage

When applied (like this), this plugin will add extra methods to RepositoryHandler:

    buildscript {
        repositories {
            mavenCentral()
        }
    
        dependencies {
            classpath "com.ullink.gradle:gradle-repositories-plugin:1.1"
        }
    }
    
    apply plugin:'repositories'
    
    repositories {
        googlecode()
        
        // you can restrict the group it applies to (== organization)
        googlecode('facebook-java-api')
        
        // you can customize the sub-pattern used for uploaded artifacts
        googlecode('il-repack', '[artifact]_[revision].[ext]')
    }
    
    dependencies {
        compile group: 'facebook-java-api', name: 'facebook-java-api', version: '2.0.+'
        foo group: 'il-repack', name: 'ILRepack', version: '1.18'
    }

## I was using URLResolver, why switch to these ?

- this plugin supports revisions patterns (e.g. '2.0.+')
- this plugin hacks around issues with googlecode or nuget ([GRADLE-2124](http://issues.gradle.org/browse/GRADLE-2124))


## Supported repositories methods

### GoogleCode

    repositories {
        googlecode(String group = null, String subPattern = null, Closure closure = null)
    }
    
- *group:* this will match both your googlecode project and your dependency group ( **recommended** to restrict lookup)

- *subPattern:* to customize artifact naming pattern, default is '\[artifact]-\[revision](-\[classifier]).\[ext]'

- *closure:* extra configuration code for your repository (can be used to customize its name or add extra patterns)

### SourceForge

    repositories {
        sourceforge(String group, String subPattern, Closure closure = null)
    }
   
- *group:* this will match both your googlecode project and your dependency group

- *subPattern:* sourceforge supports folders, so almost every project has its own download pattern, e.g. :
    sourceforge('ikvm','[module]/[revision]/[artifact]-[revision].[ext]')

- *closure:* extra configuration code for your repository (can be used to customize its name or add extra patterns)
    
### GitHub
    
    repositories {
        github(String group = null, String subPattern = null, Closure closure = null)
    }
    
- *group:* this will match both your github project and your dependency group ( **recommended** to restrict lookup)

- *subPattern:* to customize artifact naming pattern, default is '\[artifact]-\[revision](-\[classifier]).\[ext]'

- *closure:* extra configuration code for your repository (can be used to customize its name or add extra patterns)

### NuGet
    
    repositories {
        nuget(String group = null, Closure closure = null)
    }

- *group:* this will match both your nuget project and your dependency group ( **recommended** to restrict lookup)

- *closure:* extra configuration code for your repository (can be used to customize its name or add extra patterns)


# License

All these plugins are licensed under the [Creative Commons � CC0 1.0 Universal](http://creativecommons.org/publicdomain/zero/1.0/) license with no warranty (expressed or implied) for any purpose.
