package it.gov.pagopa.standinmanager;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import it.gov.pagopa.standinmanager.config.ApiClient;
import org.openapitools.client.api.CacheApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

import java.time.Duration;

@SpringBootApplication
public class Application {

  @Value("${api-config-cache.base-path}")
  private String basePath;

  @Value("${api-config-cache.api-key}")
  private String apiKey;

  @Value("${cosmos.endpoint}")
  private String cosmosEndpoint;

  @Value("${cosmos.key}")
  private String cosmosKey;

  @Value("${aws.region}")
  private String region;

  @Value("${forwarder.connectionTimeout}")
  private Integer forwarderConnectTimeout;

  @Value("${forwarder.readTimeout}")
  private Integer forwarderReadTimeout;

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  public SesClient sesClient() {
    return SesClient.builder().region(Region.of(region)).build();
  }

  @Bean
  public CacheApi cacheApi() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(basePath);
    apiClient.setApiKey(apiKey);
    return new CacheApi(apiClient);
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .setReadTimeout(Duration.ofSeconds(forwarderReadTimeout))
        .setConnectTimeout(Duration.ofSeconds(forwarderConnectTimeout))
        .build();
  }

  @Bean
  public CosmosClient getCosmosClient() {
    return new CosmosClientBuilder().endpoint(cosmosEndpoint).key(cosmosKey).buildClient();
  }
}
