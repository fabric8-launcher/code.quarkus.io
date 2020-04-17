package io.quarkus.code.rest

import io.quarkus.code.service.QuarkusProjectService
import io.quarkus.code.service.GitHubService
import io.quarkus.code.model.GitHubCreatedRepository
import io.quarkus.code.model.QuarkusProject
import io.quarkus.runtime.StartupEvent
import org.eclipse.microprofile.openapi.annotations.Operation
import java.util.logging.Level
import java.util.logging.Logger
import javax.enterprise.event.Observes
import javax.inject.Inject
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.ws.rs.*
import javax.ws.rs.core.MediaType.APPLICATION_JSON


@Path("/github")
class GitHubResource {

    companion object {
        private val LOG = Logger.getLogger(GitHubResource::class.java.name)
    }

    @Inject
    internal lateinit var projectCreator: QuarkusProjectService

    @Inject
    lateinit var gitHubService: GitHubService

    fun onStart(@Observes e: StartupEvent) {
        if (gitHubService.isEnabled()) {
            LOG.log(Level.INFO) {
                """
            GitHub is enabled:
                clientId = ${gitHubService.config.clientId.orElse(null)}
                clientSecret = xxxxxxxxxx
            """.trimIndent()
            }
        } else {
            LOG.log(Level.INFO, "GitHub is disabled")
        }
    }

    @POST
    @Path("/project")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Create project and push generated code to GitHub")
    fun createProject(@Valid @BeanParam project: QuarkusProject,
                      @NotEmpty @HeaderParam("GitHub-Code") code: String,
                      @NotEmpty @HeaderParam("GitHub-State") state: String): GitHubCreatedRepository {
        check(gitHubService.isEnabled()) { "GitHub is not enabled" }
        val location = projectCreator.createTmp(project)
        val token = gitHubService.fetchAccessToken(code, state);
        if (gitHubService.repositoryExists(token.accessToken, project.artifactId)) {
            throw WebApplicationException("This repository name ${project.artifactId} already exists", 409)
        }
        val repo = gitHubService.createRepository(token.accessToken, project.artifactId)
        gitHubService.push(repo.ownerName, token.accessToken, repo.url, location)
        return repo
    }
}