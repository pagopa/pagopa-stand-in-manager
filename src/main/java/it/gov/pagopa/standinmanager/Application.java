package it.gov.pagopa.standinmanager;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.microsoft.azure.kusto.data.Client;
import com.microsoft.azure.kusto.data.ClientFactory;
import com.microsoft.azure.kusto.data.auth.ConnectionStringBuilder;
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

import java.net.URISyntaxException;
import java.time.Duration;
import java.util.TimeZone;

@SpringBootApplication
public class Application {

    @Value("${api-config-cache.base-path}")
    private String basePath;
    @Value("${api-config-cache.api-key}")
    private String apiKey;
    @Value("${dataexplorer.url}")
    private String dataExplorerUrl;
    @Value("${dataexplorer.clientId}")
    private String dataExplorerClientId;
    @Value("${dataexplorer.appKey}")
    private String dataExplorerKey;
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
        setTimezone(args);
        SpringApplication.run(Application.class, args);
    }

    private static void setTimezone(String[] args) {
        for (String arg : args) {
            String[] argSections = arg.split("=");
            if (argSections[0].contains("user.timezone")) {
                TimeZone.setDefault(TimeZone.getTimeZone(argSections[1]));
                return;
            }
        }
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
    public Client getClient() throws URISyntaxException {
        return ClientFactory.createClient(
                ConnectionStringBuilder.createWithAadManagedIdentity(dataExplorerUrl));
    }

    @Bean
    public CosmosClient getCosmosClient() {
        return new CosmosClientBuilder().endpoint(cosmosEndpoint).key(cosmosKey).buildClient();
    }
}
