package com.aajpm.altair.service;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.exception.DeviceUnavailableException;
import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

// TODO : Migrate from AlpacaObservatoryService to ASCOMTelescopeService and tear down
public class AlpacaObservatoryService extends ObservatoryService {


    WebClient alpaca;

    int connTimeout = 5000;
    int responseTimeout = 5000;

    int transactionCounter = 1;

    Set<String> availableDevices = new HashSet<>();

    public AlpacaObservatoryService(String baseURL) {
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
    public void start() throws DeviceUnavailableException {
        // TODO Auto-generated method stub

        // populate available devices
        throw new UnsupportedOperationException("Unimplemented method 'start'");
    }

    @Override
    public void start(boolean parked) throws DeviceUnavailableException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'start'");
    }


    @Override
    public boolean connected(String device) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'connected'");
    }


    @Override
    public boolean isParked(String device) throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isParked'");
    }


    @Override
    public boolean isAtHome(String device) throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isAtHome'");
    }


    @Override
    public boolean isSlaved() throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isSlaved'");
    }


    @Override
    public void setSlaved(boolean slaved) throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSlaved'");
    }


    @Override
    public void abortJob() throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'abortJob'");
    }


    @Override
    public void halt() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'halt'");
    }


    @Override
    public void reset() throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reset'");
    }


    @Override
    public boolean takeControl(AltairUser user) throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'takeControl'");
    }


    @Override
    public void releaseControl(AltairUser user) throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'releaseControl'");
    }

    

}