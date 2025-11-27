package tech.hellsoft.trading.config;

import java.io.Serializable;

public record Configuration(String apiKey, String team, String host, String species,
                            String snapshotsDir) implements Serializable {

  public Configuration {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalArgumentException("API key cannot be null or blank");
    }
    if (team == null || team.isBlank()) {
      throw new IllegalArgumentException("Team cannot be null or blank");
    }
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("Host cannot be null or blank");
    }
    species = normalize(species);
    snapshotsDir = valueOrDefault(snapshotsDir, "snapshots");
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed;
  }

  private static String valueOrDefault(String value, String defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return value;
  }
}
