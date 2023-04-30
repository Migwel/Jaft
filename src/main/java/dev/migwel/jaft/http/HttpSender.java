package dev.migwel.jaft.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.migwel.jaft.election.HeartbeatService;
import dev.migwel.jaft.rpc.AppendEntriesResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class HttpSender {

    private static final Logger log = LogManager.getLogger(HttpSender.class);


    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public HttpSender(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @CheckForNull
    public <REQ, RES> RES send(URI uri, REQ request, Class<RES> responseClass) {
        HttpResponse<String> responseStr;
        String requestBody = requestToString(request);
        if (requestBody == null) {
            log.warn("Could not serialize request: "+ request);
            return null;
        }
        try {
            responseStr = httpClient.send(buildHttpRequest(uri, requestBody), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException | RuntimeException e) {
            log.warn("Something went wrong while calling server: "+ uri, e);
            return null;
        }
        if (responseStr.statusCode() != 200) {
            return null;
        }

        try {
            return objectMapper.readValue(responseStr.body(), responseClass);
        } catch (JsonProcessingException e) {
            log.warn("Could not deserialize the response: "+ responseStr.body(), e);
            return null;
        }
    }

    @CheckForNull
    private <REQ> String requestToString(REQ request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.warn("Something went wrong while serializing the request", e);
            return null;
        }
    }

    @Nonnull
    private HttpRequest buildHttpRequest(URI uri, String body) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }
}
