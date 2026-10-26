package com.example.camundademo.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.camundademo.dto.StartRequest;
import com.example.camundademo.dto.StatusUpdate;
import com.example.camundademo.service.RequestStatusStore;
import com.example.camundademo.service.WorkflowService;

/**
 * Simule le SERVICE EXTERNE : reçoit une requête, lance le workflow, et reçoit
 * les changements de statut envoyés par le workflow (http-connector).
 */
@RestController
@RequestMapping("/mock-api")
public class MockExternalApiController {

    private static final Logger log = LoggerFactory.getLogger(MockExternalApiController.class);

    private final RequestStatusStore store;
    private final WorkflowService workflowService;

    public MockExternalApiController(RequestStatusStore store, WorkflowService workflowService) {
        this.store = store;
        this.workflowService = workflowService;
    }

    /** 1) Le service externe reçoit une requête (WAITING_TEAM) et lance le workflow. */
    @PostMapping("/requests")
    public ResponseEntity<Map<String, Object>> receiveRequest(@RequestBody StartRequest req) {
        ProcessInstance pi = workflowService.start(req);
        store.record(req.idRequest(), "WAITING_TEAM");
        log.info("[MOCK-API] requête {} reçue -> WAITING_TEAM, workflow {}", req.idRequest(), pi.getId());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idRequest", req.idRequest());
        body.put("status", "WAITING_TEAM");
        body.put("processInstanceId", pi.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    /** 2) Appelé par le workflow : changement de statut (WAITING_TRADER, COMPLETED, EXPIRED). */
    @PostMapping("/requests/{idRequest}/status")
    public Map<String, Object> updateStatus(@PathVariable String idRequest, @RequestBody StatusUpdate body) {
        if (store.isFailMode()) {
            log.warn("[MOCK-API] mode panne actif : 503 pour {}", idRequest);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service externe indisponible (simulé)");
        }
        store.record(idRequest, body.status());
        log.info("[MOCK-API] {} -> {}", idRequest, body.status());
        return Map.of("ok", true, "idRequest", idRequest, "status", body.status());
    }

    /** Consultation du statut + historique. */
    @GetMapping("/requests/{idRequest}")
    public Map<String, Object> get(@PathVariable String idRequest) {
        String current = store.current(idRequest);
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Requête inconnue : " + idRequest);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idRequest", idRequest);
        m.put("status", current);
        m.put("history", store.history(idRequest));
        return m;
    }

    @GetMapping("/requests")
    public Map<String, String> all() {
        return store.all();
    }

    /** Simule une panne du service externe (pour tester les incidents Camunda). */
    @PostMapping("/fail")
    public Map<String, Object> fail(@RequestParam boolean enabled) {
        store.setFailMode(enabled);
        return Map.of("failMode", enabled);
    }
}
