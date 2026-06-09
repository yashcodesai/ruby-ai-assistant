package com.ruby.ruby.dto;

public class ChatResponse {

    private String response;

    private String action;

    public ChatResponse() {
    }

    public ChatResponse(String response) {
        this.response = response;
    }

    public ChatResponse(
            String response,
            String action
    ) {
        this.response = response;
        this.action = action;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}