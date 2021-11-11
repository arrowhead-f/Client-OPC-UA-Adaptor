package eu.arrowhead.client.skeleton.consumer;

import eu.arrowhead.common.dto.shared.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;

import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import eu.arrowhead.client.library.ArrowheadService;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.dto.shared.OrchestrationFlags.Flag;
import eu.arrowhead.common.dto.shared.OrchestrationFormRequestDTO.Builder;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.SSLProperties;
//import sun.management.Sensor;
//import eu.arrowhead.client.skeleton.provider.JSONReader;

import java.io.*;
import java.util.*;

@SpringBootApplication
@ComponentScan(basePackages = {CommonConstants.BASE_PACKAGE}) //TODO: add custom packages if any
public class ConsumerMain implements ApplicationRunner {

	//=================================================================================================
	// members

	@Autowired
	private ArrowheadService arrowheadService;

	@Autowired
	protected SSLProperties sslProperties;
	private final Logger logger = LogManager.getLogger(ConsumerMain.class);
	//=================================================================================================
	// methods

	//------------------------------------------------------------------------------------------------
	public static void main(final String[] args) {

		SpringApplication.run(ConsumerMain.class, args);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void run(final ApplicationArguments args) throws Exception {
		getNodeStructure("plantstructure");
		readsignal("signalstatus");
		writesignal("signalupdate","true");
		writesignal("signalupdate","false");

	}
	public void readsignal(String serviceDefinition){
		List<OrchestrationResultDTO> response = orchestrate(serviceDefinition);
		for(int i=0;i<response.size();i++){
			OrchestrationResultDTO result=response.get(i);
			String httpMeta = result.getMetadata().get("http-method");
			final HttpMethod httpMethod = HttpMethod.valueOf(httpMeta);//Http method should be specified in the description of the service.
			String identifier=result.getMetadata().get("NodeId");
			logger.info(result.getMetadata());
			final String address = result.getProvider().getAddress();
			final int port = result.getProvider().getPort();
			final String serviceUri = result.getServiceUri();
			final String interfaceName = result.getInterfaces().get(0).getInterfaceName(); //Simplest way of choosing an interface.
			String token = null;
			if (result.getAuthorizationTokens() != null) {
				token = result.getAuthorizationTokens().get(interfaceName); //Can be null when the security type of the provider is 'CERTIFICATE' or nothing.
			}
			final Object payload = null;
			final String consumedReadService = arrowheadService.consumeServiceHTTP(String.class, httpMethod, address, port, serviceUri, interfaceName, token, payload,"NodeId",identifier);
			logger.info("Consumed service: "+consumedReadService);
		}
	}
	public void writesignal(String serviceDefinition, String value){
		List<OrchestrationResultDTO> response = orchestrate(serviceDefinition);
		for(int i=0;i<response.size();i++){
			OrchestrationResultDTO result=response.get(i);
			String httpMeta = result.getMetadata().get("http-method");
			final HttpMethod httpMethod = HttpMethod.valueOf(httpMeta);//Http method should be specified in the description of the service.
			String identifier=result.getMetadata().get("NodeId");
			final String address = result.getProvider().getAddress();
			final int port = result.getProvider().getPort();
			final String serviceUri = result.getServiceUri();
			final String interfaceName = result.getInterfaces().get(0).getInterfaceName(); //Simplest way of choosing an interface.
			String token = null;
			if (result.getAuthorizationTokens() != null) {
				token = result.getAuthorizationTokens().get(interfaceName); //Can be null when the security type of the provider is 'CERTIFICATE' or nothing.
			}
			final Object payload = null;
			//Trying to update the last signal whose name starts with "Q10"----You can customize this to update your desired signal-------//
			if(identifier.contains("Q10")){
				final String consumedReadService = arrowheadService.consumeServiceHTTP(String.class, httpMethod, address, port, serviceUri, interfaceName, token, payload,"NodeId",identifier,"value",value);
				logger.info("Consumed service: "+consumedReadService);
			}
		}
	}
	public void getNodeStructure(String serviceDefinition) throws IOException {
		List<OrchestrationResultDTO> response = orchestrate(serviceDefinition);
		OrchestrationResultDTO result=response.get(0);
		String httpMeta = result.getMetadata().get("http-method");
		final HttpMethod httpMethod = HttpMethod.valueOf(httpMeta);//Http method should be specified in the description of the service.
		final String address = result.getProvider().getAddress();
		final int port = result.getProvider().getPort();
		final String serviceUri = result.getServiceUri();
		final String interfaceName = result.getInterfaces().get(0).getInterfaceName(); //Simplest way of choosing an interface.
		String token = null;
		if (result.getAuthorizationTokens() != null) {
			token = result.getAuthorizationTokens().get(interfaceName); //Can be null when the security type of the provider is 'CERTIFICATE' or nothing.
		}
		final Object payload = null;
		final String consumedReadService = arrowheadService.consumeServiceHTTP(String.class, httpMethod, address, port, serviceUri, interfaceName, token, payload);
		logger.info(consumedReadService);
		//Write the consumed data into the local file
		FileWriter file= new FileWriter("client-skeleton-consumer/src/main/resources/OPCUA_Entry.json");
		try{
			file.write(consumedReadService);
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
	//------For finding services using the Meta data from the JSON file------//
		public void SeviceSearch() throws IOException, ParseException, InterruptedException {
			ArrayList<String> NodeId = new ArrayList<>();
			ArrayList<String> Device = new ArrayList<>();
			ArrayList<String> DeviceType = new ArrayList<>();
			ArrayList<String> Location = new ArrayList<>();

			Scanner in = new Scanner(System.in);
			System.out.println("Enter the device Name: Sensor/Actuator/All ");
			String deviceinput = in.next();
			if (deviceinput.equalsIgnoreCase("Sensor"))
				System.out.println("Enter the device Type Name: Phototransistor/Push-button/All ");
			else
				System.out.println("Enter the device Type Name: Motor/Conveyor-Belt/Slider/All ");

			String devicetypeinput = in.next();
			if (devicetypeinput.equalsIgnoreCase("push-button") || devicetypeinput.equalsIgnoreCase("slider"))
				System.out.println("Enter the Location : LoadingStation/OffLoadingStation/All ");
			else if (devicetypeinput.equalsIgnoreCase("Motor"))
				System.out.println("Enter the Location : MillingStation/DrillingStation/All ");
			else
				System.out.println("Enter the Location : LoadingStation/MillingStation/DrillingStation/OffLoadingStation/All ");
			String locationinput = in.next();

			//Finding list of devices based on the input value
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(new FileReader("client-skeleton-consumer/src/main/resources/OPCUA_Entry.json"));
			JSONObject jsonObject = (JSONObject) obj;
			JSONArray Services = (JSONArray) jsonObject.get("Components");
			Iterator<JSONObject> iterator = Services.iterator();

			while (iterator.hasNext()) {
				JSONObject iter = iterator.next();
				String device = iter.get("Device").toString();
				String deviceType = iter.get("DeviceType").toString();
				String location = iter.get("Location").toString();
				String nodeId= iter.get("NodeId").toString();

				if ((device.equalsIgnoreCase(deviceinput) || deviceinput.equalsIgnoreCase("All")) && (deviceType.equalsIgnoreCase(devicetypeinput) || devicetypeinput.equalsIgnoreCase("All"))
						&& (location.equalsIgnoreCase(locationinput) || locationinput.equalsIgnoreCase("All"))) {
					NodeId.add(nodeId);
					Device.add(device);
					DeviceType.add(deviceType);
					Location.add(location);
				}
			}
			//Meta Data declaration
			Map<String, String> meta = new HashMap<String, String>();

			// --------------------- Read OPC-UA Variable using metadata---------------------------
			for (int i = 0; i < Device.size(); i++) {
				meta.put("DeviceType", DeviceType.get(i));
				meta.put("http-method", "GET");
				meta.put("NodeId",NodeId.get(i));
				meta.put("Device", Device.get(i));
				meta.put("Location", Location.get(i));
				logger.info(meta);

				//SIMPLE EXAMPLE OF INITIATING AN ORCHESTRATION
				List<OrchestrationResultDTO> DTOresult = orchestrate(meta);
				for (int r = 0; r < DTOresult.size(); r++) {
					OrchestrationResultDTO result = DTOresult.get(r);
					String httpMeta = result.getMetadata().get("http-method");
					final HttpMethod httpMethod = HttpMethod.valueOf(httpMeta);//Http method should be specified in the description of the service.
					final String address = result.getProvider().getAddress();
					final int port = result.getProvider().getPort();
					final String serviceUri = result.getServiceUri();
					final String interfaceName = result.getInterfaces().get(0).getInterfaceName(); //Simplest way of choosing an interface.
					String token = null;
					if (result.getAuthorizationTokens() != null) {
						token = result.getAuthorizationTokens().get(interfaceName); //Can be null when the security type of the provider is 'CERTIFICATE' or nothing.
					}
					final Object payload = null; //Can be null if not specified in the description of the service.
					final String consumedReadService = arrowheadService.consumeServiceHTTP(String.class, httpMethod, address, port, serviceUri, interfaceName, token, payload, "NodeId",result.getMetadata().get("NodeId"));
					System.out.println("Service response: Status is " + consumedReadService);
				}
			}
	 	}

	/*--------------------Orchestration using just meta data with the customized core system-------------------------*/
	public List<OrchestrationResultDTO> orchestrate(Map metadata) throws InterruptedException {
		final Builder orchestrationFormBuilder = arrowheadService.getOrchestrationFormBuilder();
		final ServiceQueryFormDTO requestedService = new ServiceQueryFormDTO();
		requestedService.setMetadataRequirements(metadata);
		//requestedService.setServiceDefinitionRequirement(serviceDefinition);

		orchestrationFormBuilder.requestedService(requestedService)
				.flag(Flag.MATCHMAKING, false) //When this flag is false or not specified, then the orchestration response cloud contain more proper provider. Otherwise only one will be chosen if there is any proper.
				.flag(Flag.OVERRIDE_STORE, true) //When this flag is false or not specified, then a Store Orchestration will be proceeded. Otherwise a Dynamic Orchestration will be proceeded.
				.flag(Flag.TRIGGER_INTER_CLOUD, false) //When this flag is false or not specified, then orchestration will not look for providers in the neighbor clouds, when there is no proper provider in the local cloud. Otherwise it will.
                .flag(Flag.METADATA_SEARCH, true);
		//System.out.println(orchestrationFormBuilder.requestedService(requestedService));

		final OrchestrationFormRequestDTO orchestrationRequest = orchestrationFormBuilder.build();

		OrchestrationResponseDTO response = null;
		try {
			response = arrowheadService.proceedOrchestration(orchestrationRequest);
		} catch(final ArrowheadException ex) {
			//Handle the unsuccessful request as you wish!
		}

		if(response ==null||response.getResponse().isEmpty()) {
			//If no proper providers found during the orchestration process, then the response list will be empty. Handle the case as you wish!
			System.out.println("FATAL ERROR: Orchestration response came back empty. Make sure the Service you try to consume is in the Service Registry and that the Consumer has the privileges to consume this Service (e.g. check intra_cloud_authorization and intra_cloud_interface_connection).");
			//System.exit(1);
			Thread.sleep(5000);
			orchestrate(metadata);
		}
		int i=0;

		final List<OrchestrationResultDTO> result = response.getResponse();//Simplest way of choosing a provider.
		return result;
	}

	/*--------------------Orchestration using ServiceDefinition-------------------------*/
	public List<OrchestrationResultDTO> orchestrate(String serviceDefinition) {
		final Builder orchestrationFormBuilder = arrowheadService.getOrchestrationFormBuilder();
		//final List<String> interfaceReq= new ArrayList<String>();
		//interfaceReq.add("HTTPS-SECURE-JSON");
		final ServiceQueryFormDTO requestedService = new ServiceQueryFormDTO();
		requestedService.setServiceDefinitionRequirement(serviceDefinition);
		//requestedService.setInterfaceRequirements(interfaceReq);

		orchestrationFormBuilder.requestedService(requestedService)
				.flag(Flag.MATCHMAKING, false) //When this flag is false or not specified, then the orchestration response cloud contain more proper provider. Otherwise only one will be chosen if there is any proper.
				.flag(Flag.OVERRIDE_STORE, true) //When this flag is false or not specified, then a Store Orchestration will be proceeded. Otherwise a Dynamic Orchestration will be proceeded.
				.flag(Flag.TRIGGER_INTER_CLOUD, false); //When this flag is false or not specified, then orchestration will not look for providers in the neighbor clouds, when there is no proper provider in the local cloud. Otherwise it will.

		final OrchestrationFormRequestDTO orchestrationRequest = orchestrationFormBuilder.build();
		printOut(orchestrationRequest);
		OrchestrationResponseDTO response = null;
		try {
			response = arrowheadService.proceedOrchestration(orchestrationRequest);
		} catch(final ArrowheadException ex) {
			//Handle the unsuccessful request as you wish!
		}

		if(response ==null||response.getResponse().isEmpty()) {
			//If no proper providers found during the orchestration process, then the response list will be empty. Handle the case as you wish!
			System.out.println("FATAL ERROR: Orchestration response came back empty. Make sure the Service you try to consume is in the Service Registry and that the Consumer has the privileges to consume this Service (e.g. check intra_cloud_authorization and intra_cloud_interface_connection).");
			System.exit(1);
		}

		final List<OrchestrationResultDTO> result = response.getResponse(); //Simplest way of choosing a provider.
		return result;
	}

	private void printOut(final Object object) {
		System.out.println(Utilities.toPrettyJson(Utilities.toJson(object)));
	}
	private String getInterface() {
		return sslProperties.isSslEnabled() ? "HTTPS-SECURE-JSON" : "HTTP-INSECURE-JSON";
	}
}
