package dev.migwel.jaft.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RequestVoteResponse(@JsonProperty(required = true) long term,
                                  @JsonProperty(required = true) boolean voteGranted) {}
