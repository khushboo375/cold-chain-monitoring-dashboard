package com.coldchain.dashboard;

import com.coldchain.dashboard.model.ColdChainEvent;
import com.coldchain.dashboard.repository.ColdChainEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:coldchain-tests;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ColdChainDashboardApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ColdChainEventRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.saveAll(List.of(
                event("VAX-001", "North Store", 4.0, "Routine check"),
                event("VAX-002", "South Store", 9.0, "Door left open"),
                event("VAX-003", "East Store", 1.0, null)));
    }

    @Test
    void contextLoads() {
    }

    @Test
    void dashboardSearchIsCaseInsensitiveAndIncludesNotesWhileMetricsUseAllEvents() throws Exception {
        mockMvc.perform(get("/").param("q", "DOOR LEFT").param("status", "all"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasSize(1)))
                .andExpect(model().attribute("totalReadings", 3))
                .andExpect(model().attribute("normalCount", 1L))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("VAX-002")));
    }

    @Test
    void dashboardCombinesSearchAndStatusFiltersAndShowsEmptyState() throws Exception {
        mockMvc.perform(get("/").param("q", "store").param("status", "LOW"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasSize(1)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("VAX-003")));

        mockMvc.perform(get("/").param("q", "missing").param("status", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasSize(0)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "No matching events. Add a reading or change your search.")));
    }

    @Test
    void apiUsesDashboardSearchAndStatusSemantics() throws Exception {
        mockMvc.perform(get("/api/events").param("q", "DOOR LEFT").param("status", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].unitId").value("VAX-002"));

        mockMvc.perform(get("/api/events").param("q", "store").param("status", "low"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].unitId").value("VAX-003"));
    }

    private static ColdChainEvent event(String unitId, String location, double temperature, String notes) {
        ColdChainEvent event = new ColdChainEvent();
        event.setUnitId(unitId);
        event.setLocation(location);
        event.setTemperatureC(temperature);
        event.setMinTempC(2.0);
        event.setMaxTempC(8.0);
        event.setNotes(notes);
        return event;
    }
}
