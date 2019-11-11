package io.quarkus.code.services

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.kohsuke.github.GHCreateRepositoryBuilder
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHubBuilder
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Path
import java.util.Objects.requireNonNull
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
open class GitHubService {
    @Inject
    lateinit var configManager: CodeQuarkusConfigManager

    val client: OkHttpClient = OkHttpClient()

    @Throws(UncheckedIOException::class)
    open fun createRepository(token: String, repositoryName: String): Pair<String, String> {
        val newlyCreatedRepo: GHRepository
        try {
            val gitHub = GitHubBuilder().withOAuthToken(token).build()
            val repositoryBuilder: GHCreateRepositoryBuilder = gitHub.createRepository(repositoryName)
            newlyCreatedRepo = repositoryBuilder
                    .description("Generated by code.quarkus.io")
                    .create()
        } catch (e: IOException) {
            throw UncheckedIOException(String.format("Could not create GitHub repository named '%s'", repositoryName), e)
        }

        return Pair(newlyCreatedRepo.ownerName, newlyCreatedRepo.httpTransportUrl)
    }

    open fun push(token: String, httpTransportUrl: String, ownerName: String, path: Path) {
        requireNonNull(httpTransportUrl, "httpTransportUrl must not be null.")
        requireNonNull(ownerName, "ownerName must not be null.")
        requireNonNull(path, "path must not be null.")

        try {
            Git.init().setDirectory(path.toFile()).call().use { repo ->
                repo.add().addFilepattern(".").call()
                repo.commit().setMessage("Initial commit")
                        .setAuthor("quarkusio", "no-reply@quarkus.io")
                        .setCommitter("quarkusio", "no-reply@quarkus.io")
                        .setSign(false)
                        .call()

                val pushCommand = repo.push()
                pushCommand.remote = httpTransportUrl
                pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(ownerName, token))
                pushCommand.call()
            }
        } catch (e: GitAPIException) {
            throw IllegalStateException("An error occurred while pushing to the git repo", e)
        }

    }

    open fun fetchAccessToken(code: String, state: String): String {
        val node = ObjectMapper().createObjectNode()
                .put("client_id", configManager.clientId)
                .put("client_secret", configManager.clientSecret)
                .put("state", state)
                .put("code", code)
        val request = Request.Builder()
                .url("https://github.com/login/oauth/access_token")
                .post(node.toString().toRequestBody("application/json".toMediaType())).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            return response.body!!.string()
        }
    }
}