package io.quarkus.code

import io.quarkus.code.model.QuarkusProject
import io.quarkus.code.writer.CommonsZipProjectWriter
import io.quarkus.cli.commands.AddExtensions
import io.quarkus.cli.commands.CreateProject
import io.quarkus.generators.BuildTool
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Singleton

@Singleton
open class QuarkusProjectCreator {
    companion object {
        const val RESOURCES_DIR = "/creator/mvnw"
        const val MAVEN_WRAPPER_DIR = ".mvn/wrapper"
        const val MAVEN_WRAPPER_JAR = "$MAVEN_WRAPPER_DIR/maven-wrapper.jar"
        const val MAVEN_WRAPPER_PROPS = "$MAVEN_WRAPPER_DIR/maven-wrapper.properties"
        const val MAVEN_WRAPPER_DOWNLOADER = "$MAVEN_WRAPPER_DIR/MavenWrapperDownloader.java"
        const val MVNW_CMD = "mvnw.cmd"
        const val MVNW = "mvnw"
    }

    open fun create(project: QuarkusProject): ByteArray {
        val baos = ByteArrayOutputStream()
        baos.use {
            val zipWriter = CommonsZipProjectWriter.createWriter(baos, project.artifactId)
            zipWriter.use {
                //FIXME use Quarkus CreateProject when updating version (remove duplication)
                val sourceType = CreateProject.determineSourceType(project.extensions)
                val context = mutableMapOf("path" to (project.path as Any))
                val success = CreateProject(zipWriter)
                    .groupId(project.groupId)
                    .artifactId(project.artifactId)
                    .version(project.version)
                    .sourceType(sourceType)
                    .buildTool(project.buildTool)
                    .className(project.className)
                    .doCreateProject(context)
                if (!success) {
                    throw IOException("Error during Quarkus project creation")
                }
                AddExtensions(zipWriter, project.buildTool)
                    .addExtensions(project.extensions)
                if (project.buildTool == BuildTool.MAVEN) {
                    this.addMvnw(zipWriter)
                } else if (project.buildTool == BuildTool.GRADLE) {
                    //TODO: Add Gradle executables
                }
            }
        }
        return baos.toByteArray()
    }

    private fun addMvnw(zipWrite: CommonsZipProjectWriter) {
        zipWrite.mkdirs(MAVEN_WRAPPER_DIR)
        writeResourceFile(zipWrite, MAVEN_WRAPPER_JAR)
        writeResourceFile(zipWrite, MAVEN_WRAPPER_PROPS)
        writeResourceFile(zipWrite, MAVEN_WRAPPER_DOWNLOADER)
        writeResourceFile(zipWrite, MVNW_CMD, true)
        writeResourceFile(zipWrite, MVNW, true)
    }

    private fun writeResourceFile(zipWrite: CommonsZipProjectWriter, filePath: String, allowExec: Boolean = false) {
        if (!zipWrite.exists(filePath)) {
            val resourcePath = "$RESOURCES_DIR/$filePath"
            val resource = QuarkusProjectCreator::class.java.getResource(resourcePath)
                ?: throw IOException("missing resource $resourcePath")
            val fileAsBytes =
                resource.readBytes()
            zipWrite.write(filePath, fileAsBytes, allowExec)
        }
    }

}