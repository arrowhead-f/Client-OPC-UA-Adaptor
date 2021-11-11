# OPC UA Node Naming Convention
There are 5 metadata keys associated with each service instances of the "SignalStatus" and "SignalUpdate" services.
The metadata keys are "NodeId", "Device", "DeviceType", "Location" and "http-method".
Hence, all nodes follow the naming structure as  \<DeviceID>\_\<DeviceType>\_\<Location>.

The names of the nodes is presented in the figure below.

![alt text](https://github.com/aparajita07/Client-OPC-UA-Adaptor/blob/main/OPCUAClient.PNG)
  
The logic behind deriving metadata of service instance associated with a particular node is:
* If the \<DeviceID> starts with "I", metadata key "Device"= "Sensor", else "Device"= "Actuator".
* The metadata key "DeviceType" is the \<DeviceType> part of the name.
* The metadata key "Location" is the \<Location> part of the name.
* The metadata "NodeId" is derived from the *NodeId* object.

## Note
Here, the naming convention for OPC UA nodes is informal and self-derived.
In real manufacturing industries, to maintain a common technical language between stakeholders, systems can be built using an international standards such as ISO 81346, which will be implemented in the next version of this Adaptor.
