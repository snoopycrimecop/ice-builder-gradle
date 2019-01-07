package com.zeroc.gradle.icebuilder.slice

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

class Python extends Input {
    final String name

    final Project project

    String args

    FileCollection files

    FileCollection include

    File srcDir

    Python(name, project) {
        this.name = name
        this.project = project
        this.srcDir = project.file("src/main/slice")
    }
}
