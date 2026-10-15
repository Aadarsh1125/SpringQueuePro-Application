package com.springqprobackend.springqpro.controller.rest;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.health.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import io.micrometer.core.instrument.Timer;

@RestController
@RequestMapping("/api/internal")
//@PreAuthorize("isAuthenticated()")
public class SystemHealthController {

    private final HealthEndpoint health;      // returns Map-style JSON (always works)
    private final MeterRegistry registry;

    public SystemHealthController(HealthEndpoint health, MeterRegistry registry) {
        this.health = health;
        this.registry = registry;
    }

    // -----------------------------
    // 1) Return Actuator health JSON
    // -----------------------------
    @GetMapping("/health")
    public Map<String, Object> getHealth() {
        HealthComponent hc = health.health();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", hc.getStatus().getCode());

        if (hc instanceof CompositeHealth composite) {
            Map<String, Object> sub = new LinkedHashMap<>();
            composite.getComponents().forEach((name, comp) -> {
                sub.put(name, Map.of(
                        "status", comp.getStatus().getCode()
                ));
            });
            out.put("components", sub);
        }

        return out;
    }

    // -----------------------------
    // 2) Return simple metric values


    @GetMapping("/metric/{name}")
    public Map<String, Object> getMetric(@PathVariable String name) {

        var search = registry.find(name);

        // Check Gauge
        Gauge gauge = search.gauge();
        if (gauge != null) {
            return Map.of(
                    "name", name,
                    "type", "gauge",
                    "value", gauge.value()
            );
        }

        // Check Counter
        Counter counter = search.counter();
        if (counter != null) {
            return Map.of(
                    "name", name,
                    "type", "counter",
                    "value", counter.count()
            );
        }

        // Check Timer
        Timer timer = search.timer();
        if (timer != null) {
            return Map.of(
                    "name", name,
                    "type", "timer",
                    "totalMs", timer.totalTime(TimeUnit.MILLISECONDS),
                    "count", timer.count(),
                    "meanMs", timer.mean(TimeUnit.MILLISECONDS)
            );
        }

        // Metric not registered: safely return null
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("type", "unknown");
        result.put("value", null);

        return result;
    }

}
