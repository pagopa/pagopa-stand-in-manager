package it.gov.pagopa.standinmanager; // TODO: refactor the package

import com.microsoft.azure.kusto.data.Client;
import com.microsoft.azure.kusto.data.ClientFactory;
import com.microsoft.azure.kusto.data.auth.ConnectionStringBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.net.URISyntaxException;

@SpringBootApplication(
)
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Value("${dataexplorer.url}")
  private String dataExplorerUrl;

  @Value("${dataexplorer.clientId}")
  private String dataExplorerClientId;

  @Value("${dataexplorer.appKey}")
  private String dataExplorerKey;

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.build();
  }

  @Bean
  public Client getClient() throws URISyntaxException {
    return ClientFactory.createClient(ConnectionStringBuilder.createWithAadManagedIdentity(dataExplorerUrl));
  }

}
