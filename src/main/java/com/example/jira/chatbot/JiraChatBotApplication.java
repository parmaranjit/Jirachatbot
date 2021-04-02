package com.example.jira.chatbot;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@SpringBootApplication
@RestController
public class JiraChatBotApplication {
    @Value("${jira.url}")
    private String jiraUrl;
    @Value("${user.id}")
    private String userId;
    @Value("${user.token}")
    private String token;

    public static void main(String[] args) {
        SpringApplication.run(JiraChatBotApplication.class, args);
    }

    @GetMapping("/jira")
    public String getQuestion(@RequestParam(value = "ask", defaultValue = "Work is in progress...") String ask) {
        HttpResponse<JsonNode> response = null;
        try {
            response = Unirest.get(jiraUrl + "/rest/api/3/search")
                    .basicAuth(userId, token)
                    .header("Accept", "application/json")
                    .queryString("jql", "text ~ \"" + ask + "\"")
                    .queryString("maxResults", "5")
                    .queryString("fields", "key,summary,status")
                    .queryString("fieldsByKeys", "true")
                    .asJson();
        } catch (UnirestException e) {
            System.out.println("Exception:" + e.getMessage());
        }

        System.out.println(response.getBody());
        return response.getBody().toString();
    }

}
