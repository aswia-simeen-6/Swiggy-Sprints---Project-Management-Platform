package com.projectmgmt.domain.project;

import com.projectmgmt.ApiTestBase;
import com.projectmgmt.domain.project.dto.CreateProjectRequest;
import com.projectmgmt.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProjectControllerIntegrationTest extends ApiTestBase {

    private User testUser;
    private String authHeader;

    @BeforeEach
    void setUp() {
        testUser = createTestUser(uniqueEmail(), "Project Test User");
        authHeader = bearerToken(testUser);
    }

    @Test
    void createProject_shouldReturnCreatedProject() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("My Project", "MYPRJ", "A test project");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("My Project"))
                .andExpect(jsonPath("$.data.key").value("MYPRJ"));
    }

    @Test
    void createProject_duplicateKey_shouldReturn409() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("First Project", "DUP01", "First");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second with same key
        CreateProjectRequest duplicate = new CreateProjectRequest("Second Project", "DUP01", "Second");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict());
    }

    @Test
    void getProject_asMember_shouldReturn200() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("Get Project", "GETP1", "Get test");

        String responseBody = mockMvc.perform(post("/api/projects")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        String projectId = objectMapper.readTree(responseBody).path("data").path("id").asText();

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Get Project"))
                .andExpect(jsonPath("$.data.key").value("GETP1"));
    }

    @Test
    void getProject_asNonMember_shouldReturn403() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("Private Proj", "PRIV1", "Private");

        String responseBody = mockMvc.perform(post("/api/projects")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        String projectId = objectMapper.readTree(responseBody).path("data").path("id").asText();

        // Different user
        User otherUser = createTestUser(uniqueEmail(), "Other User");
        String otherAuth = bearerToken(otherUser);

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", otherAuth))
                .andExpect(status().isForbidden());
    }

    @Test
    void listProjects_shouldReturnUserProjects() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("List Project", "LIST1", "List test");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void createProject_unauthenticated_shouldReturn401() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("No Auth", "NOAU1", "No auth test");

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
