package com.projectmgmt.domain.comment;

import com.projectmgmt.ApiTestBase;
import com.projectmgmt.domain.comment.dto.CreateCommentRequest;
import com.projectmgmt.domain.issue.IssueType;
import com.projectmgmt.domain.issue.Priority;
import com.projectmgmt.domain.issue.dto.CreateIssueRequest;
import com.projectmgmt.domain.project.dto.CreateProjectRequest;
import com.projectmgmt.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommentControllerIntegrationTest extends ApiTestBase {

    private User testUser;
    private String authHeader;
    private String issueId;

    @BeforeEach
    void setUp() throws Exception {
        testUser = createTestUser(uniqueEmail(), "Comment Test User");
        authHeader = bearerToken(testUser);

        String key = "CMT" + UUID.randomUUID().toString().substring(0, 3).toUpperCase();
        CreateProjectRequest projectReq = new CreateProjectRequest("Comment Test", key, "Comment testing");

        String projResponse = mockMvc.perform(post("/api/projects")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectReq)))
                .andReturn().getResponse().getContentAsString();

        String projectId = objectMapper.readTree(projResponse).path("data").path("id").asText();

        CreateIssueRequest issueReq = new CreateIssueRequest(
                IssueType.TASK, "Comment target", null, Priority.MEDIUM,
                null, null, null, null, null, null);

        String issueResponse = mockMvc.perform(post("/api/projects/" + projectId + "/issues")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueReq)))
                .andReturn().getResponse().getContentAsString();

        issueId = objectMapper.readTree(issueResponse).path("data").path("id").asText();
    }

    @Test
    void addComment_shouldCreateComment() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("This is a comment", null);

        mockMvc.perform(post("/api/issues/" + issueId + "/comments")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.body").value("This is a comment"))
                .andExpect(jsonPath("$.data.authorId").value(testUser.getId().toString()));
    }

    @Test
    void addComment_withMention_shouldCreateComment() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest(
                "Hey @" + testUser.getId() + " check this out", null);

        mockMvc.perform(post("/api/issues/" + issueId + "/comments")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.body", containsString("@")));
    }

    @Test
    void listComments_shouldReturnThreadedComments() throws Exception {
        // Add parent comment
        CreateCommentRequest parent = new CreateCommentRequest("Parent comment", null);
        String parentResponse = mockMvc.perform(post("/api/issues/" + issueId + "/comments")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parent)))
                .andReturn().getResponse().getContentAsString();

        String parentId = objectMapper.readTree(parentResponse).path("data").path("id").asText();

        // Add reply
        CreateCommentRequest reply = new CreateCommentRequest("Reply comment", UUID.fromString(parentId));
        mockMvc.perform(post("/api/issues/" + issueId + "/comments")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reply)))
                .andExpect(status().isCreated());

        // List comments - should show threaded structure
        mockMvc.perform(get("/api/issues/" + issueId + "/comments")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void deleteComment_asAuthor_shouldSucceed() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("Delete me", null);
        String response = mockMvc.perform(post("/api/issues/" + issueId + "/comments")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        String commentId = objectMapper.readTree(response).path("data").path("id").asText();

        mockMvc.perform(delete("/api/issues/" + issueId + "/comments/" + commentId)
                        .header("Authorization", authHeader))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteComment_asOtherUser_shouldReturn403() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("Not your comment", null);
        String response = mockMvc.perform(post("/api/issues/" + issueId + "/comments")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        String commentId = objectMapper.readTree(response).path("data").path("id").asText();

        // Different user tries to delete
        User otherUser = createTestUser(uniqueEmail(), "Other Commenter");
        String otherAuth = bearerToken(otherUser);

        mockMvc.perform(delete("/api/issues/" + issueId + "/comments/" + commentId)
                        .header("Authorization", otherAuth))
                .andExpect(status().isForbidden());
    }
}
