package com.example.camundademo.dto;

/** Corps envoyé par le workflow (http-connector) : {"idRequest":"...","status":"..."} */
public record StatusUpdate(String idRequest, String status) {
}
