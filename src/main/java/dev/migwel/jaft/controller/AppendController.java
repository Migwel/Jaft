package dev.migwel.jaft.controller;

import dev.migwel.jaft.rpc.AppendEntriesRequest;
import dev.migwel.jaft.rpc.AppendEntriesResponse;
import dev.migwel.jaft.statemachine.LogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

public class AppendController {

    private final LogService logService;

    public AppendController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping(path="/appendEntries")
    @ResponseBody
    public AppendEntriesResponse appendEntries(@RequestBody AppendEntriesRequest request) {
        return logService.appendEntries(request);
    }
}
