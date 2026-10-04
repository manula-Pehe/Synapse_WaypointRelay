package com.synapse.waypoint.core.file;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.file.entity.FileKind;
import com.synapse.waypoint.core.file.service.FileService;
import com.synapse.waypoint.core.order.support.ApiSignIn;

/** GET /api/files/{id} over HTTP. Users and bytes are invented. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FileApiTests {

    private static final String PASSWORD = "Test-Password-1";
    private static final byte[] PNG_BYTES = {7, 8, 9, 10};

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired FileService files;

    @Test
    void shouldStreamTheBytesWithTypeAndPrivateCaching() throws Exception {
        String id = files.store(PNG_BYTES, "image/png", FileKind.SIGNATURE, null);

        mvc.perform(get("/api/files/" + id).header(HttpHeaders.AUTHORIZATION, signedIn("DRIVER")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=86400, private"))
                .andExpect(content().bytes(PNG_BYTES));
    }

    @Test
    void shouldServeEveryRole() throws Exception {
        String id = files.store(PNG_BYTES, "image/png", FileKind.PHOTO, null);

        for (String role : new String[] {"STORE_MANAGER", "DISPATCHER", "LOADER", "DRIVER"}) {
            mvc.perform(get("/api/files/" + id).header(HttpHeaders.AUTHORIZATION, signedIn(role)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldReturn404ForAnUnknownId() throws Exception {
        mvc.perform(get("/api/files/f-unknown").header(HttpHeaders.AUTHORIZATION, signedIn("DISPATCHER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn401WithoutAToken() throws Exception {
        String id = files.store(PNG_BYTES, "image/png", FileKind.PHOTO, null);

        mvc.perform(get("/api/files/" + id))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String signedIn(String role) throws Exception {
        String email = "files." + role.toLowerCase() + "@example.lk";
        jdbc.update("""
                INSERT INTO users (id, name, role, email, secret_hash, active)
                VALUES (?, ?, ?, ?, ?, true)""", "usr-files-" + role, "Test " + role, role, email,
                encoder.encode(PASSWORD));
        return ApiSignIn.bearer(mvc, email, PASSWORD);
    }
}
