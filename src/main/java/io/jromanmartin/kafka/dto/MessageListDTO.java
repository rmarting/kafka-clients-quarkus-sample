package io.jromanmartin.kafka.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MessageListDTO implements Serializable {

    private static final long serialVersionUID = 138669946178014325L;

    @Schema(description = "List of messages", required = false)
    private List<MessageDTO> list = new ArrayList<>();

    /**
     * @return the messages
     */
    public List<MessageDTO> getMessages() {
        return list;
    }

    /**
     * @param messageDTOS the messages to set
     */
    public void setMessages(List<MessageDTO> messageDTOS) {
        this.list = messageDTOS;
    }

    public void addCustomMessage(MessageDTO messageDTO) {
        if (null == list) {
            list = new ArrayList<>();
        }
        list.add(messageDTO);
    }

}
