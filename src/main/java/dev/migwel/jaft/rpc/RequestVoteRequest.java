package dev.migwel.jaft.rpc;


import com.fasterxml.jackson.annotation.JsonProperty;

public record RequestVoteRequest(@JsonProperty(required = true) long term,
                                 @JsonProperty(required = true) String candidateId,
                                 @JsonProperty(required = true) long lastLogEntry,
                                 @JsonProperty(required = true) long lastLogTerm){}
