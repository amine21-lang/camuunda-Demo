package com.example.camundademo.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.camundademo.dto.SubmitBody;
import com.example.camundademo.dto.UserBody;
import com.example.camundademo.service.WorkflowService;

/** Simule les actions des interfaces (équipe et trader) et permet d'inspecter le workflow. */
@RestController
@RequestMapping("/api/requests")
public class WorkflowController {

    private final WorkflowService service;

    public WorkflowController(WorkflowService service) {
        this.service = service;
    }

    /** Etat du workflow : state, tâches actives, reminderCount, outcome... */
    @GetMapping("/{idRequest}")
    public Map<String, Object> status(@PathVariable String idRequest) {
        return service.status(idRequest);
    }

    /** Emails simulés (variable emailLog). */
    @GetMapping(value = "/{idRequest}/emails", produces = MediaType.TEXT_PLAIN_VALUE)
    public String emails(@PathVariable String idRequest) {
        return service.emailLog(idRequest);
    }

    /** Bouton "Assign to me" (interface équipe). */
    @PostMapping("/{idRequest}/team/claim")
    public Map<String, Object> claim(@PathVariable String idRequest, @RequestBody(required = false) UserBody body) {
        String user = (body == null || body.userId() == null) ? "demo" : body.userId();
        service.claimTeamTask(idRequest, user);
        return Map.of("claimedBy", user);
    }

    /** Case "I confirm..." + bouton Submit (interface équipe). */
    @PostMapping("/{idRequest}/team/submit")
    public Map<String, Object> submit(@PathVariable String idRequest, @RequestBody SubmitBody body) {
        String user = body.userId() == null ? "demo" : body.userId();
        boolean confirm = Boolean.TRUE.equals(body.confirmOnBehalf());
        service.submitTeamTask(idRequest, user, confirm);
        return Map.of("submitted", true, "confirmOnBehalf", confirm);
    }

    /** Bouton "Je confirme la confirmation de mon compte" (interface trader). */
    @PostMapping("/{idRequest}/trader/confirm")
    public Map<String, Object> traderConfirm(@PathVariable String idRequest) {
        service.confirmTrader(idRequest);
        return Map.of("traderConfirmed", true);
    }
}
