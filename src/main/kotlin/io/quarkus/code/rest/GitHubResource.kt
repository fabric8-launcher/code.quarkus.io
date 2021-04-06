package io.quarkus.code.rest

import io.quarkus.code.model.GitHubCreatedRepository
import io.quarkus.code.model.ProjectDefinition
import io.quarkus.code.service.GitHubService
import io.quarkus.code.service.QuarkusProjectService
import io.quarkus.runtime.StartupEvent
import org.eclipse.microprofile.openapi.annotations.Operation
import java.util.logging.Level
import java.util.logging.Logger
import javax.enterprise.event.Observes
import javax.inject.Inject
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MediaType.APPLICATION_JSON
import javax.ws.rs.core.Response


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
    fun createProject(
        @Valid @BeanParam projectDefinition: ProjectDefinition,
        @NotEmpty @HeaderParam("GitHub-Code") code: String,
        @NotEmpty @HeaderParam("GitHub-State") state: String
    ): GitHubCreatedRepository {
        check(gitHubService.isEnabled()) { "GitHub is not enabled" }
        val token = gitHubService.fetchAccessToken(code, state)
        val login = gitHubService.login(token.accessToken)
        if (gitHubService.repositoryExists(login, token.accessToken, projectDefinition.artifactId)) {
            throw WebApplicationException("This repository name ${projectDefinition.artifactId} already exists", 409)
        }
        val location = projectCreator.createTmp(projectDefinition, true)
        val repo = gitHubService.createRepository(login, token.accessToken, projectDefinition.artifactId)
        var created = false;
        var i = 0;
        while (!created && i < 5) {

            created = gitHubService.repositoryExists(login, token.accessToken, projectDefinition.artifactId)
            ++i;
            if (!created) {
                LOG.info("Repository not yet created retrying: $i/3")
            }
        }
        if (!created) {
            throw InternalServerErrorException(
                Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error while creating GitHub repository.")
                    .type(MediaType.TEXT_PLAIN)
                    .build()
            )
        }
        gitHubService.push(repo.ownerName, token.accessToken, repo.url, location)
        return repo
    }
}