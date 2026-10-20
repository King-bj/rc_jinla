package com.notifyhub.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocalMockController.class)
class LocalMockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void post_returnsAccepted() throws Exception {
        mockMvc.perform(post("/mock/crm/contacts/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"PAY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true));
    }
}
