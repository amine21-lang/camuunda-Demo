package com.example.camundademo.dto;

/** Données reçues par le service externe puis transmises au workflow. */
public record StartRequest(
        String idRequest,
        String traderFirstName,
        String traderLastName,
        String traderEmail,
        String marketId,
        String marketName,
        Boolean testMode,              // défaut : true (timers raccourcis)
        Integer testIntervalSeconds) { // défaut : 30 s = 1 "jour ouvré"
}
