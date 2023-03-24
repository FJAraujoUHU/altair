package com.aajpm.altair.utility.webutils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.utility.exception.ASCOMException;
import com.aajpm.altair.utility.exception.DeviceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;

import io.netty.channel.ChannelOption;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Should check which Alpaca calls are really synchronous, and add sleeps to the bad behaving ones.
public class AlpacaClient {

    WebClient alpaca, cameraClient;

    int transactionCounter = 1;
    
    int clientID = 0;

    int imageBufferSize = 256 * 1024 * 1024; // 256 MB

    private final Logger logger = LoggerFactory.getLogger(AlpacaClient.class.getName());
    
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

        cameraClient = WebClient.builder()
                .baseUrl(baseURL)
                .clientConnector(
                    new ReactorClientHttpConnector(
                        HttpClient.create()
                            .responseTimeout(Duration.ofMillis(responseTimeout))
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connTimeout)))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(imageBufferSize))
                .build();

        Hooks.onErrorDropped(error -> {
            if (!(error instanceof java.lang.IllegalStateException))    // Ignore, a bug in WebClient reports them twice.
                logger.warn("Exception happened : ", error);
        });
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
     * Gets the server description asynchronously, which contains the server name, manufacturer, version, and location.
     * @return A Mono that will return the server description
     */
    public Mono<JsonNode> getServerDescription() {
        return makeManualGetReq("/management/v1/description").map(node -> node.findValue("Value"));
    }

    /**
     * Gets the server description, which contains the server name, manufacturer, version, and location.
     * Warning: This method is synchronous and will not throw an exception if the server returns an error.
     * @return The server description
     */
    public JsonNode getServerDescriptionBlocking() {
        return getServerDescription().block();
    }

    /**
     * Gets the list of available devices on the server asynchronously.
     * @return A Mono that will return the list of available devices
     * @throws WebClientException If the command could not be processed by the server
     */
    public Mono<JsonNode> getAvailableDevices() throws WebClientException {
        return makeManualGetReq("/management/v1/configureddevices").map(node -> node.findValue("Value"));
    }

    /**
     * Gets the list of available devices on the server.
     * @return The list of available devices
     */
    public JsonNode getAvailableDevicesAwait() {
        return getAvailableDevices().block();
    }


    /**
     * Runs a manual GET request to the specified endpoint asynchronously. This is used for endpoints that are not part of the Alpaca API, or do not follow its standards.
     * @param endpoint The endpoint to call, e.g. /api/v1/telescope/0/connected
     * @return A Mono that will return the response from the server
     * @throws WebClientResponseException If the command could not be processed by the server
     */
    public Mono<JsonNode> makeManualGetReq(String endpoint) throws WebClientException {
        return alpaca.get()
            .uri(endpoint)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .doOnCancel(() -> {});
    }
    
    /**
     * Runs a manual GET request to the specified endpoint. This is used for endpoints that are not part of the Alpaca API, or do not follow its standards.
     * Warning: This method does not check for errors, is synchronous and will not throw an exception if the server returns an error.
     * @param endpoint The endpoint to call, e.g. /api/v1/telescope/0/connected
     * @return The response from the server
     * @throws WebClientResponseException If the command could not be processed by the server
     */
    public JsonNode makeManualGetReqAwait(String endpoint) {
        return makeManualGetReq(endpoint).block();
    }

    /**
     * Runs a manual PUT request to the specified endpoint asynchronously. This is used for endpoints that are not part of the Alpaca API, or do not follow its standards.
     * @param endpoint The endpoint to call, e.g. /api/v1/telescope/0/slewtocoordinates
     * @param body The body of the request, e.g. a MultiValueMap containing the arguments for the call
     * @return A Mono that can return the response from the server
     * @throws WebClientResponseException If the command could not be processed by the server
     */
    public Mono<JsonNode> makeManualPutReq(String endpoint, Object body) throws WebClientException {
        return alpaca.put()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .doOnCancel(() -> {});
    }

    /**
     * Runs a manual PUT request to the specified endpoint. This is used for endpoints that are not part of the Alpaca API, or do not follow its standards.
     * Warning: This method does not check for errors and is synchronous.
     * @param endpoint The endpoint to call, e.g. /api/v1/telescope/0/slewtocoordinates
     * @param body The body of the request, e.g. a MultiValueMap containing the arguments for the call
     * @return The response from the server
     * @throws WebClientResponseException If the command could not be processed by the server
     */
    public JsonNode makeManualPutReqAwait(String endpoint, Object body) throws WebClientResponseException {
        return makeManualPutReq(endpoint, body).block();
    }

    /**
     * Runs an Alpaca compliant GET request to the specified endpoint asynchronously.
     * @param shortEndpoint The endpoint to call, without prefixes, e.g. telescope/0/connected
     * @return A Mono that will return the value of the call.
     * @throws WebClientException If the command could not be processed by the server
     */
    public Mono<JsonNode> get(String shortEndpoint) throws WebClientException {
        String url = "/api/v1/" + shortEndpoint + "?clientid=" + clientID + "&clienttransactionid=" + transactionCounter++;

        return makeManualGetReq(url).flatMap(json -> {
            if (json == null)
                return Mono.error(new DeviceUnavailableException("No response from server when calling " + shortEndpoint));
            
            int errNo = json.findValue("ErrorNumber").asInt();
            if (errNo != 0)
                    return Mono.error(new ASCOMException(errNo, json.findValue("ErrorMessage").asText()));
            return Mono.just(json.findValue("Value"));
        });
    }

    /**
     * Runs an Alpaca compliant GET request to the specified endpoint asynchronously.
     * @param deviceType The device type, e.g. "telescope"
     * @param deviceNumber The device number, e.g. 0
     * @param action The action to perform, e.g. "connected"
     * @return A Mono that will return the value of the call.
     * @throws WebClientException If the command could not be processed by the server
     */
    public Mono<JsonNode> get(String deviceType, int deviceNumber, String action) throws WebClientException {
        return get(deviceType + "/" + deviceNumber + "/" + action);
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
    public JsonNode getAwait(String shortEndpoint) throws DeviceUnavailableException, ASCOMException, WebClientResponseException {
        JsonNode response = get(shortEndpoint).block();

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
    public JsonNode getAwait(String deviceType, int deviceNumber, String action) throws DeviceUnavailableException, ASCOMException, WebClientResponseException {
        return getAwait(deviceType + "/" + deviceNumber + "/" + action);
    }

    /**
     * Runs an Alpaca compliant PUT request to the specified endpoint asynchronously.
     * Warning: This method does not check for errors, those must be checked when using the Mono returned by this method.
     * @param shortEndpoint The endpoint to call, without prefixes, e.g. telescope/0/slewtocoordinates
     * @param args The arguments to pass to the call
     * @return A Mono that can return the response from the server
     * @throws WebClientResponseException If the command could not be processed by the server
     */
    public Mono<JsonNode> put(String shortEndpoint, MultiValueMap<String, String> args) throws DeviceUnavailableException, ASCOMException, WebClientResponseException {
        String url = "/api/v1/" + shortEndpoint;

        MultiValueMap<String, String> body;

        if (args == null)
            body = new LinkedMultiValueMap<>(2);
        else
            body = new LinkedMultiValueMap<>(args);
        
        body.add("ClientID", Integer.toString(clientID));
        body.add("ClientTransactionID", Integer.toString(transactionCounter++));

        return makeManualPutReq(url, body).flatMap(json -> {
            if (json == null)
                return Mono.error(new DeviceUnavailableException("No response from server when calling " + shortEndpoint));
            
            int errNo = json.findValue("ErrorNumber").asInt();
            if (errNo != 0)
                    return Mono.error(new ASCOMException(errNo, json.findValue("ErrorMessage").asText()));
            JsonNode value = json.findValue("Value");
            if (value == null)
                return Mono.empty();
            return Mono.just(value);
        });
    }

    /**
     * Runs an Alpaca compliant PUT request asynchronously.
     * Warning: This method does not check for errors, those must be checked when using the Mono returned by this method.
     * @param deviceType The type of device, e.g. "telescope"
     * @param deviceNumber The zero based index of the device as set on the server
     * @param action The action to poll, e.g. "slewtocoordinates"
     * @param args The arguments to pass to the call
     * @return A Mono that can return the response from the server
     * @throws WebClientResponseException
     */
    public Mono<JsonNode> put(String deviceType, int deviceNumber, String action, MultiValueMap<String, String> args) throws WebClientResponseException {
        return put(deviceType + "/" + deviceNumber + "/" + action, args);
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
    public void putAwait(String shortEndpoint, MultiValueMap<String, String> args) throws DeviceUnavailableException, ASCOMException, WebClientResponseException {
        String url = "/api/v1/" + shortEndpoint;

        MultiValueMap<String, String> body;

        if (args == null)
            body = new LinkedMultiValueMap<>(2);
        else
            body = new LinkedMultiValueMap<>(args);
        
        body.add("ClientID", Integer.toString(clientID));
        body.add("ClientTransactionID", Integer.toString(transactionCounter++));

        JsonNode response = makeManualPutReqAwait(url, body);

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
    public void putAwait(String deviceType, int deviceNumber, String action, MultiValueMap<String, String> args) throws DeviceUnavailableException, ASCOMException, WebClientResponseException {
        putAwait(deviceType + "/" + deviceNumber + "/" + action, args);
    }






    /**
     * Retrieves an image from an Alpaca-compliant camera.
     * @return TODO
     */
    public Mono<BufferedImage> cameraPhoto() {    
        return cameraClient.get()
                .uri("api/v1/camera/0/imagearray")
                .accept(MediaType.parseMediaType("application/imagebytes"))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        MediaType contentType = response.headers().contentType().orElse(null);
                        if (contentType == null)
                            return Mono.error(new RuntimeException(
                                    "Error when retrieving image from camera: No content type returned"));

                        String typeStr = contentType.toString();
                        // If the camera supports the Alpaca ImageBytes format
                        if (typeStr.startsWith("application/imagebytes")) {
                            return response.bodyToMono(byte[].class)
                                    .map(AlpacaClient::parseImage);
                        }

                        // If the camera falls back to standard Alpaca JSON
                        if (typeStr.startsWith("application/json")) {
                            return response.bodyToMono(JsonNode.class)
                                    .map(AlpacaClient::parseImage);
                        }

                        // If the camera returns an unsupported content type
                        return Mono.error(new RuntimeException(
                                "Error when retrieving image from camera: Unsupported content type returned: "
                                        + typeStr));

                    } else {
                        return Mono.error(new RuntimeException(
                                "Error when retrieving image from camera: " + response.statusCode().toString()));
                    }
                });
    }

    private static BufferedImage parseImage(byte[] imageBytes) {
        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        DataInputStream dis = new DataInputStream(bis);

        try {
        int metadataVersion = readIntLE(dis);
        int errorNumber = readIntLE(dis);
        long clientTransactionID = readUIntLE(dis);
        long serverTransactionID = readUIntLE(dis);
        int dataStart = readIntLE(dis);
        ImageArrayType imageElementType = ImageArrayType.fromValue(readIntLE(dis));
        ImageArrayType transmissionElementType = ImageArrayType.fromValue(readIntLE(dis));
        int rank = readIntLE(dis);
        int dim1 = readIntLE(dis);
        int dim2 = readIntLE(dis);
        int dim3 = readIntLE(dis);

        if (errorNumber != 0)
            throw new ASCOMException(errorNumber);

        

        
        
        


        dis.skipNBytes((dataStart - 44));

        return null;


        } catch (IOException e) {
            throw new RuntimeException("Error when parsing image bytes", e);
        }
    }

    private static BufferedImage parseImage(JsonNode json) {





        throw new UnsupportedOperationException("Not implemented yet");
    }

    /*private static int readIntLE(DataInputStream dis) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(dis.readInt());
        return buf.getInt(0);
    }*/

    private static int readIntLE(DataInputStream dis) throws IOException {
        return ByteBuffer.wrap(dis.readNBytes(4)).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static long readUIntLE(DataInputStream dis) throws IOException {
        // Since Java doesn't have unsigned types,
        // to get the unsigned value of a signed int, 
        // we need to mask the sign bit and then cast to long to make up for the lost bit.
        return readIntLE(dis) & 0xFFFFFFFFL;
    }

}
