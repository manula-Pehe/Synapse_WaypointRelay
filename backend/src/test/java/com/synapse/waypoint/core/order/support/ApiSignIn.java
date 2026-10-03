package com.synapse.waypoint.core.order.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Signs in through the real login endpoint and returns the Authorization header value. */
public final class ApiSignIn {

    private static final String TOKEN_FIELD = "\"token\":\"";

    private ApiSignIn() {
    }

    public static String bearer(MockMvc mvc, String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + email + "\",\"secret\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int start = body.indexOf(TOKEN_FIELD) + TOKEN_FIELD.length();
        return "Bearer " + body.substring(start, body.indexOf('"', start));
    }
}
