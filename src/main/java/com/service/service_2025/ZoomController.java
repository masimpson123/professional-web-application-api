package com.service.service_2025;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Signs Zoom Video SDK tokens so the SDK secret never reaches the browser.
 * https://developers.zoom.us/docs/video-sdk/auth/
 */
@RestController
@CrossOrigin
public class ZoomController {

	/** Everyone who opens the video page joins this one session. */
	private static final String SESSION = "msio-video";

	@Value("${zoom.sdk.key}")
	private String sdkKey;

	@Value("${zoom.sdk.secret}")
	private String sdkSecret;

	@GetMapping("/zoom-token")
	public Map<String, String> token() throws Exception {
		if (sdkKey.isBlank() || sdkSecret.isBlank()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Zoom credentials are not configured");
		}

		// Zoom requires exp to be 30 minutes to 48 hours after iat.
		long iat = Instant.now().getEpochSecond() - 30; // allow for clock skew
		String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
		String payload = encode(new Gson().toJson(Map.of(
				"app_key", sdkKey,
				"tpc", SESSION,
				"role_type", 0, // participant; no host is needed to join
				"version", 1,
				"iat", iat,
				"exp", iat + 2 * 60 * 60
		)).getBytes(StandardCharsets.UTF_8));

		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(sdkSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		String signature = encode(mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8)));

		return Map.of("session", SESSION, "token", header + "." + payload + "." + signature);
	}

	private static String encode(byte[] bytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
