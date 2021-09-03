package de.faweizz.topicservice

import com.auth0.jwk.UrlJwkProvider
import com.fasterxml.jackson.databind.SerializationFeature
import de.faweizz.topicservice.adjacent.atlas.AtlasClient
import de.faweizz.topicservice.adjacent.keycloak.KeycloakClient
import de.faweizz.topicservice.persistence.Mongo
import de.faweizz.topicservice.service.Configuration
import de.faweizz.topicservice.service.auditlogging.AuditLogger
import de.faweizz.topicservice.service.client.ClientService
import de.faweizz.topicservice.service.joined.execution.DeploymentService
import de.faweizz.topicservice.service.joined.execution.JoinedResourceCodeExecutionService
import de.faweizz.topicservice.service.joined.resource.JoinedResourceService
import de.faweizz.topicservice.service.messaging.StubMessagingService
import de.faweizz.topicservice.service.resource.ResourceService
import de.faweizz.topicservice.service.sharing.ResourceSharingService
import de.faweizz.topicservice.service.transformation.TransformationService
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.net.URL

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    val configuration = Configuration(
        kafkaAddress = "kafka:9093",
        trustStoreLocation = "/home/kafka.client.truststore.jks",
        trustStorePassword = "password"
    )
    val mongo = Mongo()
    val atlasClient = AtlasClient(
        atlasAddress = "http://atlas:21000",
        kafkaAddress = configuration.kafkaAddress,
        keycloakAddress = "http://keycloak:8080"
    )
    val keycloakClient = KeycloakClient()
    val messagingService = StubMessagingService()
    val kafkaClient = de.faweizz.topicservice.adjacent.kafka.KafkaClient(configuration)
    val auditLogger = AuditLogger(kafkaClient)

    val transformationService = TransformationService(mongo, atlasClient, configuration)
    val resourceService = ResourceService(atlasClient, mongo, kafkaClient, auditLogger)
    val topicSharingService =
        ResourceSharingService(mongo, messagingService, resourceService, transformationService, auditLogger)
    val clientService = ClientService(mongo, keycloakClient, atlasClient, auditLogger)
    val joinedResourceService =
        JoinedResourceService(mongo, atlasClient, kafkaClient, messagingService, transformationService, auditLogger)
    val deploymentService = DeploymentService(mongo, configuration)
    val joinedResourceCodeExecutionService =
        JoinedResourceCodeExecutionService(
            mongo,
            messagingService,
            resourceService,
            clientService,
            deploymentService,
            auditLogger
        )

    install(Authentication) {
        jwt {
            verifier(UrlJwkProvider(URL("http://keycloak:8080/auth/realms/TestRealm/protocol/openid-connect/certs")))
            validate {
                val username = it.payload.claims["preferred_username"]?.asString() ?: return@validate null
                Actor(username)
            }
        }
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    routing {
        authenticate {
            post("resource") {
                resourceService.executeCreationRequest(call.receive(), call.getActor())
                call.respond(HttpStatusCode.OK)
            }

            post("resource/{id}/request-sharing") {
                call.respond(topicSharingService.requestSharing(call.getIdParameter(), call.getActor()))
            }

            post("sharing-request/{id}/accept") {
                topicSharingService.accept(call.getActor(), call.getIdParameter())
                call.respond(HttpStatusCode.OK)
            }

            post("sharing-request/{id}/deny") {
                topicSharingService.decline(call.getActor(), call.getIdParameter())
                call.respond(HttpStatusCode.OK)
            }

            post("sharing-request/{id}/transformation") {
                call.respond(
                    topicSharingService.addTransformation(
                        call.getIdParameter(),
                        call.receive(),
                        call.getActor()
                    )
                )
            }

            post("clients") {
                call.respond(clientService.createClientIfActorIsPermitted(call.getActor(), call.receive()))
            }

            post("joined-resource") {
                call.respond(joinedResourceService.create(call.receive(), call.getActor()))
            }

            post("joined-resource-poll") {
                call.respond(joinedResourceService.createJoinRequest(call.receive(), call.getActor()))
            }

            post("joined-resource-poll/{id}/accept") {
                call.respond(joinedResourceService.accept(call.getActor(), call.getIdParameter()))
            }

            post("joined-resource-poll/{id}/decline") {
                call.respond(joinedResourceService.decline(call.getActor(), call.getIdParameter()))
            }

            post("joined-resource-code-poll") {
                call.respond(
                    joinedResourceCodeExecutionService.createCodeExecutionRequest(
                        call.getActor(),
                        call.receive()
                    )
                )
            }

            post("joined-resource-code-poll/{id}/accept") {
                call.respond(joinedResourceCodeExecutionService.accept(call.getActor(), call.getIdParameter()))
            }

            post("joined-resource-code-poll/{id}/decline") {
                call.respond(joinedResourceCodeExecutionService.decline(call.getActor(), call.getIdParameter()))
            }
        }
    }
}

private fun ApplicationCall.getIdParameter(): String {
    return parameters["id"] ?: throw Exception("missing id parameter")
}

private fun ApplicationCall.getActor(): Actor {
    return principal() ?: throw Exception("Error authenticating")
}

