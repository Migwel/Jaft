package dev.migwel.jaft.controller;

import dev.migwel.jaft.rpc.InfoResponse;
import dev.migwel.jaft.server.ServerState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
public class InfoController {

    private final ServerState serverState;

    public InfoController(ServerState serverState) {
        this.serverState = serverState;
    }

    @GetMapping("/info")
    public InfoResponse getInfo() {
        return new InfoResponse(serverState.getLeadership(), serverState.getCurrentTerm(), serverState.getCurrentLeader());
    }
}
