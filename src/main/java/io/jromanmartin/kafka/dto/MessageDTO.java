package io.jromanmartin.kafka.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serializable;

public class MessageDTO implements Serializable {

    private static final long serialVersionUID = 1305278483346223763L;

    @Schema(description = "Key to identify this message.", required = false, example = "1")
    private String key;

    @Schema(description = "Timestamp.", required = true)
    private long timestamp;

    @Schema(description = "Content.", required = true, example = "Simple message")
    private String content;

    @Schema(description = "Partition number.", required = false, readOnly = true /*accessMode = Schema.AccessMode.READ_ONLY*/)
    private int partition;

    @Schema(description = "Offset in the partition.", required = false, readOnly = true /*accessMode = Schema.AccessMode.READ_ONLY*/)
    private long offset;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @return the partition
     */
    public int getPartition() {
        return partition;
    }

    /**
     * @param partition the partition to set
     */
    public void setPartition(int partition) {
        this.partition = partition;
    }

    /**
     * @return the offset
     */
    public long getOffset() {
        return offset;
    }

    /**
     * @param offset the offset to set
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }

}
