package com.taskflow.alwaysinprogressbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.alwaysinprogressbackend.repository.ProjectRepository;
import com.taskflow.alwaysinprogressbackend.repository.TaskRepository;
import com.taskflow.alwaysinprogressbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskFlowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerAndLoginReturnsJwtAndStoresHashedPassword() throws Exception {
        register("Jane Doe", "jane@example.com", "password123");

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "jane@example.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();

        assertThat(token).isNotBlank();
        assertThat(userRepository.findByEmail("jane@example.com"))
                .isPresent()
                .get()
                .extracting("password")
                .isNotEqualTo("password123");
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void projectTaskFilteringPaginationAndStatsWorkEndToEnd() throws Exception {
        String ownerToken = registerAndLogin("Owner", "owner@example.com", "password123");
        String assigneeToken = registerAndLogin("Assignee", "assignee@example.com", "password123");
        String assigneeId = jwtUserId(assigneeToken);

        String projectId = createProject(ownerToken, "Website Redesign");

        createTask(ownerToken, projectId, "Design homepage", "TODO", "HIGH", assigneeId);
        createTask(ownerToken, projectId, "Build API", "IN_PROGRESS", "MEDIUM", assigneeId);
        createTask(ownerToken, projectId, "Ship docs", "DONE", "LOW", null);

        mockMvc.perform(get("/projects/{projectId}/tasks", projectId)
                        .header("Authorization", bearer(ownerToken))
                        .param("status", "TODO")
                        .param("page", "0")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Design homepage"))
                .andExpect(jsonPath("$[0].status").value("TODO"));

        mockMvc.perform(get("/projects/{projectId}/stats", projectId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCounts.TODO").value(1))
                .andExpect(jsonPath("$.statusCounts.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.statusCounts.DONE").value(1))
                .andExpect(jsonPath("$.assigneeCounts['" + assigneeId + "']").value(2))
                .andExpect(jsonPath("$.assigneeCounts.UNASSIGNED").value(1));
    }

    @Test
    void registerValidationReturnsStructuredFieldErrors() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "",
                                "email", "not-an-email",
                                "password", ""
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation failed"))
                .andExpect(jsonPath("$.fields.name").value("Name is required"))
                .andExpect(jsonPath("$.fields.email").value("Invalid email"))
                .andExpect(jsonPath("$.fields.password").value("Password is required"));
    }

    @Test
    void duplicateRegistrationReturnsBadRequest() throws Exception {
        register("Jane Doe", "jane@example.com", "password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Jane Again",
                                "email", "jane@example.com",
                                "password", "password456"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("user already exists"));
    }

    @Test
    void invalidLoginReturnsUnauthorized() throws Exception {
        register("Jane Doe", "jane@example.com", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "jane@example.com",
                                "password", "wrong-password"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid credentials"));
    }

    @Test
    void projectUpdateByNonOwnerReturnsForbidden() throws Exception {
        String ownerToken = registerAndLogin("Owner", "owner@example.com", "password123");
        String otherToken = registerAndLogin("Other", "other@example.com", "password123");
        String projectId = createProject(ownerToken, "Private Project");

        mockMvc.perform(patch("/projects/{projectId}", projectId)
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Hijacked",
                                "description", "Should not update"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void missingProjectReturnsNotFound() throws Exception {
        String token = registerAndLogin("Owner", "owner@example.com", "password123");

        mockMvc.perform(patch("/projects/{projectId}", UUID.randomUUID())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Missing",
                                "description", "No project here"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not found"));
    }

    @Test
    void invalidPathParameterReturnsBadRequest() throws Exception {
        String token = registerAndLogin("Owner", "owner@example.com", "password123");

        mockMvc.perform(get("/projects/not-a-uuid")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid parameter"));
    }

    @Test
    void creatingTaskWithUnknownAssigneeReturnsBadRequest() throws Exception {
        String ownerToken = registerAndLogin("Owner", "owner@example.com", "password123");
        String projectId = createProject(ownerToken, "Website Redesign");

        mockMvc.perform(post("/projects/{projectId}/tasks", projectId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Assign impossible task",
                                "description", "Assignee does not exist",
                                "status", "TODO",
                                "priority", "HIGH",
                                "assigneeId", UUID.randomUUID().toString()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid assignee"));
    }

    @Test
    void taskUpdateAndDeleteWorkEndToEnd() throws Exception {
        String ownerToken = registerAndLogin("Owner", "owner@example.com", "password123");
        String projectId = createProject(ownerToken, "Website Redesign");
        String taskId = createTask(ownerToken, projectId, "Draft copy", "TODO", "LOW", null);

        mockMvc.perform(patch("/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Publish copy",
                                "status", "DONE",
                                "priority", "HIGH"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Publish copy"))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        mockMvc.perform(delete("/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Task deleted successfully"));

        assertThat(taskRepository.existsById(UUID.fromString(taskId))).isFalse();
    }

    @Test
    void assigneeCannotDeleteTaskCreatedBySomeoneElse() throws Exception {
        String ownerToken = registerAndLogin("Owner", "owner@example.com", "password123");
        String assigneeToken = registerAndLogin("Assignee", "assignee@example.com", "password123");
        String assigneeId = jwtUserId(assigneeToken);
        String projectId = createProject(ownerToken, "Website Redesign");
        String taskId = createTask(ownerToken, projectId, "Assigned task", "TODO", "HIGH", assigneeId);

        mockMvc.perform(delete("/tasks/{taskId}", taskId)
                        .header("Authorization", bearer(assigneeToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));

        assertThat(taskRepository.existsById(UUID.fromString(taskId))).isTrue();
    }

    @Test
    void projectDeleteRemovesProjectAndTasks() throws Exception {
        String ownerToken = registerAndLogin("Owner", "owner@example.com", "password123");
        String projectId = createProject(ownerToken, "Website Redesign");
        String taskId = createTask(ownerToken, projectId, "Draft copy", "TODO", "LOW", null);

        mockMvc.perform(delete("/projects/{projectId}", projectId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Project deleted"));

        assertThat(projectRepository.existsById(UUID.fromString(projectId))).isFalse();
    }

    private String registerAndLogin(String name, String email, String password) throws Exception {
        register(name, email, password);

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private void register(String name, String email, String password) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", name,
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk());
    }

    private String createProject(String token, String name) throws Exception {
        String response = mockMvc.perform(post("/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", name,
                                "description", "Q2 launch work"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    private String createTask(String token,
                              String projectId,
                              String title,
                              String status,
                              String priority,
                              String assigneeId) throws Exception {

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", title);
        body.put("description", title + " description");
        body.put("status", status);
        body.put("priority", priority);
        body.put("assigneeId", assigneeId);

        String response = mockMvc.perform(post("/projects/{projectId}/tasks", projectId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    private String jwtUserId(String token) throws Exception {
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        JsonNode claims = objectMapper.readTree(payload);
        return claims.get("userId").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
