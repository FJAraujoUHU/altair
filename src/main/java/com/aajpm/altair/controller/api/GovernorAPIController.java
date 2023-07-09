package com.aajpm.altair.controller.api;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aajpm.altair.config.ObservatoryConfig;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.security.account.AltairUserService;
import com.aajpm.altair.service.GovernorService;
import com.aajpm.altair.service.GovernorService.GovernorStatus;
import com.aajpm.altair.service.OrderService;
import com.aajpm.altair.service.ProgramService;
import com.aajpm.altair.utility.exception.UnauthorisedException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/altair/api/governor")
public class GovernorAPIController {

    @Autowired
    GovernorService governor;

    @Autowired
    OrderService orderService;

    @Autowired
    ProgramService programService;

    @Autowired
    ObservatoryConfig config;


    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<GovernorStatus> getStatus() {
        return Flux.interval(Duration.ofMillis(config.getStatusUpdateInterval()))
                .flatMap(i -> governor.getStatus());
    }

    @PostMapping(value = "/enable")
    public Mono<Boolean> enable() {
        try {
            AltairUser user = AltairUserService.getCurrentUser();
            if (!governor.userCanOperate(user))
                return Mono.error(new UnauthorisedException(user));

            return governor.enable();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @PostMapping(value = "/disable")
    public Mono<Boolean> disable() {
        try {
            AltairUser user = AltairUserService.getCurrentUser();
            if (!governor.userCanOperate(user))
                return Mono.error(new UnauthorisedException(user));

            return governor.disable();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @PostMapping(value = "/connectall")
    public Mono<Boolean> connectAll() {
        try {
            AltairUser user = AltairUserService.getCurrentUser();
            if (!governor.userCanOperate(user))
                return Mono.error(new UnauthorisedException(user));

            return governor.connectAll();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @PostMapping(value = "/disconnectall")
    public Mono<Boolean> disconnectAll() {
        try {
            AltairUser user = AltairUserService.getCurrentUser();
            if (!governor.userCanOperate(user))
                return Mono.error(new UnauthorisedException(user));

            return governor.disconnectAll();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @PostMapping(value = "/enteradminmode")
    public Mono<Boolean> enterAdminMode() {
        try {
            AltairUser user = AltairUserService.getCurrentUser();

            return governor.enterAdminMode(user);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @PostMapping(value = "/exitadminmode")
    public Mono<Boolean> exitAdminMode() {
        try {
            AltairUser user = AltairUserService.getCurrentUser();
            
            return governor.exitAdminMode(user);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @PostMapping(value = "/setsafeoverride")
    public Mono<Boolean> setSafeOverride(@RequestParam(value = "override") boolean safeOverride) {
        try {
            AltairUser user = AltairUserService.getCurrentUser();
            if (!governor.userCanOperate(user))
                return Mono.error(new UnauthorisedException(user));

            governor.setSafeOverride(safeOverride);
            return Mono.just(true);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @PostMapping(value = "/setslaving")
    public Mono<Boolean> setSlaving(@RequestParam(value = "enable") boolean enable) {
        try {
            AltairUser user = AltairUserService.getCurrentUser();
            if (!governor.userCanOperate(user))
                return Mono.error(new UnauthorisedException(user));

            else return governor.setSlaving(enable);
        } catch (Exception e) {
            return Mono.error(e);
        } 
    }

    @PostMapping(value = "/usealtairslaving")
    public Mono<Boolean> useAltairSlaving(@RequestParam(value = "usealtair") boolean useAltair) {
        try {
            AltairUser user = AltairUserService.getCurrentUser();
            if (!governor.userCanOperate(user))
                return Mono.error(new UnauthorisedException(user));

            else return governor.useAltairSlaving(useAltair);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @PostMapping(value = "/startorder")
    public Mono<Boolean> startOrder(@RequestParam(value = "orderid") int orderId) {
        try {
            AltairUser user = AltairUserService.getCurrentUser();
            if (!governor.userCanOperate(user))
                return Mono.error(new UnauthorisedException(user));

            return governor.queueOrder(orderService.findById(orderId));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @PostMapping(value = "/startprogram")
    public Mono<Boolean> startProgram(@RequestParam(value = "programid") int programId) {
        try {
            AltairUser user = AltairUserService.getCurrentUser();
            if (!governor.userCanOperate(user))
                return Mono.error(new UnauthorisedException(user));

            return governor.startProgram(programService.findById(programId), user);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @PostMapping(value = "/abortorder")
    public Mono<Boolean> abortOrder() {
        try {
            AltairUser user = AltairUserService.getCurrentUser();
            if (!governor.userCanOperate(user))
                return Mono.error(new UnauthorisedException(user));

            return governor.abortOrder();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @PostMapping(value = "/startobservatory")
    public Mono<Boolean> startObservatory() {
        try {
            AltairUser user = AltairUserService.getCurrentUser();
            if (!governor.userCanOperate(user))
                return Mono.error(new UnauthorisedException(user));

            return governor.startObservatory();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @PostMapping(value = "/stopobservatory")
    public Mono<Boolean> stopObservatory() {
        try {
            AltairUser user = AltairUserService.getCurrentUser();
            if (!governor.userCanOperate(user))
                return Mono.error(new UnauthorisedException(user));
                
            else return governor.stopObservatory();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
    
}
