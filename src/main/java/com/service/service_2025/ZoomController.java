package com.service.service_2025;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Signs Zoom Video SDK tokens so the SDK secret never reaches the browser.
 * https://developers.zoom.us/docs/video-sdk/auth/
 */
@RestController
@CrossOrigin
public class ZoomController {

	/** Every session room in the Zoom telehealth demo is named like this, so tokens can't be minted for anything else. */
	private static final Pattern SESSION = Pattern.compile("cs-[a-z0-9]{1,32}");

	@Value("${zoom.sdk.key}")
	private String sdkKey;

	@Value("${zoom.sdk.secret}")
	private String sdkSecret;

	/**
	 * @param session the Zoom session (room) to join
	 * @param role    {@code host} or {@code participant}; hosts can end the session for everyone
	 * @param userKey stable label for this visitor, which Zoom reports back on each participant
	 */
	@GetMapping("/zoom-token")
	public Map<String, String> token(
			@RequestParam String session,
			@RequestParam(defaultValue = "participant") String role,
			@RequestParam(required = false) String userKey) throws Exception {
		if (sdkKey.isBlank() || sdkSecret.isBlank()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Zoom credentials are not configured");
		}
		if (!SESSION.matcher(session).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown session");
		}
		if (!role.equals("host") && !role.equals("participant")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be host or participant");
		}

		// Zoom requires exp to be 30 minutes to 48 hours after iat.
		long iat = Instant.now().getEpochSecond() - 30; // allow for clock skew
		Map<String, Object> claims = new HashMap<>(Map.of(
				"app_key", sdkKey,
				"tpc", session,
				"role_type", role.equals("host") ? 1 : 0,
				"version", 1,
				"iat", iat,
				"exp", iat + 2 * 60 * 60
		));
		if (userKey != null && !userKey.isBlank()) {
			claims.put("user_key", userKey.substring(0, Math.min(userKey.length(), 36))); // Zoom's limit
		}

		String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
		String payload = encode(new Gson().toJson(claims).getBytes(StandardCharsets.UTF_8));

		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(sdkSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		String signature = encode(mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8)));

		return Map.of("session", session, "token", header + "." + payload + "." + signature);
	}

	private static String encode(byte[] bytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
