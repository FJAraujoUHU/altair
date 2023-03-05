package com.aajpm.altair.service;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.utility.exception.ASCOMException;
import com.aajpm.altair.utility.exception.TelescopeException;
import com.aajpm.altair.utility.exception.TelescopeUnavailableException;
import com.aajpm.altair.utility.statusreporting.TelescopeStatus;
import com.fasterxml.jackson.databind.JsonNode;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

public class AlpacaTelescopeService extends TelescopeService {


    WebClient alpaca;

    int connTimeout = 5000;
    int responseTimeout = 5000;

    int transactionCounter = 1;

    Set<String> availableDevices = new HashSet<>();

    public AlpacaTelescopeService(String baseURL) {
        alpaca = WebClient.builder()
                .baseUrl(baseURL)
                .clientConnector(
                    new ReactorClientHttpConnector(
                        HttpClient.create()
                            .responseTimeout(Duration.ofMillis(responseTimeout))
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connTimeout)))
                .build();
    }


    @Override
    public void start() throws TelescopeUnavailableException {
        // TODO Auto-generated method stub

        // populate available devices
        throw new UnsupportedOperationException("Unimplemented method 'start'");
    }

    @Override
    public void start(boolean parked) throws TelescopeUnavailableException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'start'");
    }

    @Override
    public boolean connected() {
        if (this.state == State.OFF || this.state == State.ERROR)
            return false;
        
        return availableDevices.stream().allMatch(device -> {
            try {
                return connected(device);
            } catch (TelescopeException e) {
                return false;
            }
        });
    }

    @Override
    public boolean connected(String device) throws TelescopeException {
        
        if (!availableDevices.contains(device.toLowerCase()))
            return false;

        String endpoint = "/api/v1/" + device.toLowerCase() + "/0/connected";
        JsonNode response = makeGetReq(endpoint);
        int errorNumber = response.findValue("ErrorNumber").asInt();
        
        if (errorNumber != 0)
            throw new ASCOMException(errorNumber);
        

        return response.findValue("Value").asBoolean();
    }

    @Override
    public void park() throws TelescopeException {
        if (this.state == State.OFF || this.state == State.ERROR)
            throw new TelescopeException("Telescope is not available");

        MultiValueMap<String, String> args = new LinkedMultiValueMap<>();
        if (this.currentUser != null) {
            args.add("ClientID", this.currentUser.getId().toString());
            args.add("ClientTransactionID", String.valueOf(transactionCounter++));
        }

        int tcErrNo = 0;
        if (connected("telescope")) {
            JsonNode response = makePutReq("/api/v1/telescope/0/park", args);
            tcErrNo = response.findValue("ErrorNumber").asInt();
        }   

        int dmErrNo = 0;
        if (connected("dome")) {

            // If dome is slaved, unslave before parking
            if (isSlaved()) {
                setSlaved(false);
            }

            if (this.currentUser != null && tcErrNo == 0) {
                args.set("ClientTransactionID", String.valueOf(transactionCounter++));
            }

            JsonNode response = makePutReq("/api/v1/dome/0/park", args);
            dmErrNo = response.findValue("ErrorNumber").asInt();
        }

        if (tcErrNo != 0) {
            throw new ASCOMException(tcErrNo);
        }
        if (dmErrNo != 0) {
            throw new ASCOMException(dmErrNo);
        }
        state = State.IDLE;
    }

    @Override
    public void unpark() throws TelescopeException {
        if (this.state == State.OFF || this.state == State.ERROR)
            throw new TelescopeException("Telescope is not available");

        MultiValueMap<String, String> args = new LinkedMultiValueMap<>();
        if (this.currentUser != null) {
            args.add("ClientID", this.currentUser.getId().toString());
            args.add("ClientTransactionID", String.valueOf(transactionCounter++));
        }

            JsonNode response = makePutReq("/api/v1/telescope/0/unpark", args);
            int errorNumber = response.findValue("ErrorNumber").asInt();
            if (errorNumber != 0)
                throw new ASCOMException(errorNumber);
    }

    @Override
    public void abortJob() throws TelescopeException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'abortJob'");
    }

    @Override
    public void halt() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'halt'");
    }

    @Override
    public void reset() throws TelescopeException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reset'");
    }

    @Override
    public boolean takeControl(AltairUser user) throws TelescopeException {
        if (this.state == State.OFF || this.state == State.ERROR)
            throw new TelescopeException("Telescope is not available");
        if (this.currentUser != null)
            return false;

        if (this.state == State.AUTO)
            abortJob();

        currentUser = user;
        this.state = State.MANUAL;
        return true;
    }

    @Override
    public void releaseControl(AltairUser user) throws TelescopeException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'releaseControl'");
    }

    public JsonNode makeGetReq(String endpoint) {
        return alpaca.get()
            .uri(endpoint)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }

    public JsonNode makePutReq(String endpoint, Object body) {
        return alpaca.put()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }


    @Override
    public boolean isSlaved() throws TelescopeException {
        JsonNode response = makeGetReq("/api/v1/telescope/0/slaved");

        int errorNumber = response.findValue("ErrorNumber").asInt();
        if (errorNumber != 0) {
            throw new ASCOMException(errorNumber);
        }

        return response.findValue("Value").asBoolean();
    }


    @Override
    public void setSlaved(boolean slaved) throws TelescopeException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>();
        args.add("Slaved", String.valueOf(slaved));
        if (this.currentUser != null) {
            args.add("ClientID", this.currentUser.getId().toString());
            args.add("ClientTransactionID", String.valueOf(transactionCounter++));
        }

        JsonNode response = makePutReq("/api/v1/telescope/0/slaved", args);

        int errorNumber = response.findValue("ErrorNumber").asInt();
        if (errorNumber != 0) {
            throw new ASCOMException(errorNumber);
        }
    }

    @Override
    public boolean isParked() throws TelescopeException {
        if (this.state == State.OFF || this.state == State.ERROR)
            throw new TelescopeException("Telescope is not available");
        
        boolean tcParked = true;
        if (connected("telescope")) {
            JsonNode response = makeGetReq("/api/v1/telescope/0/atpark");
            int errorNumber = response.findValue("ErrorNumber").asInt();
            if (errorNumber != 0) {
                throw new ASCOMException(errorNumber);
            }
            tcParked = response.findValue("Value").asBoolean();
        }

        boolean dmParked = true;
        if (connected("dome")) {
            JsonNode response = makeGetReq("/api/v1/dome/0/atpark");
            int errorNumber = response.findValue("ErrorNumber").asInt();
            if (errorNumber != 0) {
                throw new ASCOMException(errorNumber);
            }
            dmParked = response.findValue("Value").asBoolean();
        }

        return tcParked && dmParked;
    }


    @Override
    public boolean isParked(String device) throws TelescopeException {
        if (this.state == State.OFF || this.state == State.ERROR)
            throw new TelescopeException("Telescope is not available");
        if (availableDevices.contains(device.toLowerCase()))
            throw new TelescopeException("Device " + device + " is not present");
        if (!device.toLowerCase().matches("telescope|dome"))
            throw new TelescopeException("Device " + device + " is not supported");
        
        JsonNode response = makeGetReq("/api/v1/" + device + "/0/atpark");
        int errorNumber = response.findValue("ErrorNumber").asInt();
        if (errorNumber != 0) {
            throw new ASCOMException(errorNumber);
        }
        return response.findValue("Value").asBoolean();
    }


    @Override
    public boolean isAtHome() throws TelescopeException {
        if (this.state == State.OFF || this.state == State.ERROR)
            throw new TelescopeException("Telescope is not available");
        
        boolean tcAtHome = true;
        if (connected("telescope")) {
            JsonNode response = makeGetReq("/api/v1/telescope/0/athome");
            int errorNumber = response.findValue("ErrorNumber").asInt();
            if (errorNumber != 0) {
                throw new ASCOMException(errorNumber);
            }
            tcAtHome = response.findValue("Value").asBoolean();
        }

        boolean dmAtHome = true;
        if (connected("dome")) {
            JsonNode response = makeGetReq("/api/v1/dome/0/athome");
            int errorNumber = response.findValue("ErrorNumber").asInt();
            if (errorNumber != 0) {
                throw new ASCOMException(errorNumber);
            }
            dmAtHome = response.findValue("Value").asBoolean();
        }

        return tcAtHome && dmAtHome;
    }


    @Override
    public boolean isAtHome(String device) throws TelescopeException {
        if (this.state == State.OFF || this.state == State.ERROR)
            throw new TelescopeException("Telescope is not available");
        if (availableDevices.contains(device.toLowerCase()))
            throw new TelescopeException("Device " + device + " is not present");
        if (!device.toLowerCase().matches("telescope|dome"))
            throw new TelescopeException("Device " + device + " is not supported");
        
        JsonNode response = makeGetReq("/api/v1/" + device + "/0/athome");
        int errorNumber = response.findValue("ErrorNumber").asInt();
        if (errorNumber != 0) {
            throw new ASCOMException(errorNumber);
        }
        return response.findValue("Value").asBoolean();
    }

    public double[] getAltAz() throws TelescopeException {
        if (this.state == State.OFF || this.state == State.ERROR)
            throw new TelescopeException("Telescope is not available");

        double azimuth, altitude;
        
        JsonNode response = makeGetReq("/api/v1/telescope/0/azimuth");
        int errorNumber = response.findValue("ErrorNumber").asInt();
        if (errorNumber != 0) {
            throw new ASCOMException(errorNumber);
        }
        azimuth = response.findValue("Value").asDouble();

        response = makeGetReq("/api/v1/telescope/0/altitude");
        errorNumber = response.findValue("ErrorNumber").asInt();
        if (errorNumber != 0) {
            throw new ASCOMException(errorNumber);
        }
        altitude = response.findValue("Value").asDouble();

        return new double[] { azimuth, altitude };
    }

    public double[] getRADec() {
        if (this.state == State.OFF || this.state == State.ERROR)
            throw new TelescopeException("Telescope is not available");

        double rightAscension, declination;
        
        JsonNode response = makeGetReq("/api/v1/telescope/0/rightascension");
        int errorNumber = response.findValue("ErrorNumber").asInt();
        if (errorNumber != 0) {
            throw new ASCOMException(errorNumber);
        }
        rightAscension = response.findValue("Value").asDouble();

        response = makeGetReq("/api/v1/telescope/0/declination");
        errorNumber = response.findValue("ErrorNumber").asInt();
        if (errorNumber != 0) {
            throw new ASCOMException(errorNumber);
        }
        declination = response.findValue("Value").asDouble();

        return new double[] { rightAscension, declination };
    }

    @Override
    public TelescopeStatus getTelescopeStatus() throws TelescopeException {
        // TODO Auto-generated method stub
        return null;
    }

}