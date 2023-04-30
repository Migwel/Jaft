package dev.migwel.jaft.controller;

import dev.migwel.jaft.election.VoteService;
import dev.migwel.jaft.rpc.RequestVoteRequest;
import dev.migwel.jaft.rpc.RequestVoteResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class VoteController {

    private final VoteService voteService;

    @Autowired
    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/requestVote")
    @ResponseBody
    public RequestVoteResponse requestVote(@RequestBody RequestVoteRequest request) {
        return voteService.requestVote(request);
    }
}
