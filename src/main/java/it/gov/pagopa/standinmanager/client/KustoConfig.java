package it.gov.pagopa.standinmanager.client;

import com.microsoft.azure.kusto.data.Client;
import com.microsoft.azure.kusto.data.ClientFactory;
import com.microsoft.azure.kusto.data.auth.ConnectionStringBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;

@Configuration
public class KustoConfig {

    private final String dataExplorerUrl;
    private final String dataExplorerClientId;
    private final String dataExplorerClientKey;

    public KustoConfig(
            @Value("${data.explorer.url}") String dataExplorerUrl,
            @Value("${data.explorer.clientId}") String dataExplorerClientId,
            @Value("${data.explorer.clientKey}") String dataExplorerClientKey
    ) {
        this.dataExplorerUrl = dataExplorerUrl;
        this.dataExplorerClientId = dataExplorerClientId;
        this.dataExplorerClientKey = dataExplorerClientKey;
    }

    @Bean
    public Client getClient() throws URISyntaxException {
        return ClientFactory.createClient(
                ConnectionStringBuilder
                        .createWithAadApplicationCredentials(dataExplorerUrl, dataExplorerClientId, dataExplorerClientKey)
        );
    }
}
