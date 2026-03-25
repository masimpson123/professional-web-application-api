package com.service.service_2025;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.FirebaseOptions;
import com.google.gson.Gson;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

// import java.io.FileInputStream;

@SpringBootApplication
@RestController
@CrossOrigin
@EnableWebSocketMessageBroker
public class Service2025Application implements WebSocketMessageBrokerConfigurer {

	public static void main(String[] args) {
		SpringApplication.run(Service2025Application.class, args);
	}

	@GetMapping("/")
	public String home() {
		return "Welcome to this <b>Java</b> microservice!<br>" +
				"This service was created using <b>Spring Initializer</b> and packaged using <b>Maven</b>.<br>" +
				"It was containerized using <b>Docker</b>.<br>" +
				"It is hosted using <b>GCP Cloud Run</b>.<br>";
	}

	@GetMapping("/weather")
	public String weather(@RequestHeader(value = "Authorization", required = false) String authHeader) {
		try {
			this.connectToFirebase();
			String uid = this.decodeAuthToken(authHeader).getUid();
			return "{\"response\":\"Hello user " + uid + ", the weather is crisp, cool, and sunny.\"}";
		} catch (Exception e) {
			return "{\"error\":\"" + e + "\"}";
		}
	}

	@GetMapping("/weather-advanced")
	public String weatherAdvanced(@RequestHeader(value = "Authorization", required = false) String authHeader) {
		try {
			this.connectToFirebase();
			FirebaseToken token = this.decodeAuthToken(authHeader);
			String uid = token.getUid();
			String message = (Boolean.TRUE.equals(token.getClaims().get("advanced-usage"))) ?
					"Hello advanced user " + uid + ", wind speed is 39 miles per hour." :
					"Hello user " + uid + ", you do not have the advanced usage claim.";
			return "{\"response\":\"" + message + "\"}";
		} catch (Exception e) {
			return "{\"error\":\"" + e + "\"}";
		}
	}

	@GetMapping("/request-advanced-usage-claim")
public String advancedUsage(@RequestHeader(value = "Authorization", required = false) String authHeader) {
		try {
			this.connectToFirebase();
			String uid = this.decodeAuthToken(authHeader).getUid();
			Map<String, Object> claims = new HashMap<>();
			claims.put("advanced-usage", true);
			FirebaseAuth.getInstance().setCustomUserClaims(uid, claims);
			return "{\"response\":\"Hello user " + uid + ", your request for the advanced usage claim has been approved!\"}";
		} catch (Exception e) {
			return "{\"error\":\"" + e + "\"}";
		}
	}

	@PostMapping("/ai")
	public String query(@RequestBody AIQuery requestBody) throws IOException, InterruptedException {
		String response = "no data";
		try(HttpClient client = HttpClient.newHttpClient()) {
			String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(apiUrl))
					.header("Content-Type", "application/json")
					// the below key must remain hidden
					.header("X-goog-api-key", "")
					.POST(HttpRequest.BodyPublishers.ofString(requestBody.getQuery()))
					.build();
			response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
		}

		return new Gson().toJson(Map.of("response", response));
	}

	@GetMapping("/search/{searchTerm}")
	public ResponseEntity<StreamingResponseBody> search(@PathVariable String searchTerm) {
		StreamingResponseBody responseBody = outputStream -> {
			Data data = new Data();

			CompletableFuture<String> placesFuture =
				CompletableFuture.supplyAsync(() -> {
					pause();
					writeToStream(outputStream, "places", filterList(data.places, searchTerm));
					return "places search is done";
				});

			CompletableFuture<String> itemsFuture =
				CompletableFuture.supplyAsync(() -> {
					pause();
					writeToStream(outputStream, "items", filterList(data.items, searchTerm));
					return "item search is done";
				});

			CompletableFuture<String> booksFuture =
				CompletableFuture.supplyAsync(() -> {
					pause();
					writeToStream(outputStream, "books", filterList(data.books, searchTerm));
					return "books search is done";
				});

			try {
				CompletableFuture.allOf(placesFuture, itemsFuture, booksFuture).get();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		};

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(responseBody);
	}

	private void pause() {
		try {
			Thread.sleep((int)(5_000 * Math.random()));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private List<String> filterList(List<String> list, String searchTerm) {
		return new ArrayList<>(list)
				.stream()
				.filter(listItem -> listItem.toLowerCase().contains(searchTerm.toLowerCase()))
				.toList();
	}

	private void writeToStream(OutputStream outputStream, String dataIdentifier, List<String> data) {
		try {
			outputStream.write(new Gson().toJson(Map.of(dataIdentifier, data)).getBytes());
			outputStream.flush();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	@MessageMapping("/websocket-input")
	@SendTo("/websocket-output")
	public String websocket(String message) {
		return LocalTime.now() + ": " + message;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/websocket-broker")
				// .setAllowedOrigins("http://localhost:4200");
				.setAllowedOrigins("https://msio-u7qjhl7iia-uc.a.run.app");
	}

	private void connectToFirebase() throws Exception {
		if (FirebaseApp.getApps().isEmpty()) {
				// FileInputStream serviceAccount = new FileInputStream("/Users/michaelsimpson/Documents/bingo.json");
				FirebaseOptions options = FirebaseOptions.builder()
						// .setCredentials(GoogleCredentials.fromStream(serviceAccount))
						.setCredentials(GoogleCredentials.getApplicationDefault())
						.setProjectId("endpoint-one")
						.build();
				FirebaseApp.initializeApp(options);
		}
	}

	private FirebaseToken decodeAuthToken(String authHeader) throws Exception {
			return FirebaseAuth.getInstance().verifyIdToken(authHeader.substring(7));
	}
}
