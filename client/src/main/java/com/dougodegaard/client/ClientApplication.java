package com.dougodegaard.client;

import com.lightstep.opentelemetry.launcher.OpenTelemetryConfiguration;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;

@Slf4j
@SpringBootApplication
public class ClientApplication {

	public static void main(String[] args) {
		log.info("starting client");

		// Installs exporter into tracer SDK default provider with batching span processor.
		OpenTelemetryConfiguration.newBuilder()
							.setServiceName("gamestore-mobileapp")
							.setAccessToken("NNClrk9E/HMFB1TZ8ouaFL93ibW+3aMuTRU2J9sEpV1J6d2Eb/NscB7VTDMvJOIBUkZA8wSRDniyU2ut93VezzXPnCFkl9KEFsjzeMsw")
							.setTracesEndpoint("https://ingest.lightstep.com:443")
							.install();

		SpringApplication.run(ClientApplication.class, args);
	}
}

@RestController
class AvailabilityController {

	private boolean validate(String console) {
		return StringUtils.hasText(console) &&
				Set.of("ps5", "ps4", "switch", "xbox").contains(console);
	}

	@GetMapping("/mobile/checkstock/{console}")
	Map<String, Object> checkStock(@PathVariable String console) {
		Tracer tracer = GlobalOpenTelemetry.getTracer("instrumentation-library-name", "1.0.0");
		Span parentSpan = tracer.spanBuilder("checkStock").setSpanKind(SpanKind.SERVER).startSpan();

		parentSpan.setAttribute("consoletype", console);
		
		Map<String, Object> returnMap;

		try {
			returnMap = Map.of("console", console,
			"available", checkAvailability(console, parentSpan));
		  } finally {
			parentSpan.end();
		  }

		return returnMap;
	}

	// Tell OpenTelemetry to inject the context in the HTTP headers
	TextMapSetter<HttpURLConnection> setter =
	new TextMapSetter<HttpURLConnection>() {
		@Override
		public void set(HttpURLConnection carrier, String key, String value) {
			// Insert the context as Header
			carrier.setRequestProperty(key, value);
		}
	};

	private boolean checkAvailability(String console, Span parentSpan) {
		// Get tracer
		Tracer tracer = GlobalOpenTelemetry.getTracer("instrumentation-library-name", "1.0.0");

		Span childSpan = tracer.spanBuilder("checkStockService")
        .setParent(Context.current().with(parentSpan))
        .startSpan();

		Assert.state(validate(console), () -> "the console specified, " + console + ", is not valid.");

		childSpan.setAttribute("consoletype", console);
		childSpan.setAttribute("developer", "dodegaard");
		childSpan.setAttribute("user-agent", "TODO grab user agent");

		// call api for availability
		boolean returnval;

		// Make the span the current span
		try (Scope ss = childSpan.makeCurrent()) {
		  // In this scope, the span is the current/active span
		  URL url;
		  try {
			// hard coded url from server project
			  url = new URL("http://127.0.0.1:8083/availability/" + console);
			  HttpURLConnection con = (HttpURLConnection) url.openConnection();
			  con.setRequestMethod("GET");
			  // Inject the request with the *current*  Context, which contains our current Span.
  			  GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), con, setter);
		  } catch (Exception e) {
			  returnval = false;
			  e.printStackTrace();
			  ((Span) ss).setStatus(StatusCode.ERROR, "Something bad happened! Console " + console + "may not be available");
		  }
		} finally {
			childSpan.end();
			returnval = true;
		}

		return returnval;
	}
}


