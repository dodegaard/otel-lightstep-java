package com.dougodegaard.service;

import com.lightstep.opentelemetry.launcher.OpenTelemetryConfiguration;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@Slf4j
@SpringBootApplication
public class ServiceApplication {

	public static void main(String[] args) {
		log.info("starting server");

		// Installs exporter into tracer SDK default provider with batching span processor.
		OpenTelemetryConfiguration.newBuilder()
							.setServiceName("gamestore-server")
							.setAccessToken("NNClrk9E/HMFB1TZ8ouaFL93ibW+3aMuTRU2J9sEpV1J6d2Eb/NscB7VTDMvJOIBUkZA8wSRDniyU2ut93VezzXPnCFkl9KEFsjzeMsw")
							.setTracesEndpoint("https://ingest.lightstep.com:443")
							.install();

		// Get tracer
		SpringApplication.run(ServiceApplication.class, args);
	}
}

@RestController
class AvailabilityController {

	private boolean validate(String console) {
		return StringUtils.hasText(console) &&
				Set.of("ps5", "ps4", "switch", "xbox").contains(console);
	}

	@GetMapping("/availability/{console}")
	Map<String, Object> getAvailability(@PathVariable String console) {
		Tracer tracer = GlobalOpenTelemetry.getTracer("instrumentation-library-name", "1.0.0");
		
		Span parentSpan = Span.current();
		Span currentSpan = tracer.spanBuilder("getAvailability")
								.setParent(Context.current().with(parentSpan))
								.startSpan();

		Map<String, Object> returnMap;

		try {
			returnMap = Map.of("console", console,
			"available", checkAvailability(console, currentSpan));
		  } finally {
			parentSpan.end();
		  }

		return returnMap;
	}

	private boolean checkAvailability(String console, Span parentSpan) {
		// Get tracer
		Tracer tracer = GlobalOpenTelemetry.getTracer("instrumentation-library-name", "1.0.0");

		Span childSpan = tracer.spanBuilder("checkAvailability")
        .setParent(Context.current().with(parentSpan))
        .startSpan();

		Assert.state(validate(console), () -> "the console specified, " + console + ", is not valid.");
		//Span span = tracer.spanBuilder("checkAvailability").startSpan();
		childSpan.setAttribute("consoletype", console);
		childSpan.setAttribute("developer", "dodegaard");

		boolean returnval;

		// Make the span the current span
		try (Scope ss = childSpan.makeCurrent()) {
		  // In this scope, the span is the current/active span
		  switch (console) {
			case "ps5":
			{
				childSpan.addEvent("Console Availability Unknown Exception!");
				throw new RuntimeException("Service exception");
			} 
			case "xbox":
			{
				childSpan.addEvent("Console Available!");
				returnval = true;
				break;
			} 
			default:
			{
				childSpan.addEvent("Console Needs Order!");
				returnval = false;
				break;
			}
		};
		} finally {
			childSpan.end();
		}

		return returnval;
	}
}


