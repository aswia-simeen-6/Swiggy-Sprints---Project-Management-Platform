package com.projectmgmt.common;

import com.projectmgmt.ApiTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HealthControllerIntegrationTest extends ApiTestBase {

    @Test
    void health_shouldReturnUpStatus() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.redis").value("UP"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
