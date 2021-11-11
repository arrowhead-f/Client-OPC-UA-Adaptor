package eu.arrowhead.client.skeleton.provider.OPC_UA;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

/**
 * This class is used to create a connection to an endpoint.
 * @author Niklas Karvonen
 */
public class OPCUAConnection {
    private String address;
    private OpcUaClient client;

    public OPCUAConnection(String address) {
        try {
            client = createClientAndConnect(address);
        } catch (Exception e) {
            System.out.println("Fatal error: Could not create OPC-UA connection. Please check OPC-Server endpoint.");
            //System.exit(69);
        }
    }

    public void dispose() {
        client.disconnect();
    }

    public OpcUaClient getConnectedClient() {
        return client;
    }

    private OpcUaClient createClientAndConnect(String address) throws Exception {
        File securityTempDir = new File(System.getProperty("java.io.tmpdir"), "security");
        if (!securityTempDir.exists() && !securityTempDir.mkdirs()) {
            throw new Exception("unable to create security dir: " + securityTempDir);
        }
       // LoggerFactory.getLogger(getClass())
         //       .info("security temp dir: {}", securityTempDir.getAbsolutePath());

        SecurityPolicy securityPolicy = SecurityPolicy.None;

        EndpointDescription[] endpoints;

        String url = "opc.tcp://" + address;
            try {
                endpoints = UaTcpStackClient
                        .getEndpoints(url)
                        .get();
            } catch (Throwable ex) {
                // try the explicit discovery endpoint as well
                String discoveryUrl = url + "/discovery";
                System.out.println("Trying explicit discovery URL: " + discoveryUrl);
                endpoints = UaTcpStackClient
                        .getEndpoints(discoveryUrl)
                        .get();
            }

        EndpointDescription endpoint = Arrays.stream(endpoints)
                .filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getSecurityPolicyUri()))
                .findFirst().orElseThrow(() -> new Exception("no desired endpoints returned"));

        //System.out.println("Using endpoint: {" + endpoint.getEndpointUrl() + "} Security policy: " + securityPolicy);

        OpcUaClientConfig config = OpcUaClientConfig.builder()
                .setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
                .setApplicationUri("urn:eclipse:milo:examples:client")
                //            .setCertificate(loader.getClientCertificate())
                //            .setKeyPair(loader.getClientKeyPair())
                .setEndpoint(endpoint)
                .setIdentityProvider(new AnonymousProvider())
                .setRequestTimeout(uint(5000))
                .build();
        OpcUaClient c = new OpcUaClient(config);
        c.connect().get();
        return c;
    }

}
