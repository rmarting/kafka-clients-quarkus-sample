package io.jromanmartin.kafka.api;

import io.jromanmartin.kafka.dto.MessageListDTO;
import io.jromanmartin.kafka.service.MessageService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/consumer")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "consumer", description = "Operations to consume messages from a Kafka Cluster")
public class ConsumerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerController.class);

    @Inject
    MessageService messageService;

    @ConfigProperty(name = "app.consumer.poolTimeout")
    Long poolTimeout;

    @Operation(summary = "Get a greeting message")
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Greeting message",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @APIResponse(responseCode = "404", description = "Greeting message not found"),
            @APIResponse(responseCode = "500", description = "Internal Server Error")})
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello Consumer!";
    }

    @Operation(summary = "Get a list of records from a topic")
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "List of records from topic",
                    content = @Content(schema = @Schema(implementation = MessageListDTO.class))),
            @APIResponse(responseCode = "404", description = "Not records in topic"),
            @APIResponse(responseCode = "500", description = "Internal Server Error")})
    @Path("/kafka/{topicName}")
    @GET()
    public Response pollFromTopic(
            @Parameter(description = "Topic name", required = true) @PathParam("topicName") String topicName,
            @Parameter(description = "Commit results", required = false) @QueryParam("commit") boolean commit,
            @Parameter(description = "Partition ID", required = false) @QueryParam("partition") Integer partition) {
        var messageListDTO = messageService.pollEvents(topicName, partition, commit);

        // Prepare response
        if (messageListDTO.getMessages().isEmpty()) {
            LOGGER.debug("Not found messages (404) in topic {}", topicName);

            return Response.status(Response.Status.NOT_FOUND).entity(messageListDTO).build();
        }

        LOGGER.debug("Pulled successfully messages (200) from topic {}", topicName);

        return Response.ok().entity(messageListDTO).build();
    }

}
