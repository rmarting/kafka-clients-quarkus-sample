package io.jromanmartin.kafka.api;

import io.jromanmartin.kafka.dto.MessageDTO;
import io.jromanmartin.kafka.service.MessageService;
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/producer")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "producer", description = "Operations to produce messages to a Kafka Cluster")
public class ProducerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerController.class);

    @Inject
    MessageService messageService;

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
        return "Hello Producer!";
    }

    @Operation(summary = "Send a message synchronously using the Kafka Client Producer API")
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Message sent",
                    content = @Content(schema = @Schema(implementation = MessageDTO.class))),
            @APIResponse(responseCode = "404", description = "Message not sent"),
            @APIResponse(responseCode = "500", description = "Internal Server Error")
    })
    @Path("/kafka/{topicName}")
    @POST
    public Response sendToTopic(
            @Parameter(description = "Topic name", required = true) @PathParam("topicName") String topicName,
            @Parameter(description = "Message to send", required = true) MessageDTO messageDTO) {
        messageDTO = messageService.publishSync(topicName, messageDTO);

        LOGGER.debug("Published successfully message (200) into topic {}", topicName);

        return Response.ok().entity(messageDTO).build();
    }

    @Operation(summary = "Send a message asynchronously using the Kafka Client Producer API")
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Message sent",
                    content = @Content(schema = @Schema(implementation = MessageDTO.class))),
            @APIResponse(responseCode = "404", description = "Message not sent"),
            @APIResponse(responseCode = "500", description = "Internal Server Error")
    })
    @Path("/kafka/async/{topicName}")
    @POST
    public Response sendToTopicAsync(
            @Parameter(description = "Topic name", required = true) @PathParam("topicName") String topicName,
            @Parameter(description = "Topic name", required = true) MessageDTO messageDTO) {
        messageDTO = messageService.publishAsync(topicName, messageDTO);

        LOGGER.debug("Published successfully async message (200) into topic {}", topicName);

        return Response.ok().entity(messageDTO).build();
    }

}
