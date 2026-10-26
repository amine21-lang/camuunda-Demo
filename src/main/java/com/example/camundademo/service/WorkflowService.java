package com.example.camundademo.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.camundademo.dto.StartRequest;

@Service
public class WorkflowService {

    public static final String PROCESS_KEY = "Process_CreationProfilTrader";
    public static final String TASK_TEAM = "UserTask_TraiterDemande";
    public static final String TASK_TRADER = "UserTask_ConfirmTrader";

    private static final Set<String> VISIBLE_VARS = Set.of("idRequest", "testMode", "testIntervalSeconds",
            "confirmOnBehalf", "traderConfirmed", "reminderCount", "maxDays", "nextReminderDate", "expiryDate",
            "outcome", "teamTaskUrl", "traderTaskUrl");

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    @Value("${demo.mock-api-base-url}")
    private String apiBaseUrl;
    @Value("${demo.app-base-url}")
    private String appBaseUrl;

    public WorkflowService(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
    }

    /** Démarre le workflow (business key = idRequest, refuse les doublons). */
    public ProcessInstance start(StartRequest r) {
        if (r.idRequest() == null || r.idRequest().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idRequest est obligatoire");
        }
        long running = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(r.idRequest()).count();
        if (running > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un workflow est déjà en cours pour " + r.idRequest());
        }
        Map<String, Object> v = new HashMap<>();
        put(v, "idRequest", r.idRequest());
        put(v, "traderFirstName", r.traderFirstName());
        put(v, "traderLastName", r.traderLastName());
        put(v, "traderEmail", r.traderEmail());
        put(v, "marketId", r.marketId());
        put(v, "marketName", r.marketName());
        v.put("testMode", r.testMode() == null ? Boolean.TRUE : r.testMode());
        v.put("testIntervalSeconds", r.testIntervalSeconds() == null ? 30 : r.testIntervalSeconds());
        v.put("apiBaseUrl", apiBaseUrl);
        v.put("appBaseUrl", appBaseUrl);
        v.put("teamGroup", "camunda-admin");   // le user "demo" voit la tâche dans Tasklist
        v.put("traderUserId", "demo");
        return runtimeService.startProcessInstanceByKey(PROCESS_KEY, r.idRequest(), v);
    }

    /** Etat complet : instance, tâches actives, variables utiles. */
    public Map<String, Object> status(String idRequest) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idRequest", idRequest);
        Map<String, Object> vars = new LinkedHashMap<>();

        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(idRequest).singleResult();
        if (pi != null) {
            out.put("state", "RUNNING");
            out.put("processInstanceId", pi.getId());
            runtimeService.getVariables(pi.getId()).forEach((k, val) -> {
                if (VISIBLE_VARS.contains(k)) vars.put(k, val);
            });
            out.put("activeTasks", tasks(idRequest));
        } else {
            HistoricProcessInstance h = lastHistoric(idRequest);
            out.put("state", "ENDED");
            out.put("processInstanceId", h.getId());
            for (HistoricVariableInstance hv : historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(h.getId()).list()) {
                if (VISIBLE_VARS.contains(hv.getName())) vars.put(hv.getName(), hv.getValue());
            }
            out.put("activeTasks", List.of());
        }
        out.put("variables", vars);
        return out;
    }

    public String emailLog(String idRequest) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(idRequest).singleResult();
        if (pi != null) {
            Object v = runtimeService.getVariable(pi.getId(), "emailLog");
            return v == null ? "(aucun email)" : v.toString();
        }
        HistoricProcessInstance h = lastHistoric(idRequest);
        return historyService.createHistoricVariableInstanceQuery().processInstanceId(h.getId())
                .variableName("emailLog").list().stream().findFirst()
                .map(x -> String.valueOf(x.getValue())).orElse("(aucun email)");
    }

    public List<Map<String, Object>> tasks(String idRequest) {
        return taskService.createTaskQuery().processInstanceBusinessKey(idRequest).list().stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("taskId", t.getId());
                    m.put("name", t.getName());
                    m.put("taskDefinitionKey", t.getTaskDefinitionKey());
                    m.put("assignee", t.getAssignee());
                    return m;
                }).toList();
    }

    /** "Assign to me" : claim exclusif de la tâche équipe. */
    public void claimTeamTask(String idRequest, String userId) {
        Task t = findTask(idRequest, TASK_TEAM);
        try {
            taskService.claim(t.getId(), userId);
        } catch (ProcessEngineException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tâche déjà prise par " + t.getAssignee() + " (" + e.getMessage() + ")");
        }
    }

    /** "Submit" : seul l'opérateur assigné peut soumettre. */
    public void submitTeamTask(String idRequest, String userId, boolean confirmOnBehalf) {
        Task t = findTask(idRequest, TASK_TEAM);
        if (t.getAssignee() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tâche non assignée : faire d'abord 'Assign to me'");
        }
        if (!t.getAssignee().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tâche assignée à " + t.getAssignee());
        }
        taskService.complete(t.getId(), Map.of("confirmOnBehalf", confirmOnBehalf));
    }

    /** Bouton "Je confirme" côté trader. */
    public void confirmTrader(String idRequest) {
        Task t = findTask(idRequest, TASK_TRADER);
        taskService.complete(t.getId(), Map.of("traderConfirmed", true));
    }

    private HistoricProcessInstance lastHistoric(String idRequest) {
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(idRequest).orderByProcessInstanceStartTime().desc()
                .list().stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun workflow pour " + idRequest));
    }

    private Task findTask(String idRequest, String taskDefinitionKey) {
        Task t = taskService.createTaskQuery().processInstanceBusinessKey(idRequest)
                .taskDefinitionKey(taskDefinitionKey).singleResult();
        if (t == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Aucune tâche active '" + taskDefinitionKey + "' pour " + idRequest);
        }
        return t;
    }

    private static void put(Map<String, Object> m, String k, Object v) {
        if (v != null) m.put(k, v);
    }
}
