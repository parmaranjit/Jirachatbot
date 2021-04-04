package com.example.jira.chatbot;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import org.json.JSONArray;
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
    private String projectName = "CSSUPINT";

    public static void main(String[] args) {
        SpringApplication.run(JiraChatBotApplication.class, args);
    }

    @GetMapping("/jira")
    public String getQuestion(@RequestParam(value = "ask", defaultValue = "Work is in progress...") String ask) {
        JSONArray queryResponse = null;
        try {
            HttpResponse<JsonNode> response = null;
            response = getConnection()
                    .queryString("jql", "project = " + projectName + " AND text ~ \"" + ask + "\"")
                    .queryString("maxResults", "5")
                    .queryString("fields", "key,summary,status,versions,fixVersions,labels")
                    .queryString("fieldsByKeys", "true")
                    .asJson();

            queryResponse = fetchFieldsJson(response);
        } catch (UnirestException e) {
            System.out.println("Exception:" + e.getMessage());
        }
        return String.valueOf(queryResponse);
    }

    private JSONArray fetchFieldsJson(HttpResponse<JsonNode> response) {
        JSONArray jsonResponse = new JSONArray();
        JSONArray issues = (JSONArray) (response.getBody().getObject()).get("issues");
        for (Object itr : issues) {
            String ticketId;
            String ticketSummary;
            String ticketState;
            StringBuilder ticketCSFixVersion = new StringBuilder();
            JSONObject jsonObject = new JSONObject();

            JSONObject issue = (JSONObject) itr;
            ticketId = (String) issue.get("key");
            JSONObject fields = (JSONObject) issue.get("fields");
            ticketSummary = (String) fields.get("summary");
            JSONObject status = (JSONObject) fields.get("status");
            ticketState = (String) status.get("name");
            if (ticketState.equalsIgnoreCase("Closed")) {
                JSONArray fixVersions = (JSONArray) fields.get("fixVersions");
                for (Object versionObject : fixVersions) {
                    JSONObject fixVersion = (JSONObject) versionObject;
                    ticketCSFixVersion.append(fixVersion.get("name")).append(" ");
                }
            }


            jsonObject.put("id", jiraUrl + "/browse/" + ticketId);
            jsonObject.put("summary", ticketSummary);
            jsonObject.put("state", ticketState);
            if (ticketCSFixVersion.length() > 0) {
                jsonObject.put("fixVersions", ticketCSFixVersion.toString().trim());
            }
            jsonResponse.put(jsonObject);
        }

        return jsonResponse;
    }

    private GetRequest getConnection() {
        return Unirest.get(jiraUrl + "/rest/api/3/search")
                .basicAuth(userId, token)
                .header("Accept", "application/json");
    }
}
