package com.aajpm.altair.utility.webutils;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.utility.exception.ASCOMException;
import com.aajpm.altair.utility.exception.DeviceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

// TODO : async calls
public class AlpacaClient {
    WebClient alpaca;

    int transactionCounter = 1;
    
    int clientID = 0;
    
    /**
     * Create a new AlpacaClient with the specified timeouts
     * @param baseURL The URL of the server, e.g. http://127.0.0.1:11111
     * @param connTimeout The connection timeout in milliseconds
     * @param responseTimeout The response timeout in milliseconds
     */
    public AlpacaClient(String baseURL, int connTimeout, int responseTimeout) {
        alpaca = WebClient.builder()
                .baseUrl(baseURL)
                .clientConnector(
                    new ReactorClientHttpConnector(
                        HttpClient.create()
                            .responseTimeout(Duration.ofMillis(responseTimeout))
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connTimeout)))
                .build();
    }

    /**
     * Sets the current user for the client, for Alpaca's logging purposes.
     * @param user The user to set as the current user, or null to set the current user to "Unknown"
     */
    public void setCurrentUser(AltairUser user) {
        int userID = (user == null)? 0 : user.getId().intValue();
        if (userID != clientID) {
            clientID = userID;
            transactionCounter = 1;
        }
    }

    /**
     * Gets the server description, which contains the server name, manufacturer, version, and location.
     * @return The server description
     */
    public JsonNode getServerDescription() {
        return makeManualGetReq("/management/v1/description").findValue("Value");
    }

    /**
     * Gets the list of available devices on the server.
     * @return The list of available devices
     */
    public JsonNode getAvailableDevices() {
        return makeManualGetReq("/management/v1/configureddevices").findValue("Value");
    }

    /**
     * Runs a manual GET request to the specified endpoint. This is used for endpoints that are not part of the Alpaca API, or do not follow its standards.
     * Warning: This method does not check for errors, is synchronous and will not throw an exception if the server returns an error.
     * @param endpoint The endpoint to call, e.g. /api/v1/telescope/0/connected
     * @return The response from the server
     * @throws WebClientResponseException If the command could not be processed by the server
     */
    public JsonNode makeManualGetReq(String endpoint) throws WebClientResponseException {
        return alpaca.get()
            .uri(endpoint)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }

    /**
     * Runs a manual PUT request to the specified endpoint. This is used for endpoints that are not part of the Alpaca API, or do not follow its standards.
     * Warning: This method does not check for errors, is synchronous and will not throw an exception if the server returns an error.
     * @param endpoint The endpoint to call, e.g. /api/v1/telescope/0/slewtocoordinates
     * @param body The body of the request, e.g. a MultiValueMap containing the arguments for the call
     * @return The response from the server
     * @throws WebClientResponseException If the command could not be processed by the server
     */
    public JsonNode makeManualPutReq(String endpoint, Object body) throws WebClientResponseException {
        return alpaca.put()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }

    /**
     * Runs an Alpaca compliant GET request to the specified endpoint.
     * Warning: This method is synchronous
     * @param shortEndpoint The endpoint to call, without prefixes, e.g. telescope/0/connected
     * @return The returned value of the call.
     * @throws DeviceUnavailableException If the server is not available
     * @throws ASCOMException If the execution of the call returns an error
     * @throws WebClientResponseException If the command could not be processed by the server
     */
    public JsonNode get(String shortEndpoint) throws DeviceUnavailableException, ASCOMException, WebClientResponseException {
        String url = "/api/v1/" + shortEndpoint + "?clientid=" + clientID + "&clienttransactionid=" + transactionCounter++;
        
        JsonNode response = alpaca.get()
            .uri(url)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        if (response == null) {
            throw new DeviceUnavailableException("No response from server when calling " + shortEndpoint);
        }
        int errNo = response.findValue("ErrorNumber").asInt();
        if (errNo != 0) {
            throw new ASCOMException(errNo, response.findValue("ErrorMessage").asText());
        }
        return response.findValue("Value");
    }

    /**
     * Runs an Alpaca compliant GET request
     * @param deviceType The type of device, e.g. "telescope"
     * @param deviceNumber The zero based index of the device as set on the server
     * @param action The action to poll, e.g. "connected"
     * @return The returned value of the call.
     * @throws DeviceUnavailableException If the server is not available
     * @throws ASCOMException If the execution of the call returns an error
     * @throws WebClientResponseException If the command could not be processed by the server
     */
    public JsonNode get(String deviceType, int deviceNumber, String action) throws DeviceUnavailableException, ASCOMException, WebClientResponseException {
        return get(deviceType + "/" + deviceNumber + "/" + action);
    }

    /**
     * Runs an Alpaca compliant PUT request to the specified endpoint.
     * Warning: This method is synchronous
     * @param shortEndpoint The endpoint to call, without prefixes, e.g. telescope/0/slewtocoordinates
     * @param args The arguments to pass to the call
     * @throws DeviceUnavailableException If the server is not available
     * @throws ASCOMException If the execution of the call returns an error
     * @throws WebClientResponseException If the command could not be processed by the server
     */
    public void put(String shortEndpoint, MultiValueMap<String, String> args) throws DeviceUnavailableException, ASCOMException, WebClientResponseException {
        String url = "/api/v1/" + shortEndpoint;

        MultiValueMap<String, String> body;

        if (args == null)
            body = new LinkedMultiValueMap<>(2);
        else
            body = new LinkedMultiValueMap<>(args);
        
        body.add("ClientID", Integer.toString(clientID));
        body.add("ClientTransactionID", Integer.toString(transactionCounter++));

        
        JsonNode response = alpaca.put()
            .uri(url)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        if (response == null) {
            throw new DeviceUnavailableException("No response from server when calling " + shortEndpoint);
        }
        int errNo = response.findValue("ErrorNumber").asInt();
        if (errNo != 0) {
            throw new ASCOMException(errNo, response.findValue("ErrorMessage").asText());
        }
    }

    /**
     * Runs an Alpaca compliant PUT request
     * @param deviceType The type of device, e.g. "telescope"
     * @param deviceNumber The zero based index of the device as set on the server
     * @param action The action to poll, e.g. "slewtocoordinates"
     * @param args The arguments to pass to the call
     * @throws DeviceUnavailableException If the server is not available
     * @throws ASCOMException If the execution of the call returns an error
     * @throws WebClientResponseException If the command could not be processed by the server
     */
    public void put(String deviceType, int deviceNumber, String action, MultiValueMap<String, String> args) throws DeviceUnavailableException, ASCOMException, WebClientResponseException {
        put(deviceType + "/" + deviceNumber + "/" + action, args);
    }


    public Object cameraPhoto() {
        // TODO A way to get the image from a pic. Maybe there's a way to store it in a file?
        throw new UnsupportedOperationException("Not implemented yet");
    }

    
}
