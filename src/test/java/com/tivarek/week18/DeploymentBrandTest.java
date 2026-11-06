package com.tivarek.week18;

import com.tivarek.TivarekApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TivarekApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeploymentBrandTest {
    @Autowired MockMvc mvc;

    @Test void homepageDisplaysRealContactAndLogo() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("0335 135 549")))
            .andExpect(content().string(containsString("tivarek@gmail.com")))
            .andExpect(content().string(org.hamcrest.Matchers.matchesRegex("(?s).*/images/tivarek-wordmark-[a-f0-9]{32}\\.png.*")));
    }

    @Test void publiclyAccessibleBrandImages() throws Exception {
        mvc.perform(get("/images/tivarek-icon.png"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"));
        mvc.perform(get("/images/tivarek-wordmark.png"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"));
    }

    @Test void healthEndpointShowsOnlyMinimalStatus() throws Exception {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test void sensitiveActuatorEndpointsAreNotPublic() throws Exception {
        mvc.perform(get("/actuator/env"))
            .andExpect(result -> assertNotEquals(200, result.getResponse().getStatus()));
    }
}
