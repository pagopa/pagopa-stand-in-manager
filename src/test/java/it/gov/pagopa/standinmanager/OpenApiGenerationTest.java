package it.gov.pagopa.standinmanager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.azure.cosmos.CosmosClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.kusto.data.Client;
import it.gov.pagopa.standinmanager.repository.CosmosEventsRepository;
import it.gov.pagopa.standinmanager.repository.DatabaseStationsRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
//@ContextConfiguration(initializers = {Initializer.class})
class OpenApiGenerationTest {

//  private static final CosmosDBEmulatorContainer cosmos = Initializer.getCosmosEmulator();
  @Autowired ObjectMapper objectMapper;
  @Autowired private MockMvc mvc;
  @MockBean Client client;
  @MockBean
  CosmosEventsRepository cosmosEventsRepository;
  @MockBean
  DatabaseStationsRepository databaseStationsRepository;
  @MockBean private EntityManagerFactory entityManagerFactory;
    @MockBean private EntityManager entityManager;

    @MockBean
    DataSource dataSource;
    @MockBean
    CosmosClient cosmosClient;

  @Test
  void swaggerSpringPlugin() throws Exception {
//    cosmos.start();
    mvc.perform(MockMvcRequestBuilders.get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
        .andDo(
            (result) -> {
              assertNotNull(result);
              assertNotNull(result.getResponse());
              final String content = result.getResponse().getContentAsString();
              assertFalse(content.isBlank());
              Object swagger =
                  objectMapper.readValue(result.getResponse().getContentAsString(), Object.class);
              String formatted =
                  objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(swagger);
              Path basePath = Paths.get("openapi/");
              Files.createDirectories(basePath);
              Files.write(basePath.resolve("openapi.json"), formatted.getBytes());
            });
  }
}
