package com.example.camundademo.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

/** "Base de données" du service externe simulé : statut + historique par idRequest. */
@Component
public class RequestStatusStore {

    public record Entry(String status, Instant at) {
    }

    private final Map<String, List<Entry>> history = new ConcurrentHashMap<>();
    private final AtomicBoolean failMode = new AtomicBoolean(false);

    public void record(String idRequest, String status) {
        history.computeIfAbsent(idRequest, k -> new CopyOnWriteArrayList<>()).add(new Entry(status, Instant.now()));
    }

    public String current(String idRequest) {
        List<Entry> l = history.get(idRequest);
        return (l == null || l.isEmpty()) ? null : l.get(l.size() - 1).status();
    }

    public List<Entry> history(String idRequest) {
        return new ArrayList<>(history.getOrDefault(idRequest, List.of()));
    }

    public Map<String, String> all() {
        Map<String, String> m = new LinkedHashMap<>();
        history.keySet().stream().sorted().forEach(k -> m.put(k, current(k)));
        return m;
    }

    public boolean isFailMode() {
        return failMode.get();
    }

    public void setFailMode(boolean enabled) {
        failMode.set(enabled);
    }
}
