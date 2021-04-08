package com.example.jira.chatbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
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

import java.io.IOException;


@SpringBootApplication
@RestController
public class JiraChatBotApplication {
    @Value("${jira.url}")
    private String jiraUrl;
    @Value("${user.id}")
    private String userId;
    @Value("${user.token}")
    private String token;
    @Value("${project.name}")
    private String projectName;

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
                    .queryString("fields", "key,issuetype,summary,status,versions,fixVersions,labels,project,assignee,priority")
                    .queryString("fieldsByKeys", "true")
                    .asJson();

            queryResponse = fetchFieldsJson(response);

        } catch (Exception e) {
            System.out.println("Exception:" + e.getMessage());
            return "{\"Exception\" : \"" + e.getMessage() + "\"}";
        }
        return String.valueOf(queryResponse);
    }

    private JSONArray fetchFieldsJson(HttpResponse<JsonNode> response) {
        JSONArray jsonResponse = new JSONArray();
        try {
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
        } catch (Exception e) {
            jsonResponse.put(e.getMessage());
        }

        return jsonResponse;
    }

    @GetMapping("/create")
    public void createIssue() throws UnirestException {
        // The payload definition using the Jackson library
        JsonNodeFactory jnf = JsonNodeFactory.instance;
        ObjectNode payload = jnf.objectNode();
        {
            ObjectNode update = payload.putObject("update");
            {
            }
            ObjectNode fields = payload.putObject("fields");
            {
                fields.put("summary", "Create Ticket Test 1");

                ObjectNode issuetype = fields.putObject("issuetype");
                {
                    issuetype.put("id", "1");
                }

                ObjectNode project = fields.putObject("project");
                {
                    project.put("id", "26914");
                }
                ObjectNode description = fields.putObject("description");
                {
                    description.put("type", "doc");
                    description.put("version", 1);
                    ArrayNode content = description.putArray("content");
                    ObjectNode content0 = content.addObject();
                    {
                        content0.put("type", "paragraph");
                        content = content0.putArray("content");
                        content0 = content.addObject();
                        {
                            content0.put("text", "Order entry fails when selecting supplier.");
                            content0.put("type", "text");
                        }
                    }
                }
                ObjectNode reporter = fields.putObject("reporter");
                {
                    reporter.put("id", "557058:7d269933-7525-46a1-8fea-a35b6caa1205");
                }

                ObjectNode priority = fields.putObject("priority");
                {
                    priority.put("name", "Should");
                }

                ObjectNode assignee = fields.putObject("assignee");
                {
                    assignee.put("displayName", "Akshay Rokade");
                }
            }
        }

// Connect Jackson ObjectMapper to Unirest
        Unirest.setObjectMapper(new ObjectMapper() {
            private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper
                    = new com.fasterxml.jackson.databind.ObjectMapper();

            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return jacksonObjectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object value) {
                try {
                    return jacksonObjectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });

// This code sample uses the  'Unirest' library:
// http://unirest.io/java.html
        HttpResponse<JsonNode> response = Unirest.post(jiraUrl + "/rest/api/3/issue")
                .basicAuth(userId, token)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .body(payload)
                .asJson();

        System.out.println(response.getBody());
    }

    private GetRequest getConnection() {
        return Unirest.get(jiraUrl + "/rest/api/3/search")
                .basicAuth(userId, token)
                .header("Accept", "application/json");
    }
}
