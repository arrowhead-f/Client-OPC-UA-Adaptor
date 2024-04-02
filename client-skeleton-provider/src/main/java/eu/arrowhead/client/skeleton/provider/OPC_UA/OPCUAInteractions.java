/*********************************************************************
* Copyright (c) 2024 Aparajita Tripathy 
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/

package eu.arrowhead.client.skeleton.provider.OPC_UA;


import eu.arrowhead.client.skeleton.provider.ProviderApplicationInitListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

/**
 * This class contains different ways of interacting with OPC-UA. Note that the clients
 * supplied to the functions must already be connected (e.g. created through the OPCUAConnection class)
 * @author Niklas Karvonen
 */


public class OPCUAInteractions {
    private final Logger logger = LogManager.getLogger(ProviderApplicationInitListener.class);

    public static Vector<String> browseNode(OpcUaClient client, NodeId browseRoot) {
        //String returnString = "";
        Vector<String> returnNodes = new Vector<String>();
        try {
            List<Node> nodes = client.getAddressSpace().browse(browseRoot).get();
            for(Node node:nodes) {
                returnNodes.add("ns=" + node.getNodeId().get().getNamespaceIndex() + ",identifier=" + node.getNodeId().get().getIdentifier() + ",displayName=" + node.getDisplayName().get().getText() + ",nodeClass=" + node.getNodeClass().get());
            }
        } catch (Exception e) {
            System.out.println("Browsing nodeId=" + browseRoot + " failed: " + e.getMessage());
        }
        return returnNodes;
    }
    public static Vector<NodeId> browseNodeIds(OpcUaClient client, NodeId browseRoot) {
        //String returnString = "";
        Vector<NodeId> returnNodes = new Vector<>();
        try {
            List<Node> nodes = client.getAddressSpace().browse(browseRoot).get();
            for(Node node:nodes) {
                returnNodes.add(node.getNodeId().get());
            }
        } catch (Exception e) {
            System.out.println("Browsing nodeId=" + browseRoot + " failed: " + e.getMessage());
        }
        return returnNodes;
    }

    public static String getNodeDescription(NodeId node, String rootNodeIdentifier){
        return node.getIdentifier().toString().replace(rootNodeIdentifier, "").replace("\"", "").replace(".", "");
    }

    public static List<String> readNode(OpcUaClient client, NodeId nodeId) {
        List<String> returnObj = new ArrayList<String>();
        String val = "";
        String returnString="";
        long t;
        String returntime="";
        try {
            VariableNode node = client.getAddressSpace().createVariableNode(nodeId);
            DataValue value = node.readValue().get();
            CompletableFuture<DataValue> test = client.readValue(0.0, TimestampsToReturn.Both, nodeId);
            DataValue data = test.get();
           // System.out.println("DataValue Object: " + data);
            val = data.getValue().toString();
            returnString = val.replace("Variant{value=","").replace("}", "");

            t= data.getServerTime().getJavaTime();
            returntime=Long.toString(t);
            //System.out.println("java time is: " + t);
            returnObj.add("ID="+nodeId.getIdentifier().toString());
            returnObj.add(returnString);
            returnObj.add(returntime);
            System.out.println("return object value time pair is: "+returnObj);
        } catch (Exception e) {
            System.out.println("ERROR: " + e.toString());
        }
        return returnObj;
    }


    public static CompletableFuture<StatusCode> writeNode2(
            final OpcUaClient client,
            final NodeId nodeId,
            final Object value) {

        return client.writeValue(nodeId, new DataValue(new Variant(value)));
    }

    public static String writeNode(OpcUaClient client, NodeId nodeId, boolean value) {

        // FIXME There should be a way to programmatically get the type from Eclipse Milo and write the variable directly using that type. As far as I can see, however, Milo only supports writing Variants which requires the conversion of a value into an object before it can be written.
        String returnString = "";
        returnString += value;
        try {
            VariableNode node = client.getAddressSpace().createVariableNode(nodeId);
            Object val = new Object();
            Object identifier = node.getDataType().get().getIdentifier();
            UInteger id = UInteger.valueOf(0);

            if(identifier instanceof UInteger) {
                id = (UInteger) identifier;
            }
            System.out.println("value passed as: "+value);
            System.out.println("nodeid passed as: "+nodeId.toString());
            DataValue data = new DataValue(new Variant(value),StatusCode.GOOD, null);
            StatusCode status = client.writeValue(nodeId, data).get();
            System.out.println("Wrote DataValue: " + data + " status: " + status);
            returnString = status.toString();
        } catch (Exception e) {
            System.out.println("ERROR: " + e.toString());
        }
        return returnString;
    }

    public List<UaMonitoredItem> NodeSubscription(OpcUaClient client, NodeId nodeId) throws InterruptedException, ExecutionException{

            client.connect().get();
            //NodeId nodeId = new NodeId(3, "\"Machine Status\".\"I3 Push-button slider 2 front\"");
            // create a subscription @ 1000ms
            UaSubscription subscription = client.getSubscriptionManager().createSubscription(1000.0).get();

            // subscribe to the Value attribute of the server's CurrentTime node
            ReadValueId readValueId = new ReadValueId(
                    nodeId,//Identifiers.Server_ServerStatus_CurrentTime,
                    AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE
            );

            UInteger clientHandle = subscription.getRequestedLifetimeCount();
            //System.out.println("Client handle number is: "+clientHandle);

            MonitoringParameters parameters = new MonitoringParameters(
                    clientHandle,
                    1000.0,     // sampling interval
                    null,       // filter, null means use default
                    uint(10),   // queue size
                    true        // discard oldest
            );

            MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
                    readValueId,
                    MonitoringMode.Reporting,
                    parameters
            );

            // when creating items in MonitoringMode.Reporting this callback is where each item needs to have its
            // value/event consumer hooked up. The alternative is to create the item in sampling mode, hook up the
            // consumer after the creation call completes, and then change the mode for all items to reporting.
            BiConsumer<UaMonitoredItem, Integer> onItemCreated =
                    (item, id) -> item.setValueConsumer(this::onSubscriptionValue);

            List<UaMonitoredItem> items = subscription.createMonitoredItems(
                    TimestampsToReturn.Both,
                    newArrayList(request),
                    onItemCreated
            ).get();

            for (UaMonitoredItem item : items) {
                if (item.getStatusCode().isGood()) {
                    logger.info("item created for nodeId={}", item.getReadValueId().getNodeId());
                } else {
                    logger.warn(
                            "failed to create item for nodeId={} (status={})",
                            item.getReadValueId().getNodeId(), item.getStatusCode());
                }
            }
            Thread.sleep(1000);
            //future.complete(client);
       return items;
    }

    public void NodeUnSubscription(OpcUaClient client, List<UaMonitoredItem> items){
        try {
            client.connect().get();
            //NodeId nodeId = new NodeId(3, "\"Machine Status\".\"I3 Push-button slider 2 front\"");
            // create a subscription @ 1000ms
            UaSubscription subscription = client.getSubscriptionManager().createSubscription(1000.0).get();
            System.out.println("items to delete: "+items.get(0).getReadValueId());
            subscription.deleteMonitoredItems(items);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private String onSubscriptionValue(UaMonitoredItem item, DataValue value) {
        String nodeVal= item.getReadValueId().getNodeId().toString()+","+value.toString();
        System.out.println("item is: "+item);
        logger.info(
                "subscription value received: item={}, value={}, timestamp={}, systemtime={}",
                item.getReadValueId().getNodeId(), value.getValue(), value.getServerTime().getJavaTime(), System.currentTimeMillis());
        return nodeVal;
    }

}
