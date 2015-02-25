/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.databridge.agent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.agent.exception.*;
import org.wso2.carbon.databridge.agent.conf.DataEndpointConfiguration;
import org.wso2.carbon.databridge.agent.endpoint.DataEndpoint;
import org.wso2.carbon.databridge.agent.endpoint.DataEndpointGroup;
import org.wso2.carbon.databridge.agent.util.DataPublisherUtil;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.exception.TransportException;

import java.util.ArrayList;
import java.util.Map;

/**
 * API Used to communicate with Data Receivers WSO2 BAM/CEP. It can be used to send events to
 * multiple BAM/CEP nodes with load balancing and failover logic.
 */

public class DataPublisher {

    private static final Log log = LogFactory.getLog(DataPublisher.class);

    /**
     * List of group of endpoints where events needs to dispatched when
     * events are published using this API
     */
    private ArrayList<DataEndpointGroup> endpointGroups = new ArrayList<DataEndpointGroup>();

    /**
     * The Agent for which the data publisher belongs to.
     */
    private DataEndpointAgent dataEndpointAgent;

    /**
     * Creates the DataPublisher instance for a specific user, and the it creates
     * connection asynchronously to receiver endpoint.
     *
     * @param receiverURLSet The receiving endpoint URL Set. This can be either load balancing URL set,
     *                       or Failover URL set.
     * @param authURLSet     The authenticating URL Set for the endpoints given in receiverURLSet parameter.
     *                       This should be in the same format as receiverURL set parameter.
     * @param username       Authorized username at receiver.
     * @param password       The password of the username provided.
     * @throws DataEndpointAgentConfigurationException
     * @throws DataEndpointException
     * @throws DataEndpointConfigurationException
     * @throws DataEndpointAuthenticationException
     * @throws TransportException
     */
    public DataPublisher(String receiverURLSet, String authURLSet, String username, String password)
            throws DataEndpointAgentConfigurationException,
            DataEndpointException, DataEndpointConfigurationException,
            DataEndpointAuthenticationException, TransportException {
        dataEndpointAgent = AgentHolder.getInstance().getDefaultDataEndpointAgent();
        if (authURLSet == null) authURLSet = DataPublisherUtil.getDefaultAuthURLSet(receiverURLSet);
        processEndpoints(dataEndpointAgent, receiverURLSet, authURLSet, username, password);
        dataEndpointAgent.addDataPublisher(this);
    }

    /**
     * Creates the DataPublisher instance for a specific user, and the it creates
     * connection asynchronously to receiver endpoint.
     *
     * @param receiverURLSet The receiving endpoint URL Set. This can be either load balancing URL set,
     *                       or Failover URL set.
     * @param username       Authorized username at receiver.
     * @param password       The password of the username provided.
     * @throws DataEndpointAgentConfigurationException
     * @throws DataEndpointException
     * @throws DataEndpointConfigurationException
     * @throws DataEndpointAuthenticationException
     * @throws TransportException
     */
    public DataPublisher(String receiverURLSet, String username, String password)
            throws DataEndpointAgentConfigurationException,
            DataEndpointException, DataEndpointConfigurationException,
            DataEndpointAuthenticationException, TransportException {
        dataEndpointAgent = AgentHolder.getInstance().getDefaultDataEndpointAgent();
        processEndpoints(dataEndpointAgent, receiverURLSet, DataPublisherUtil.
                getDefaultAuthURLSet(receiverURLSet), username, password);
        dataEndpointAgent.addDataPublisher(this);
    }

    /**
     * Creates the DataPublisher instance for a specific user, and the it creates
     * connection asynchronously to receiver endpoint.
     *
     * @param type           The Agent name from which the DataPublisher that needs to be created. By default Thrift,
     *                       and Binary is supported. The type should match with the <Name>
     *                       element in the <data-agent-conf.xml>.
     * @param receiverURLSet The receiving endpoint URL Set. This can be either load balancing URL set,
     *                       or Failover URL set.
     * @param authURLSet     The authenticating URL Set for the endpoints given in receiverURLSet parameter.
     *                       This should be in the same format as receiverURL set parameter.
     * @param username       Authorized username at receiver.
     * @param password       The password of the username provided.
     * @throws DataEndpointAgentConfigurationException
     * @throws DataEndpointException
     * @throws DataEndpointConfigurationException
     * @throws DataEndpointAuthenticationException
     * @throws TransportException
     */
    public DataPublisher(String type, String receiverURLSet, String authURLSet, String username, String password)
            throws DataEndpointAgentConfigurationException,
            DataEndpointException, DataEndpointConfigurationException,
            DataEndpointAuthenticationException, TransportException {
        dataEndpointAgent = AgentHolder.getInstance().getDataEndpointAgent(type);
        processEndpoints(dataEndpointAgent, receiverURLSet, authURLSet, username, password);
        dataEndpointAgent.addDataPublisher(this);
    }


    /**
     * This validates the input that are passed in the DataPublisher creation,
     * and initiates the endpoints connection.
     *
     * @param dataEndpointAgent Agent of the DataPublisher.
     * @param receiverURLSet    The receiving endpoint URL Set. This can be either load balancing URL set,
     *                          or Failover URL set.
     * @param authURLSet        The authenticating URL Set for the endpoints given in receiverURLSet parameter.
     *                          This should be in the same format as receiverURL set parameter. If the authURLSet
     *                          is null, then default authURLSet will be generated from the receiverURL.
     * @param username          Authorized username at receiver.
     * @param password          The password of the username provided.
     * @throws DataEndpointConfigurationException
     * @throws DataEndpointAgentConfigurationException
     * @throws DataEndpointException
     * @throws DataEndpointAuthenticationException
     * @throws TransportException
     */
    private void processEndpoints(DataEndpointAgent dataEndpointAgent,
                                  String receiverURLSet, String authURLSet, String username, String password)
            throws DataEndpointConfigurationException, DataEndpointAgentConfigurationException,
            DataEndpointException, DataEndpointAuthenticationException, TransportException {
        ArrayList receiverURLGroups = DataPublisherUtil.getEndpointGroups(receiverURLSet);
        ArrayList authURLGroups = DataPublisherUtil.getEndpointGroups(authURLSet);
        DataPublisherUtil.validateURLs(receiverURLGroups, authURLGroups);

        for (int i = 0; i < receiverURLGroups.size(); i++) {
            Object[] receiverGroup = (Object[]) receiverURLGroups.get(i);
            Object[] authGroup = (Object[]) authURLGroups.get(i);
            boolean failOver = (Boolean) receiverGroup[0];

            DataEndpointGroup endpointGroup;
            if (failOver) endpointGroup = new DataEndpointGroup(DataEndpointGroup.HAType.FAILOVER, dataEndpointAgent);
            else endpointGroup = new DataEndpointGroup(DataEndpointGroup.HAType.LOADBALANCE,
                    dataEndpointAgent);
            /**
             * Since the first element holds the failover/LB settings
             * we need to start iterating from 2nd element.
             */
            for (int j = 1; j < receiverGroup.length; j++) {
                DataEndpointConfiguration endpointConfiguration =
                        new DataEndpointConfiguration((String) receiverGroup[j],
                                (String) authGroup[j], username, password, dataEndpointAgent.getTransportPool(),
                                dataEndpointAgent.getSecuredTransportPool(), dataEndpointAgent.
                                getAgentConfiguration().getBatchSize());
                DataEndpoint dataEndpoint = dataEndpointAgent.getNewDataEndpoint();
                dataEndpoint.initialize(endpointConfiguration);
                endpointGroup.addDataEndpoint(dataEndpoint);
            }
            endpointGroups.add(endpointGroup);
        }
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a blocking invocation until the event can be inserted in to internal
     * queue for the publishing to the endpoint groups. But in case if any one or all of the receiver
     * groups cannot send the event to the endpoint due to network connection failure,
     * or Receiver node is unreachable or Receiver node has shutdown, and
     * internal queue has become full then it will be blocked until the receiver is reachable again.
     *
     * @param event The Event that needs to sent for the receiver groups
     */
    public void publish(Event event) {
        for (DataEndpointGroup endpointGroup : endpointGroups) {
            endpointGroup.publish(event);
        }
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a blocking invocation until the event can be inserted in to internal
     * queue for the publishing to the endpoint groups. But in case if any one or all of the receiver
     * groups cannot send the event to the endpoint due to network connection failure,
     * or Receiver node is unreachable or Receiver node has shutdown, and
     * internal queue has become full then it will be blocked until the receiver is reachable again.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     */
    public void publish(String streamId, Object[] metaDataArray, Object[] correlationDataArray,
                        Object[] payloadDataArray) {
        publish(new Event(streamId, System.currentTimeMillis(), metaDataArray,
                correlationDataArray, payloadDataArray));
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a blocking invocation until the event can be inserted in to internal
     * queue for the publishing to the endpoint groups. But in case if any one or all of the receiver
     * groups cannot send the event to the endpoint due to network connection failure,
     * or Receiver node is unreachable or Receiver node has shutdown, and
     * internal queue has become full then it will be blocked until the receiver is reachable again.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     * @param arbitraryDataMap     Arbitrary data element of the event, which was not included in the stream
     *                             definition of the event, and intermittent data.
     */
    public void publish(String streamId, Object[] metaDataArray, Object[] correlationDataArray,
                        Object[] payloadDataArray, Map<String, String> arbitraryDataMap) {
        publish(new Event(streamId, System.currentTimeMillis(), metaDataArray,
                correlationDataArray, payloadDataArray, arbitraryDataMap));
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a blocking invocation until the event can be inserted in to internal
     * queue for the publishing to the endpoint groups. But in case if any one or all of the receiver
     * groups cannot send the event to the endpoint due to network connection failure,
     * or Receiver node is unreachable or Receiver node has shutdown, and
     * internal queue has become full then it will be blocked until the receiver is reachable again.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     * @param timeStamp            Timestamp of the event.
     */
    public void publish(String streamId, long timeStamp, Object[] metaDataArray,
                        Object[] correlationDataArray, Object[] payloadDataArray) {
        publish(new Event(streamId, timeStamp, metaDataArray, correlationDataArray, payloadDataArray));
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a blocking invocation until the event can be inserted in to internal
     * queue for the publishing to the endpoint groups. But in case if any one or all of the receiver
     * groups cannot send the event to the endpoint due to network connection failure,
     * or Receiver node is unreachable or Receiver node has shutdown, and
     * internal queue has become full then it will be blocked until the receiver is reachable again.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     * @param timeStamp            Timestamp of the event.
     * @param arbitraryDataMap     Arbitrary data element of the event, which was not included in the stream
     *                             definition of the event, and intermittent data.
     */
    public void publish(String streamId, long timeStamp, Object[] metaDataArray,
                        Object[] correlationDataArray, Object[] payloadDataArray, Map<String, String> arbitraryDataMap) {
        publish(new Event(streamId, timeStamp, metaDataArray, correlationDataArray, payloadDataArray, arbitraryDataMap));
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a non-blocking invocation and if the queue if full
     * then it will simply drop the event.
     *
     * @param event The event which needs to be published to the receiver groups
     * @return the success/failure of the event that has been published/dropped.
     */
    public boolean tryPublish(Event event) {
        boolean sent = true;
        for (DataEndpointGroup endpointGroup : endpointGroups) {
            try {
                endpointGroup.tryPublish(event);
                sent = true;
            } catch (EventQueueFullException e) {
                log.error("Unable to process the event for endpoint group "
                        + endpointGroup.toString() + ", dropping the event. ", e);
                if (log.isDebugEnabled()) log.debug("Dropped Event: " + event.toString() + " for the endpoint group " +
                        endpointGroup.toString());
                sent = false;
            }
        }
        return sent;
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a non-blocking invocation and if the queue if full
     * then it will wait until timeoutMS value, and try to insert into the queue.
     * If the publisher still can't insert into the queue then it will
     * return back with success/failure to the client.
     *
     * @param event     The event which needs to be published to the receiver groups
     * @param timeoutMS The timeout value to insert publish the event
     * @return the success/failure of the event that has been published/dropped.
     */
    public boolean tryPublish(Event event, long timeoutMS) {
        boolean sent = true;
        for (DataEndpointGroup endpointGroup : endpointGroups) {
            try {
                endpointGroup.tryPublish(event, timeoutMS);
            } catch (EventQueueFullException e) {
                log.error("Unable to process the event for endpoint group "
                        + endpointGroup.toString() + ", dropping the event. ", e);
                if (log.isDebugEnabled()) log.debug("Dropped Event: " + event.toString() + " for the endpoint group " +
                        endpointGroup.toString());
                sent = false;
            }
        }
        return sent;
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a non-blocking invocation and if the queue if full
     * then it will wait until timeoutMS value, and try to insert into the queue.
     * If the publisher still can't insert into the queue then it will
     * return back with success/failure to the client.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     * @param timeoutMS            The timeout value to insert publish the event
     * @return the success/failure of the event that has been published/dropped.
     */
    public boolean tryPublish(String streamId, Object[] metaDataArray, Object[] correlationDataArray,
                              Object[] payloadDataArray, long timeoutMS) {
        return tryPublish(new Event(streamId, System.currentTimeMillis(), metaDataArray,
                correlationDataArray, payloadDataArray), timeoutMS);
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a non-blocking invocation and if the queue if full
     * then it will wait until timeoutMS value, and try to insert into the queue.
     * If the publisher still can't insert into the queue then it will
     * return back with success/failure to the client.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     * @param arbitraryDataMap     Arbitrary data element of the event, which was not included in the stream
     *                             definition of the event, and intermittent data.
     * @param timeoutMS            The timeout value to insert publish the event
     * @return the success/failure of the event that has been published/dropped.
     */
    public boolean tryPublish(String streamId, Object[] metaDataArray, Object[] correlationDataArray,
                              Object[] payloadDataArray, Map<String, String> arbitraryDataMap, long timeoutMS) {
        return tryPublish(new Event(streamId, System.currentTimeMillis(), metaDataArray,
                correlationDataArray, payloadDataArray, arbitraryDataMap), timeoutMS);
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a non-blocking invocation and if the queue if full
     * then it will wait until timeoutMS value, and try to insert into the queue.
     * If the publisher still can't insert into the queue then it will
     * return back with success/failure to the client.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     * @param timeStamp            Time stamp of the event.
     * @param timeoutMS            The timeout value to insert publish the event
     * @return the success/failure of the event that has been published/dropped.
     */
    public boolean tryPublish(String streamId, long timeStamp, Object[] metaDataArray,
                              Object[] correlationDataArray, Object[] payloadDataArray, long timeoutMS) {
        return tryPublish(new Event(streamId, timeStamp, metaDataArray, correlationDataArray, payloadDataArray),
                timeoutMS);
    }

    /**
     * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a non-blocking invocation and if the queue if full
     * then it will wait until timeoutMS value, and try to insert into the queue.
     * If the publisher still can't insert into the queue then it will
     * return back with success/failure to the client.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     * @param arbitraryDataMap     Arbitrary data element of the event, which was not included in the stream
     *                             definition of the event, and intermittent data.
     * @param timeStamp            Time stamp of the event.
     * @param timeoutMS            The timeout value to insert publish the event
     * @return the success/failure of the event that has been published/dropped.
     */
    public boolean tryPublish(String streamId, long timeStamp, Object[] metaDataArray,
                              Object[] correlationDataArray, Object[] payloadDataArray,
                              Map<String, String> arbitraryDataMap, long timeoutMS) {
        return tryPublish(new Event(streamId, timeStamp, metaDataArray, correlationDataArray,
                payloadDataArray, arbitraryDataMap), timeoutMS);
    }

    /**
     * * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a non-blocking invocation and if the queue if full
     * then it will simply drop the event.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     * @return the success/failure of the event that has been published/dropped.
     */
    public boolean tryPublish(String streamId, Object[] metaDataArray, Object[] correlationDataArray,
                              Object[] payloadDataArray) {
        return tryPublish(new Event(streamId, System.currentTimeMillis(), metaDataArray,
                correlationDataArray, payloadDataArray));
    }

    /**
     * * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a non-blocking invocation and if the queue if full
     * then it will simply drop the event.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     * @param arbitraryDataMap     Arbitrary data element of the event, which was not included in the stream
     *                             definition of the event, and intermittent data.
     * @return the success/failure of the event that has been published/dropped.
     */
    public boolean tryPublish(String streamId, Object[] metaDataArray, Object[] correlationDataArray,
                              Object[] payloadDataArray, Map<String, String> arbitraryDataMap) {
        return tryPublish(new Event(streamId, System.currentTimeMillis(), metaDataArray,
                correlationDataArray, payloadDataArray, arbitraryDataMap));
    }

    /**
     * * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a non-blocking invocation and if the queue if full
     * then it will simply drop the event.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     * @param timeStamp            TimeStamp of the event.
     * @return the success/failure of the event that has been published/dropped.
     */
    public boolean tryPublish(String streamId, long timeStamp, Object[] metaDataArray,
                              Object[] correlationDataArray, Object[] payloadDataArray) {
        return tryPublish(new Event(streamId, timeStamp, metaDataArray, correlationDataArray, payloadDataArray));
    }

    /**
     * * Publish an event based on the event properties that are passed
     * for all receiver groups which has been specified in the DataPublisher.
     * This is a non-blocking invocation and if the queue if full
     * then it will simply drop the event.
     *
     * @param streamId             StreamId for which the event belongs to.
     * @param metaDataArray        Meta data element of the event.
     * @param correlationDataArray Correlation data element of the event.
     * @param payloadDataArray     Payload data element of the event.
     * @param arbitraryDataMap     Arbitrary data element of the event, which was not included in the stream
     *                             definition of the event, and intermittent data.
     * @param timeStamp            Timestamp of the event.
     * @return the success/failure of the event that has been published/dropped.
     */
    public boolean tryPublish(String streamId, long timeStamp, Object[] metaDataArray,
                              Object[] correlationDataArray, Object[] payloadDataArray,
                              Map<String, String> arbitraryDataMap) {
        return tryPublish(new Event(streamId, timeStamp, metaDataArray, correlationDataArray,
                payloadDataArray, arbitraryDataMap));
    }

    /**
     * Graceful shutdown of all the operations of the data publisher.
     * It will flush all the events to the relevant endpoint, and closes all the
     * resources and thread pools used for its operation. Once the shutdown operation
     * is called you can't publish events using the data publisher.
     *
     * @throws DataEndpointException
     */
    public void shutdown() throws DataEndpointException {
        for (DataEndpointGroup dataEndpointGroup : endpointGroups) {
            dataEndpointGroup.shutdown();
        }
        dataEndpointAgent.shutDown(this);
    }

    /**
     * Graceful shutdown of all the operations of the data publisher, and also
     * if there is no other data publishers registered with the associated then
     * it will shutdown the agent as well. Also during the data publisher shutdown
     * it will flush all the events to the relevant endpoint, and closes all the
     * resources and thread pools used for its operation. Once the shutdown operation
     * is called you can't publish events using the data publisher.
     *
     * @throws DataEndpointException
     */
    public void shutdownWithAgent() throws DataEndpointException {
        shutdown();
        dataEndpointAgent.shutDown();
    }
}


