package com.projectmgmt.domain.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectmgmt.ApiTestBase;
import com.projectmgmt.domain.issue.IssueType;
import com.projectmgmt.domain.issue.Priority;
import com.projectmgmt.domain.issue.dto.CreateIssueRequest;
import com.projectmgmt.domain.issue.dto.TransitionRequest;
import com.projectmgmt.domain.project.dto.CreateProjectRequest;
import com.projectmgmt.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkflowIntegrationTest extends ApiTestBase {

    private User testUser;
    private String authHeader;
    private String projectId;

    @BeforeEach
    void setUp() throws Exception {
        testUser = createTestUser(uniqueEmail(), "Workflow Test User");
        authHeader = bearerToken(testUser);

        String key = "WF" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        CreateProjectRequest projectReq = new CreateProjectRequest("Workflow Test", key, "Workflow testing");

        String projResponse = mockMvc.perform(post("/api/projects")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectReq)))
                .andReturn().getResponse().getContentAsString();

        projectId = objectMapper.readTree(projResponse).path("data").path("id").asText();
    }

    @Test
    void getStatuses_shouldReturnDefaultWorkflow() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId + "/workflow/statuses")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(4))) // To Do, In Progress, In Review, Done
                .andExpect(jsonPath("$.data[0].name").value("To Do"));
    }

    @Test
    void getTransitions_shouldReturnDefaultTransitions() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId + "/workflow/transitions")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(4))));
    }

    @Test
    void transitionIssue_validTransition_shouldChangeStatus() throws Exception {
        // Create issue (starts in To Do)
        String issueId = createIssueAndGetId("Transition me");

        // Get allowed transitions
        String transitionsResponse = mockMvc.perform(get("/api/issues/" + issueId + "/transitions")
                        .header("Authorization", authHeader))
                .andReturn().getResponse().getContentAsString();

        JsonNode transitions = objectMapper.readTree(transitionsResponse).path("data");
        if (transitions.isArray() && !transitions.isEmpty()) {
            String targetStatusId = transitions.get(0).path("id").asText();

            TransitionRequest transitionReq = new TransitionRequest(UUID.fromString(targetStatusId));

            mockMvc.perform(post("/api/issues/" + issueId + "/transitions")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transitionReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.statusId").value(targetStatusId));
        }
    }

    @Test
    void transitionIssue_invalidTransition_shouldReturn400() throws Exception {
        String issueId = createIssueAndGetId("Bad transition");

        // Try to transition to a random UUID (not a valid status)
        TransitionRequest request = new TransitionRequest(UUID.randomUUID());

        mockMvc.perform(post("/api/issues/" + issueId + "/transitions")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private String createIssueAndGetId(String title) throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                IssueType.TASK, title, null, Priority.MEDIUM,
                null, null, null, null, null, null);

        String response = mockMvc.perform(post("/api/projects/" + projectId + "/issues")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asText();
    }
}
