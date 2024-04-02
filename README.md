# Client-OPC-UA-Adaptor
This project provides the possibility to integrate an existing OPC-UA Server with a Provider client in an [Arrowhead Framework](http://github.com/eclipse-arrowhead) local cloud with *ServiceRegistry* changes, that allows the provider to register multiple service instances with same service definition.
This SR changes will be available in release 4.4.
This adaptor is based on the [Eclipse Milo (tm)](https://github.com/eclipse/milo) OPC-UA stack and the [Client Skeletons](https://github.com/arrowhead-f/client-skeleton-java-spring) for the [Arrowhead Framework 4.3.0](https://github.com/eclipse-arrowhead/core-java-spring).
This repository has an example Provider and Consumer. The Provider allows Consumers to read and write OPC-UA Variable nodes in an OPC-UA Server using a REST API.
To implement this system, we used a fischertecknik indexed line factory with an inbuilt OPC-UA server of 9 sensor nodes and 10 actuator nodes.
When the Provider is started it will:

1. Connect to an OPC-UA server using connection details found in /src/main/resources/application.properties. 
2. Browse all Variable nodes beneath a given "root" node (also provided in application.properties), and register read/write services for all OPC-UA Variables nodes beneath this "root" Node to the *ServiceRegistry*. 
The service definition for each read service instance will be "SignalStatus" and for each write service instance, it is "SignalUpdate". The term "root" is chosen here since no nodes above this node in the OPC-UA hierarchy will be added to the *ServiceRegistry* making it the top level object.
3. Registers a "plantstructure" service, which provides the metadata of all available nodes in a JSON format. (The consumer needs to access this service in case it wants to perform orchestration using metadata)
4. Monitors the root node every 15 seconds to track any changes in its node structure. In case of change, it updates the *ServiceRegistry* with the correct service information at runtime. Hence, changes to any existing node or the addition of new nodes are automatically registered in the Arrowhead cloud.
6. Respond to incoming traffic to the REST API for all three services.

The example Consumer queries the Orchestrator for the "SignalStatus" service and gets the endpoints of all 19 service instances(9 sensor nodes and 10 actuator nodes). It gets the node identifier value from the metadata key "NodeId" of the orchestration response, and uses that in the query parameter while sending the service request to the Provider. For the "plantstructure" service, the Consumer does not need to send any query parameters to access the Provider's REST API. While consuming "SignalUpdate" service, along with the "NodeId", the Consumer needs to send "value" (String value "true"/"false") in the query parameters.

## How to use

### Requirements
* Java JRE/JDK 11
* Maven 3.5+
* Arrowhead core services running, for more info goto [Arrowhead Framework 4.3.0](https://github.com/eclipse-arrowhead/core-java-spring)
* To run the example Provider and Consumer, your OPC-UA Server should be configured with no security mode and no security policy. You can later add your own desired OPC-UA security using the [Eclipse Milo Stack](https://github.com/eclipse/milo) which this repository is based on.
* All variables below the chosen "root" node in the OPC-UA server should be both readable and writeable.
* The name of each node should follow a specific pattern such that the metadata for each service instance is derived from the node name. Refer [this](https://github.com/aparajita07/Client-OPC-UA-Adaptor/blob/main/NodeNaming.md) to name your OPC-UA nodes.


### Setup and run
1. Download or clone the repository.
2. Select a Node in your OPC-UA server that will serve as a root. All found Variable Nodes below this root
will be added to the Arrowhead Service Registry and exposed via a REST api. 
3. Edit the /src/main/resources/application.properties file to match your setup (Core systems, security configs, system names etc)
4. Build the code (e.g. for both Provider and Consumer you can run "maven package" in the root directory of the project)
4. Add rights to use the different services and service interfaces to the Consumer (e.g. intra_cloud and intra_cloud_connection_interfaces tables in the MySQL database).

To run your Provider, go to client-skeleton-provider/target folder and run: ```java -jar arrowhead-client-skeleton-provider-4.1.3.13.jar```.
To run your Consumer, go to client-skeleton-consumer/target folder and run: ```java -jar arrowhead-client-skeleton-consumer-4.1.3.13.jar```.

### Best practices
Note that the code provided in this repository is provided only as a starting point and needs to be extended to fit the application at hand. Please use the [best practices of the Arrowhead Spring skeletons](https://github.com/arrowhead-f/client-skeleton-java-spring#best-practices-to-start-with-the-skeletons) and [the OPC-UA documentation](https://opcfoundation.org/developer-tools/specifications-unified-architecture) when designing your application.
