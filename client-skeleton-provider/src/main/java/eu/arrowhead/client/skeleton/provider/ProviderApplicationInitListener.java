/*********************************************************************
* Copyright (c) 2024 Aparajita Tripathy 
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/

package eu.arrowhead.client.skeleton.provider;

import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import com.google.gson.JsonParser;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceSecurityType;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.UaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.library.ArrowheadService;
import ai.aitia.arrowhead.application.library.config.ApplicationInitListener;
import ai.aitia.arrowhead.application.library.util.ApplicationCommonConstants;
import eu.arrowhead.client.skeleton.provider.security.ProviderSecurityConfig;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.core.CoreSystem;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.client.skeleton.provider.OPC_UA.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

@Component
public class ProviderApplicationInitListener extends ApplicationInitListener {
	
	//=================================================================================================
	// members
	
	@Autowired
	private ArrowheadService arrowheadService;
	private OPCUAInteractions interactions = new OPCUAInteractions();
	
	@Autowired
	private ProviderSecurityConfig providerSecurityConfig;
	
	@Value(ApplicationCommonConstants.$TOKEN_SECURITY_FILTER_ENABLED_WD)
	private boolean tokenSecurityFilterEnabled;
	
	@Value(CommonConstants.$SERVER_SSL_ENABLED_WD)
	private boolean sslEnabled;

	@Value(ApplicationCommonConstants.$APPLICATION_SYSTEM_NAME)
	private String mySystemName;

	@Value(ApplicationCommonConstants.$APPLICATION_SERVER_ADDRESS_WD)
	private String mySystemAddress;

	@Value(ApplicationCommonConstants.$APPLICATION_SERVER_PORT_WD)
	private int mySystemPort;

	@Value("${opc.ua.connection_address}")
	private String opcuaServerAddress;

	@Value("${opc.ua.root_node_namespace}")
	private int rootNodeNamespaceIndex;

	@Value("${opc.ua.root_node_identifier}")
	private String rootNodeIdentifier;
	protected UaClient client;
	private final Logger logger = LogManager.getLogger(ProviderApplicationInitListener.class);

	//=================================================================================================
	// methods
	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) {

		//Checking the availability of necessary core systems
		checkCoreSystemReachability(CoreSystem.SERVICEREGISTRY);
		if (tokenSecurityFilterEnabled) {
			checkCoreSystemReachability(CoreSystem.AUTHORIZATION);
			//Initialize Arrowhead Context
			arrowheadService.updateCoreServiceURIs(CoreSystem.AUTHORIZATION);
		}

		setTokenSecurityFilter();
		opcuaServerAddress = opcuaServerAddress.replaceAll("opc.tcp://", "");
		logger.info("OPC UA SERVER_ADDRESS:" + opcuaServerAddress);
		List<UaMonitoredItem> items;

		//TODO: implement here any custom behavior on application start up
		//Register the plantstructure service which provides the map or the metadata information of the factory
		final ServiceRegistryRequestDTO ServiceRequest = createServiceRegistryRequest("PlantStructure", "/factory/map", HttpMethod.GET);
		arrowheadService.forceRegisterServiceToServiceRegistry(ServiceRequest);

		//Register read and write services into ServiceRegistry and get the node structure and count
		RegisterOPCUAService(opcuaServerAddress, rootNodeIdentifier, rootNodeNamespaceIndex);
	}

	//---------------Monitor the Node structure. In case of change, re-register services for the updated node structure------------//
	@EventListener(ApplicationReadyEvent.class)
	public void MonitorOPCUANodes() throws Exception {
		List<String> nodeDescription= new ArrayList<>();
		List<String> nodeDescriptionNew= new ArrayList<>();
		nodeDescription= getOPCUAStructure(opcuaServerAddress, rootNodeIdentifier, rootNodeNamespaceIndex);
		try{
			while(true){
				nodeDescriptionNew= getOPCUAStructure(opcuaServerAddress, rootNodeIdentifier, rootNodeNamespaceIndex);
				logger.info("new Node description: "+nodeDescriptionNew);
				if(nodeDescriptionNew.equals(nodeDescription)){
					logger.info("No change in node structure");
				}
				else {
					logger.info("Change in node structure");
					nodeDescription = nodeDescriptionNew;
					arrowheadService.unregisterServiceFromServiceRegistry("SignalStatus", "/factory/read/variable");
					logger.info("Unregistering service: "+"SignalStatus");
					arrowheadService.unregisterServiceFromServiceRegistry("SignalUpdate", "/factory/write/variable");
					logger.info("Unregistering service: "+"SignalUpdate");
					RegisterOPCUAService(opcuaServerAddress, rootNodeIdentifier, rootNodeNamespaceIndex);
				}
				Thread.sleep(15000);
			}
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	// Provides the OPC UA Node structure beneath the root node interms of number of nodes and description of each node
	private List<String> getOPCUAStructure(String opcuaServerAddress, String rootNodeIdentifier, int rootNodeNamespaceIndex ) {
			OPCUAConnection connection = new OPCUAConnection(opcuaServerAddress);
		    NodeId nodeId = new NodeId(rootNodeNamespaceIndex, rootNodeIdentifier);
			List<String> nodeDescription = new ArrayList<>();
			Vector<NodeId> nodesBeneath = OPCUAInteractions.browseNodeIds(connection.getConnectedClient(), nodeId);
			int size = nodesBeneath.size();
			String sizeInString = Integer.toString(size);
			nodeDescription.add(sizeInString);
			//connection.dispose();
			String[] Description = new String[size];
			int i = 0;
			for (NodeId node : nodesBeneath) {
				//Description[i] = nodeString.substring(nodeString.indexOf("displayName=") + "displayName=".length(), nodeString.indexOf(",nodeClass"));
				Description[i] = interactions.getNodeDescription(node, rootNodeIdentifier);
				nodeDescription.add(Description[i]);
			}
		return nodeDescription;
	}
		private void RegisterOPCUAService(String opcuaServerAddress, String rootNodeIdentifier, int rootNodeNamespaceIndex){
			OPCUAConnection conn = new OPCUAConnection(opcuaServerAddress);
			final OpcUaClient client= conn.getConnectedClient();
			OPCUAInteractions interactions= new OPCUAInteractions();
			FileWriter file = null;

			try {
				//Write OPC UA Metadata to JSON File
				JSONObject object= new JSONObject();
				object.put("Factory", "Fischer-Technik");
				JSONArray services= new JSONArray();
				file= new FileWriter("client-skeleton-provider/src/main/resources/OPCUA_Entry.json");

				// Read OPC UA Variables to register services at Run Time
				NodeId nodeId = new NodeId(rootNodeNamespaceIndex, rootNodeIdentifier);
				Vector<NodeId> nodesBeneath = OPCUAInteractions.browseNodeIds(conn.getConnectedClient(), nodeId);
				int size= nodesBeneath.size();
				String [] Device=new String[size];
				String [] DeviceType=new String[size];
				String [] Location=new String[size];
				String [] Description= new String[size];
				String id="";
				int i=0;
				for (NodeId node: nodesBeneath) {
					String nodeString=node.toString();
					String parts[] = nodeString.split(",");
					String identifierPart[] = parts[1].split("=");
					String identifier = identifierPart[1].replace("}","");
					//System.out.println("Node id is: "+identifier);
					Description[i]= interactions.getNodeDescription(node, rootNodeIdentifier);
					//System.out.println("Node description is: "+Description[i]);
					if (Description[i].startsWith("I")){
						Device[i]= "Sensor";
						if(Description[i].contains("_LoadingStation")) {
							Location[i] = "LoadingStation";
						}
						else if(Description[i].contains("_MillingStation")){
							Location[i]="MillingStation";
						}
						else if(Description[i].contains("_DrillingStation")){
							Location[i]="Drilling";
						}
						else if(Description[i].contains("_OffLoadingStation")){
							Location[i]="OffLoadingStation";
						}

						if(Description[i].contains("Phototransistor")){
							DeviceType[i]="Phototransistor";
						}
						else
							DeviceType[i]="Push-button";
					}

					else {
						Device[i] = "Actuator";
						if (Description[i].contains("_LoadingStation")) {
							Location[i] = "LoadingStation";
						} else if (Description[i].contains("_MillingStation")) {
							Location[i] = "MillingStation";
						} else if (Description[i].contains("_DrillingStation")) {
							Location[i] = "DrillingStation";
						} else if(Description[i].contains("_OffLoadingStation")){
							Location[i]="OffLoadingStation";
						}

						if (Description[i].contains("Conveyor-Belt")) {
							DeviceType[i] = "Conveyor-Belt";
						} else if (Description[i].contains("Slider")) {
							DeviceType[i] = "Slider";
						} else {
							DeviceType[i] = "Motor";
						}
					}

					id= Description[i].substring(0,Description[i].indexOf("_"));
					ServiceRegistryRequestDTO serviceRequest1 = createServiceRegistryRequest("SignalStatus" ,  "/factory/read/variable", HttpMethod.GET);
					serviceRequest1.getMetadata().put("NodeId", identifier);
					serviceRequest1.getMetadata().put("Device", Device[i]);
					serviceRequest1.getMetadata().put("DeviceType", DeviceType[i]);
					serviceRequest1.getMetadata().put("Location", Location[i]);
					arrowheadService.forceRegisterServiceToServiceRegistry(serviceRequest1);
					logger.info("Registered read service for variable " + id + ".");

					if(id.contains("Q")){
						ServiceRegistryRequestDTO serviceRequest2 = createServiceRegistryRequest("SignalUpdate" ,  "/factory/write/variable", HttpMethod.PUT);
						serviceRequest2.getMetadata().put("NodeId", identifier);
						serviceRequest2.getMetadata().put("Device", Device[i]);
						serviceRequest2.getMetadata().put("DeviceType", DeviceType[i]);
						serviceRequest2.getMetadata().put("Location", Location[i]);
						arrowheadService.forceRegisterServiceToServiceRegistry(serviceRequest2);
						logger.info("Registered write service for variable " + id + ".");
					}

					JSONObject descriptions= new JSONObject();
					descriptions.put("NodeId",identifier);
					descriptions.put("Device",Device[i]);
					descriptions.put("DeviceType",DeviceType[i]);
					descriptions.put("Location",Location[i]);
					//System.out.println(Description[k]+":  "+descriptions);
					if((services.contains(descriptions))==false){
						services.add(descriptions);
					}
					i++;
				}

				object.put("Components", services);
				file.write(object.toJSONString());
				logger.info("OPC-UA Metadata list written in JSON File");
			}

			catch (Exception e) {
				System.out.println("ERROR: Could not register to ServiceRegistry.");
			}
			finally{
				try {
					file.flush();
					file.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void customDestroy() {
		//TODO: implement here any custom behavior on application shout down
		logger.info("Unregistering services!!");
		arrowheadService.unregisterServiceFromServiceRegistry("PlantStructure", "/factory/map");
		arrowheadService.unregisterServiceFromServiceRegistry("SignalStatus", "/factory/read/variable");
		arrowheadService.unregisterServiceFromServiceRegistry("SignalUpdate", "/factory/write/variable");
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------

	//-------------------------------------------------------------------------------------------------
	private ServiceRegistryRequestDTO createServiceRegistryRequest(final String serviceDefinition, final String serviceUri, final HttpMethod httpMethod) {
		final ServiceRegistryRequestDTO serviceRegistryRequest = new ServiceRegistryRequestDTO();
		serviceRegistryRequest.setServiceDefinition(serviceDefinition);
		final SystemRequestDTO systemRequest = new SystemRequestDTO();
		systemRequest.setSystemName(mySystemName);
		systemRequest.setAddress(mySystemAddress);
		systemRequest.setPort(mySystemPort);

		if (tokenSecurityFilterEnabled) {
			systemRequest.setAuthenticationInfo(Base64.getEncoder().encodeToString(arrowheadService.getMyPublicKey().getEncoded()));
			serviceRegistryRequest.setSecure(ServiceSecurityType.TOKEN.name());
			serviceRegistryRequest.setInterfaces(List.of("HTTPS-SECURE-JSON"));
		} else if (sslEnabled) {
			systemRequest.setAuthenticationInfo(Base64.getEncoder().encodeToString(arrowheadService.getMyPublicKey().getEncoded()));
			serviceRegistryRequest.setSecure(ServiceSecurityType.CERTIFICATE.name());
			serviceRegistryRequest.setInterfaces(List.of("HTTPS-SECURE-JSON"));
			serviceRegistryRequest.setSecure(ServiceSecurityType.NOT_SECURE.name());
			serviceRegistryRequest.setInterfaces(List.of("HTTP-INSECURE-JSON"));
		}
		serviceRegistryRequest.setProviderSystem(systemRequest);
		serviceRegistryRequest.setServiceUri(serviceUri);
		serviceRegistryRequest.setMetadata(new HashMap<>());
		serviceRegistryRequest.getMetadata().put("http-method", httpMethod.name());
		return serviceRegistryRequest;
	}


	private void setTokenSecurityFilter() {
		if(!tokenSecurityFilterEnabled) {
			logger.info("TokenSecurityFilter in not active");
		} else {
			final PublicKey authorizationPublicKey = arrowheadService.queryAuthorizationPublicKey();
			if (authorizationPublicKey == null) {
				throw new ArrowheadException("Authorization public key is null");
			}
			
			KeyStore keystore;
			try {
				keystore = KeyStore.getInstance(sslProperties.getKeyStoreType());
				keystore.load(sslProperties.getKeyStore().getInputStream(), sslProperties.getKeyStorePassword().toCharArray());
			} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ex) {
				throw new ArrowheadException(ex.getMessage());
			}			
			final PrivateKey providerPrivateKey = Utilities.getPrivateKey(keystore, sslProperties.getKeyPassword());

			providerSecurityConfig.getTokenSecurityFilter().setAuthorizationPublicKey(authorizationPublicKey);
			providerSecurityConfig.getTokenSecurityFilter().setMyPrivateKey(providerPrivateKey);
		}
	}
}
