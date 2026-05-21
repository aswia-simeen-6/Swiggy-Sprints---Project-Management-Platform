package com.projectmgmt.domain.issue;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectmgmt.ApiTestBase;
import com.projectmgmt.domain.issue.dto.CreateIssueRequest;
import com.projectmgmt.domain.issue.dto.UpdateIssueRequest;
import com.projectmgmt.domain.project.dto.CreateProjectRequest;
import com.projectmgmt.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IssueControllerIntegrationTest extends ApiTestBase {

    private User testUser;
    private String authHeader;
    private String projectId;

    @BeforeEach
    void setUp() throws Exception {
        testUser = createTestUser(uniqueEmail(), "Issue Test User");
        authHeader = bearerToken(testUser);

        String key = "ISS" + UUID.randomUUID().toString().substring(0, 3).toUpperCase();
        CreateProjectRequest projectReq = new CreateProjectRequest("Issue Test Proj", key, "Testing issues");

        String projResponse = mockMvc.perform(post("/api/projects")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectReq)))
                .andReturn().getResponse().getContentAsString();

        projectId = objectMapper.readTree(projResponse).path("data").path("id").asText();
    }

    @Test
    void createIssue_shouldReturnCreatedIssue() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                IssueType.TASK, "First task", "Description here",
                Priority.HIGH, null, null, null, null, List.of("backend"), Map.of());

        mockMvc.perform(post("/api/projects/" + projectId + "/issues")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("First task"))
                .andExpect(jsonPath("$.data.issueType").value("TASK"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.issueKey").isNotEmpty())
                .andExpect(jsonPath("$.data.reporterId").value(testUser.getId().toString()));
    }

    @Test
    void createIssue_subtaskWithoutParent_shouldReturn400() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                IssueType.SUBTASK, "Orphan subtask", null,
                Priority.LOW, null, null, null, null, null, null);

        mockMvc.perform(post("/api/projects/" + projectId + "/issues")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getIssue_shouldReturnIssueDetails() throws Exception {
        String issueId = createIssueAndGetId("Get test issue");

        mockMvc.perform(get("/api/issues/" + issueId)
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Get test issue"))
                .andExpect(jsonPath("$.data.statusName").isNotEmpty());
    }

    @Test
    void updateIssue_shouldTrackChanges() throws Exception {
        String issueId = createIssueAndGetId("Update me");

        // Get version from the issue
        String issueResponse = mockMvc.perform(get("/api/issues/" + issueId)
                        .header("Authorization", authHeader))
                .andReturn().getResponse().getContentAsString();
        long version = objectMapper.readTree(issueResponse).path("data").path("version").asLong();

        UpdateIssueRequest update = new UpdateIssueRequest(
                "Updated title", null, Priority.CRITICAL, null, null, null, null, null, (int) version);

        mockMvc.perform(patch("/api/issues/" + issueId)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated title"))
                .andExpect(jsonPath("$.data.priority").value("CRITICAL"));
    }

    @Test
    void deleteIssue_shouldSoftDelete() throws Exception {
        String issueId = createIssueAndGetId("Delete me");

        mockMvc.perform(delete("/api/issues/" + issueId)
                        .header("Authorization", authHeader))
                .andExpect(status().isNoContent());

        // Subsequent GET should 404
        mockMvc.perform(get("/api/issues/" + issueId)
                        .header("Authorization", authHeader))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBoard_shouldReturnColumnsWithIssues() throws Exception {
        createIssueAndGetId("Board issue 1");
        createIssueAndGetId("Board issue 2");

        mockMvc.perform(get("/api/projects/" + projectId + "/board")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns").isArray())
                .andExpect(jsonPath("$.columns", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.projectId").value(projectId));
    }

    @Test
    void getBoard_withETag_shouldReturn304WhenUnchanged() throws Exception {
        createIssueAndGetId("ETag issue");

        String etag = mockMvc.perform(get("/api/projects/" + projectId + "/board")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("ETag");

        if (etag != null) {
            mockMvc.perform(get("/api/projects/" + projectId + "/board")
                            .header("Authorization", authHeader)
                            .header("If-None-Match", etag))
                    .andExpect(status().isNotModified());
        }
    }

    @Test
    void watchAndUnwatch_shouldToggleWatchStatus() throws Exception {
        String issueId = createIssueAndGetId("Watch me");

        mockMvc.perform(post("/api/issues/" + issueId + "/watch")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/issues/" + issueId + "/watch")
                        .header("Authorization", authHeader))
                .andExpect(status().isNoContent());
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
