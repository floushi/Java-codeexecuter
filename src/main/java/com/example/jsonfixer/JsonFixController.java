package com.example.jsonfixer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
public class JsonFixController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/fix-json")
    public ResponseEntity<String> fixJson(
            @RequestBody String input,
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey) {

        String expectedKey = System.getenv("JSONFIXER_API_KEY");
        if (expectedKey == null || apiKey == null || !expectedKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\": \"Invalid or missing API key\"}");
        }

        try {
            // Prüfe, ob der Input bereits valides JSON ist
            objectMapper.readTree(input);
            return ResponseEntity.ok(input);

        } catch (Exception e1) {
            // Versuch, das JSON zu reparieren
            try {
                String repaired = repairJson(input);
                objectMapper.readTree(repaired); // nochmal validieren
                return ResponseEntity.ok(repaired);
            } catch (Exception e2) {
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Failed to parse JSON after attempting repairs.");
                error.put("original_input", input);
                error.put("attempted_fix", repairJson(input));
                error.put("details", e2.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error.toString());
            }
        }
    }

    private String repairJson(String input) {
        if (input == null || input.isBlank()) return "{}";

        String cleaned = input
                // typografische Zeichen ersetzen
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("‘", "'")
                .replace("’", "'")

                // unescaped doppelte Anführungszeichen escapen
                .replaceAll("(?<!\\\\)\"", "\\\\\"")

                // Newlines/Returns escapen
                .replaceAll("(?<!\\\\)\\n", "\\\\n")
                .replaceAll("(?<!\\\\)\\r", "\\\\r")

                .trim();

        return cleaned;
    }
}
