package it.gov.pagopa.standinmanager;

import com.github.terma.javaniotcpproxy.StaticTcpProxyConfig;
import com.github.terma.javaniotcpproxy.TcpProxy;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.CosmosDBEmulatorContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

@SuppressWarnings("resource")
public class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Integer[] exposedPorts = {8081, 10251, 10252, 10253, 10254};
    private static final List<TcpProxy> startedProxies = new ArrayList<>();
    private static final String regex = "(?s).*Started\r\n$";
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    private static CosmosDBEmulatorContainer emulator = null;

    static {
        try {
            emulator = new CosmosDBEmulatorContainer(
                    DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator"))
                    .withExposedPorts(exposedPorts)
                    .withEnv("AZURE_COSMOS_EMULATOR_IP_ADDRESS_OVERRIDE", InetAddress.getLocalHost().getHostAddress())
                    .withEnv("AZURE_COSMOS_EMULATOR_PARTITION_COUNT", "3")
                    .withEnv("AZURE_COSMOS_EMULATOR_ENABLE_DATA_PERSISTENCE", "true");
            emulator.setWaitStrategy((new LogMessageWaitStrategy())
                    .withRegEx(regex)
                    .withStartupTimeout(Duration.ofSeconds(300L)));
            emulator.start();

            // TCP proxy workaround for Cosmos DB Emulator bug, see: https://github.com/testcontainers/testcontainers-java/issues/5518
            Initializer.startTcpProxy(exposedPorts);

        } catch (Exception e) {
            e.printStackTrace();
            emulator.stop();
        }
    }

    public static CosmosDBEmulatorContainer getEmulator() {
        return emulator;
    }

    public static List<TcpProxy> getStartedProxies() {
        return startedProxies;
    }

    private static void startTcpProxy(Integer... ports) {
        for (Integer port : ports) {
            StaticTcpProxyConfig tcpProxyConfig = new StaticTcpProxyConfig(port, emulator.getHost(), emulator.getMappedPort(port));
            tcpProxyConfig.setWorkerCount(1);
            TcpProxy tcpProxy = new TcpProxy(tcpProxyConfig);
            tcpProxy.start();
            startedProxies.add(tcpProxy);
        }
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            tempFolder.create();
            Path keyStoreFile = tempFolder.newFile("azure-cosmos-emulator.keystore").toPath();
            KeyStore keyStore = null;
            int c=1;
            while(c<10){
                try{
                    keyStore = emulator.buildNewKeyStore();
                    break;
                }catch (Exception e){}
                c++;
                Thread.sleep(10000);
            }

            keyStore.store(new FileOutputStream(keyStoreFile.toFile()), emulator.getEmulatorKey().toCharArray());

            System.setProperty("javax.net.ssl.trustStore", keyStoreFile.toString());
            System.setProperty("javax.net.ssl.trustStorePassword", emulator.getEmulatorKey());
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");

            TestPropertyValues.of(
                    "cosmos.endpoint=" + emulator.getEmulatorEndpoint(),
                    "cosmos.key=" + emulator.getEmulatorKey()
            ).applyTo(applicationContext.getEnvironment());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}