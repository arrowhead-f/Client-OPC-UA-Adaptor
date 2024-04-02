/*********************************************************************
* Copyright (c) 2024 Aparajita Tripathy 
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/

package eu.arrowhead.client.skeleton.provider.controller;
import eu.arrowhead.common.Defaults;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import eu.arrowhead.client.skeleton.provider.OPC_UA.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@CrossOrigin(maxAge = Defaults.CORS_MAX_AGE, allowCredentials = Defaults.CORS_ALLOW_CREDENTIALS,
		allowedHeaders = { HttpHeaders.ORIGIN, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, HttpHeaders.AUTHORIZATION }
)

@RestController
@RequestMapping("/factory")
public class ProviderController {
	//=================================================================================================
	// members
	@Value("${opc.ua.connection_address}")
	private String opcuaServerAddress;

	@Value("${opc.ua.root_node_namespace}")
	private int rootNodeNamespaceIndex;

	@Value("${opc.ua.root_node_identifier}")
	private String rootNodeIdentifier;

	public ProviderController() throws IOException, ParseException {
	}

	@GetMapping(path = "/read/variable")
	@ResponseBody
	public String readvalue(@RequestParam(name="NodeId") String nodeIdentifier){
		NodeId nodeId= new NodeId(rootNodeNamespaceIndex,nodeIdentifier);
		opcuaServerAddress = opcuaServerAddress.replaceAll("opc.tcp://", "");
		OPCUAConnection connection = new OPCUAConnection(opcuaServerAddress);
		List<String> body;
		String returnval;
		try{
			body = OPCUAInteractions.readNode(connection.getConnectedClient(), nodeId);
			returnval=body.toString();
			connection.dispose();
			//String Val = body.replace("Variant{value=","{");
			return returnval;
		} catch (Exception ex) {
			connection.dispose();
			return "There was an error reading the OPC-UA node.";
		}
	}
	@PutMapping(path = "/write/variable")
	@ResponseBody
	public String writevalue(@RequestParam(name="NodeId") String nodeIdentifier, @RequestParam(name = "value") final String value){
		NodeId nodeId= new NodeId(rootNodeNamespaceIndex,nodeIdentifier);
		opcuaServerAddress = opcuaServerAddress.replaceAll("opc.tcp://", "");
		OPCUAConnection connection = new OPCUAConnection(opcuaServerAddress);
		String body= "Wrote value: " + value+", "+"NodeId= "+nodeIdentifier+", ";
		boolean bolValue= false;
		if(value.equalsIgnoreCase ("true"))
			bolValue=true;
		else bolValue=false;
		try{
			body += OPCUAInteractions.writeNode(connection.getConnectedClient(), nodeId,bolValue);
			connection.dispose();
			//String Val = body.replace("Variant{value=","{");
			return body;
		} catch (Exception ex) {
			connection.dispose();
			return "There was an error writing the OPC-UA node.";
		}
	}

	@GetMapping(path="/map")
	@ResponseBody
	public String getStructure() throws IOException {
		String file= "client-skeleton-provider/src/main/resources/OPCUA_Entry.json";
		String map= new String(Files.readAllBytes(Paths.get(file)));
		return map;
	}

	@GetMapping(path= "/exit")
	@ResponseBody
	public String sessionExit(){
		System.exit(0);
		System.out.println("Unregistering services");
		return "System shutting down!!";
	}
	//-------------------------------------------------------------------------------------------------
	//TODO: implement here your provider related REST end points
	// FIXME Double-check that the token security prevents tampering with variables in the OPC-UA it is not supposed to access (I.e. only allows access to the variables in the Service Registry)
	//-------------------------------------------------------------------------------------------------

	@RequestMapping("*")
	@ResponseBody
	public String fallbackMethod(){
		return "fallback method";
	}
}
