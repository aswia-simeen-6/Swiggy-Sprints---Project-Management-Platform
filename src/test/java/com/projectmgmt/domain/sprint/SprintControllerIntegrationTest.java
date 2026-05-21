package com.projectmgmt.domain.sprint;

import com.projectmgmt.ApiTestBase;
import com.projectmgmt.domain.issue.IssueType;
import com.projectmgmt.domain.issue.Priority;
import com.projectmgmt.domain.issue.dto.CreateIssueRequest;
import com.projectmgmt.domain.project.dto.CreateProjectRequest;
import com.projectmgmt.domain.sprint.dto.CreateSprintRequest;
import com.projectmgmt.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SprintControllerIntegrationTest extends ApiTestBase {

    private User testUser;
    private String authHeader;
    private String projectId;

    @BeforeEach
    void setUp() throws Exception {
        testUser = createTestUser(uniqueEmail(), "Sprint Test User");
        authHeader = bearerToken(testUser);

        String key = "SPR" + UUID.randomUUID().toString().substring(0, 3).toUpperCase();
        CreateProjectRequest projectReq = new CreateProjectRequest("Sprint Test", key, "Sprint testing");

        String projResponse = mockMvc.perform(post("/api/projects")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectReq)))
                .andReturn().getResponse().getContentAsString();

        projectId = objectMapper.readTree(projResponse).path("data").path("id").asText();
    }

    @Test
    void createSprint_shouldReturnCreatedSprint() throws Exception {
        CreateSprintRequest request = new CreateSprintRequest(
                "Sprint 1", "First sprint goal",
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(14));

        mockMvc.perform(post("/api/projects/" + projectId + "/sprints")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Sprint 1"))
                .andExpect(jsonPath("$.data.status").value("PLANNED"));
    }

    @Test
    void listSprints_shouldReturnProjectSprints() throws Exception {
        createSprintAndGetId("List Sprint");

        mockMvc.perform(get("/api/projects/" + projectId + "/sprints")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void startSprint_shouldTransitionToActive() throws Exception {
        String sprintId = createSprintAndGetId("Start Sprint");

        mockMvc.perform(post("/api/sprints/" + sprintId + "/start")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void getVelocity_shouldReturnCompletedSprints() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId + "/velocity")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    private String createSprintAndGetId(String name) throws Exception {
        CreateSprintRequest request = new CreateSprintRequest(
                name, "Goal", LocalDate.now().plusDays(1), LocalDate.now().plusDays(14));

        String response = mockMvc.perform(post("/api/projects/" + projectId + "/sprints")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asText();
    }
}
