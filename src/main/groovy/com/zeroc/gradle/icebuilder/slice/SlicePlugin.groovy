// **********************************************************************
//
// Copyright (c) 2014-present ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

class SlicePlugin implements Plugin<Project> {
    private static final def LOGGER = Logging.getLogger(SliceTask)

    public static final String GROUP_SLICE = "slice"

    public static final String TASK_COMPILE_SLICE = "compileSlice"

    void apply(Project project) {
        project.tasks.create('compileSlice', SliceTask) {
            group = "Slice"
        }

        // Create and install the extension object.
        def slice = project.extensions.create("slice", SliceExtension,
                project.container(Java),
                project.container(Python, { name -> new Python(name, project) })
        )

        // slice.extensions.add("python", project.container(Python))

        slice.extensions.create("freezej", Freezej,
                project.container(Dict), project.container(Index))

        if (isAndroidProject(project)) {
            project.afterEvaluate {
                // Android projects do not define a 'compileJava' task. We wait until the project is evaluated
                // and add our dependency to the variant's javaCompiler task.
                getAndroidVariants(project).all { variant ->
                    variant.registerJavaGeneratingTask(project.tasks.getByName('compileSlice'), slice.output)
                }
            }
        } else {
            project.plugins.withType(JavaPlugin) {
                // Set a resolution strategy for zeroc dependencies
                // Configuration compileClassPath = project.configurations.getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
                project.configurations.configureEach { Configuration config ->
                    config.resolutionStrategy.eachDependency { DependencyResolveDetails details ->
                        if (details.requested.group == "com.zeroc") {
                            details.useVersion slice.iceVersion
                        }
                    }
                }

                // Set output dir to be 'build/slice/java'
                slice.output = project.file("${project.buildDir}/slice/java")

                // Add slice.output as java source dir
                JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention)
                SourceSet main = javaConvention.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                main.java.srcDirs(slice.output)

                // Task compileJava triggers compileSlice
                project.tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME).configure {
                    it.dependsOn(TASK_COMPILE_SLICE)
                }
            }
        }
    }

    def isAndroidProject(Project project) {
        return project.hasProperty('android') && project.android.sourceSets
    }

    def getAndroidVariants(Project project) {
        // https://sites.google.com/a/android.com/tools/tech-docs/new-build-system/user-guide
        return project.android.hasProperty('libraryVariants') ?
                project.android.libraryVariants : project.android.applicationVariants
    }
}
