package io.quarkus.code.services

import io.quarkus.code.model.QuarkusProject
import io.quarkus.maven.utilities.MojoUtils
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors


internal class QuarkusProjectCreatorTest {

    companion object {
        val EXPECTED_ZIP_CONTENT = arrayOf(
            "code-with-quarkus/",
            "code-with-quarkus/pom.xml",
            "code-with-quarkus/src/",
            "code-with-quarkus/src/main/",
            "code-with-quarkus/src/main/java/",
            "code-with-quarkus/src/main/java/org/",
            "code-with-quarkus/src/main/java/org/acme/",
            "code-with-quarkus/src/main/java/org/acme/ExampleResource.java",
            "code-with-quarkus/src/test/",
            "code-with-quarkus/src/test/java/",
            "code-with-quarkus/src/test/java/org/",
            "code-with-quarkus/src/test/java/org/acme/",
            "code-with-quarkus/src/test/java/org/acme/ExampleResourceTest.java",
            "code-with-quarkus/src/test/java/org/acme/NativeExampleResourceIT.java",
            "code-with-quarkus/src/main/resources/",
            "code-with-quarkus/src/main/resources/META-INF/",
            "code-with-quarkus/src/main/resources/META-INF/resources/",
            "code-with-quarkus/src/main/resources/META-INF/resources/index.html",
            "code-with-quarkus/src/main/docker/",
            "code-with-quarkus/src/main/docker/Dockerfile.native",
            "code-with-quarkus/src/main/docker/Dockerfile.jvm",
            "code-with-quarkus/.dockerignore",
            "code-with-quarkus/src/main/resources/application.properties",
            "code-with-quarkus/.gitignore",
            "code-with-quarkus/.mvn/",
            "code-with-quarkus/.mvn/wrapper/",
            "code-with-quarkus/.mvn/wrapper/maven-wrapper.jar",
            "code-with-quarkus/.mvn/wrapper/maven-wrapper.properties",
            "code-with-quarkus/.mvn/wrapper/MavenWrapperDownloader.java",
            "code-with-quarkus/mvnw.cmd",
            "code-with-quarkus/mvnw"
        )

        val EXPECTED_ZIP_CONTENT_CUSTOM = arrayOf(
            "test-app/",
            "test-app/pom.xml",
            "test-app/src/",
            "test-app/src/main/",
            "test-app/src/main/java/",
            "test-app/src/main/java/com/",
            "test-app/src/main/java/com/test/",
            "test-app/src/main/java/com/test/TestResource.java",
            "test-app/src/test/",
            "test-app/src/test/java/",
            "test-app/src/test/java/com/",
            "test-app/src/test/java/com/test/",
            "test-app/src/test/java/com/test/TestResourceTest.java",
            "test-app/src/test/java/com/test/NativeTestResourceIT.java",
            "test-app/src/main/resources/",
            "test-app/src/main/resources/META-INF/",
            "test-app/src/main/resources/META-INF/resources/",
            "test-app/src/main/resources/META-INF/resources/index.html",
            "test-app/src/main/docker/",
            "test-app/src/main/docker/Dockerfile.native",
            "test-app/src/main/docker/Dockerfile.jvm",
            "test-app/.dockerignore",
            "test-app/src/main/resources/application.properties",
            "test-app/.gitignore",
            "test-app/.mvn/",
            "test-app/.mvn/wrapper/",
            "test-app/.mvn/wrapper/maven-wrapper.jar",
            "test-app/.mvn/wrapper/maven-wrapper.properties",
            "test-app/.mvn/wrapper/MavenWrapperDownloader.java",
            "test-app/mvnw.cmd",
            "test-app/mvnw"
        )
    }

    @Test
    @DisplayName("When using default project, then, it should create all the files correctly with the requested content")
    fun testCreateProject() {
        // When
        val creator = QuarkusProjectCreator()
        val proj = creator.create(QuarkusProject())
        val (testDir, zipList) = ProjectTestHelpers.extractProject(proj)
        val fileList = ProjectTestHelpers.readFiles(testDir)
        val pomText = Paths.get(testDir.path, "code-with-quarkus/pom.xml")
            .toFile().readText(Charsets.UTF_8)
        val resourceText = Paths.get(testDir.path, "code-with-quarkus/src/main/java/org/acme/ExampleResource.java")
            .toFile().readText(Charsets.UTF_8)
        // Then
        assertThat(zipList, contains(*EXPECTED_ZIP_CONTENT))

        assertThat(fileList.size, equalTo(33))

        assertThat(pomText, containsString("<groupId>org.acme</groupId>"))
        assertThat(pomText, containsString("<artifactId>code-with-quarkus</artifactId>"))
        assertThat(pomText, containsString("<version>1.0.0-SNAPSHOT</version>"))
        assertThat(pomText, containsString("<quarkus-plugin.version>${MojoUtils.getPluginVersion()}</quarkus-plugin.version>"))

        assertThat(resourceText, containsString("@Path(\"/hello\")"))
    }

    @Test
    @DisplayName("When using a custom project, then, it should create all the files correctly with the requested content")
    fun testCreateCustomProject() {
        // When
        val creator = QuarkusProjectCreator()
        val proj = creator.create(
            QuarkusProject(
                groupId = "com.test",
                artifactId = "test-app",
                version = "2.0.0",
                className = "com.test.TestResource",
                path = "/test/it",
                extensions = setOf(
                    "io.quarkus:quarkus-resteasy-jsonb",
                    "io.quarkus:quarkus-hibernate-validator",
                    "io.quarkus:quarkus-neo4j"
                )
            )
        )
        val (testDir, zipList) = ProjectTestHelpers.extractProject(proj)
        val fileList = ProjectTestHelpers.readFiles(testDir)
        val pomText = Paths.get(testDir.path, "test-app/pom.xml")
            .toFile().readText(Charsets.UTF_8)
        val resourceText = Paths.get(testDir.path, "test-app/src/main/java/com/test/TestResource.java")
            .toFile().readText(Charsets.UTF_8)

        // Then
        assertThat(zipList, contains(*EXPECTED_ZIP_CONTENT_CUSTOM))
        assertThat(fileList.size, equalTo(33))

        assertThat(pomText, containsString("<groupId>com.test</groupId>"))
        assertThat(pomText, containsString("<artifactId>test-app</artifactId>"))
        assertThat(pomText, containsString("<version>2.0.0</version>"))
        assertThat(pomText, containsString("<quarkus-plugin.version>${MojoUtils.getPluginVersion()}</quarkus-plugin.version>"))
        assertThat(pomText, containsString("<groupId>io.quarkus</groupId>"))
        assertThat(pomText, containsString("<artifactId>quarkus-resteasy-jsonb</artifactId>"))
        assertThat(pomText, containsString("<artifactId>quarkus-hibernate-validator</artifactId>"))
        assertThat(pomText, containsString("<artifactId>quarkus-neo4j</artifactId>"))

        assertThat(resourceText, containsString("@Path(\"/test/it\")"))
    }

    @Test
    @DisplayName("Should create multiple project correctly")
    @Timeout(2)
    fun testCreateMultipleProject() {
        val executorService = Executors.newFixedThreadPool(4)

        val latch = CountDownLatch(20)
        val creator = QuarkusProjectCreator()
        val creates = (1..20).map { i ->
            Callable {
                val result = creator.create(QuarkusProject())
                latch.countDown()
                result
            }
        }
        executorService.invokeAll(creates)
        println("await")
        latch.await()
        println("done")
    }

}
