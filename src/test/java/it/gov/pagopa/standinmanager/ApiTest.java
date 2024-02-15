package it.gov.pagopa.standinmanager;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.azure.cosmos.CosmosClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.kusto.data.Client;
import it.gov.pagopa.standinmanager.model.AppInfo;
import it.gov.pagopa.standinmanager.model.GetResponse;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.repository.CosmosStationRepository;
import it.gov.pagopa.standinmanager.repository.DatabaseStationsRepository;
import it.gov.pagopa.standinmanager.repository.model.CosmosStandInStation;
import java.time.Instant;
import java.util.Arrays;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class ApiTest {

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private MockMvc mvc;

    @MockBean private Client client;
    @MockBean private CosmosStationRepository cosmosStationRepository;
    @MockBean private CosmosEventsRepository cosmosEventsRepository;
    @MockBean private DatabaseStationsRepository databaseStationsRepository;
    @MockBean private EntityManagerFactory entityManagerFactory;
    @MockBean private EntityManager entityManager;
    @MockBean private DataSource dataSource;
    @MockBean private CosmosClient cosmosClient;
    @Test
    void swagger() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(UriComponentsBuilder.fromUriString("").build().toUri()).accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
    }
    @Test
    void info() throws Exception {
        when(cosmosStationRepository.getStations()).thenReturn(Arrays.asList(new CosmosStandInStation("","station1", Instant.now()),new CosmosStandInStation("","station2", Instant.now())));
        mvc.perform(MockMvcRequestBuilders.get("/info").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andDo(
                        (result) -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                            final String content = result.getResponse().getContentAsString();
                            assertFalse(content.isBlank());
                            AppInfo res =
                                    objectMapper.readValue(result.getResponse().getContentAsString(), AppInfo.class);
                            assertEquals(res.getEnvironment(), "test");
                        });

    }
    @Test
    void stations() throws Exception {
        when(cosmosStationRepository.getStations()).thenReturn(Arrays.asList(new CosmosStandInStation("","station1", Instant.now()),new CosmosStandInStation("","station2", Instant.now())));
        mvc.perform(MockMvcRequestBuilders.get("/stations").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andDo(
                        (result) -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                            final String content = result.getResponse().getContentAsString();
                            assertFalse(content.isBlank());
                            GetResponse res =
                                    objectMapper.readValue(result.getResponse().getContentAsString(), GetResponse.class);
                            assertEquals(res.getStations().size(), 2);
                        });

    }
}
