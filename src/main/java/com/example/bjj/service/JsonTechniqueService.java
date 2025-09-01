package com.example.bjj.service;

import com.example.bjj.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class JsonTechniqueService implements TechniqueService {

  private static final Logger log = LoggerFactory.getLogger(JsonTechniqueService.class);

  private final List<Technique> all = new ArrayList<>();
  private final Map<Long, Technique> byId = new HashMap<>();
  private final AtomicLong seq = new AtomicLong(1);

  public JsonTechniqueService() {
    loadFromJson("techniques.json"); // src/main/resources/techniques.json
  }

  /* ===================== Public API used by MainView ===================== */

  @Override
  public List<Technique> find(Position position,
                              Set<Category> categories,
                              Belt maxBelt,
                              Boolean giOnly,
                              Boolean nogiOnly,
                              String query,
                              String sort) {
    String q = query == null ? "" : query.trim().toLowerCase();

    return all.stream()
        // position filter
        .filter(t -> position == null || Objects.equals(t.getPosition(), position))
        // categories (show only selected)
        .filter(t -> categories == null || categories.isEmpty() || categories.contains(t.getCategory()))
        // belt filter: include techniques up to max belt
        .filter(t -> {
          if (maxBelt == null || t.getBelt() == null) return true;
          return t.getBelt().ordinal() <= maxBelt.ordinal();
        })
        // gi/nogi filter
        .filter(t -> {
          if (Boolean.TRUE.equals(giOnly)) {
            return t.getRuleset() == null || t.getRuleset() == Ruleset.GI || t.getRuleset() == Ruleset.BOTH;
          }
          if (Boolean.TRUE.equals(nogiOnly)) {
            return t.getRuleset() == null || t.getRuleset() == Ruleset.NOGI || t.getRuleset() == Ruleset.BOTH;
          }
          return true;
        })
        // text search
        .filter(t -> {
          if (q.isBlank()) return true;
          String name = orEmpty(t.getName());
          String desc = orEmpty(t.getDescription());
          return name.toLowerCase().contains(q) || desc.toLowerCase().contains(q);
        })
        // sorting
        .sorted((a, b) -> {
          String s = sort == null ? "relevance" : sort;
          switch (s) {
            case "alpha":
              return orEmpty(a.getName()).compareToIgnoreCase(orEmpty(b.getName()));
            case "belt":
              int ao = a.getBelt() == null ? 999 : a.getBelt().ordinal();
              int bo = b.getBelt() == null ? 999 : b.getBelt().ordinal();
              int cmp = Integer.compare(ao, bo);
              return (cmp != 0) ? cmp : orEmpty(a.getName()).compareToIgnoreCase(orEmpty(b.getName()));
            case "relevance":
            default:
              // very simple relevance scorer
              int ra = relevance(a, q);
              int rb = relevance(b, q);
              int rcmp = Integer.compare(ra, rb);
              return (rcmp != 0) ? rcmp : orEmpty(a.getName()).compareToIgnoreCase(orEmpty(b.getName()));
          }
        })
        .collect(Collectors.toList());
  }

  /* ===== Optional basic CRUD (no-op persistence; keeps TechniqueController happy) ===== */

  public Technique save(Technique t) {
    if (t.getId() == null) {
      t.setId(seq.getAndIncrement());
      all.add(t);
      byId.put(t.getId(), t);
    } else if (!byId.containsKey(t.getId())) {
      all.add(t);
      byId.put(t.getId(), t);
    } else {
      // replace existing
      byId.put(t.getId(), t);
      for (int i = 0; i < all.size(); i++) {
        if (Objects.equals(all.get(i).getId(), t.getId())) {
          all.set(i, t);
          break;
        }
      }
    }
    return t;
  }

  public void delete(Long id) {
    if (id == null) return;
    byId.remove(id);
    all.removeIf(x -> Objects.equals(x.getId(), id));
  }

  /* ===================== JSON loader ===================== */

  private void loadFromJson(String resourceName) {
    try {
      var res = new ClassPathResource(resourceName);
      if (!res.exists()) {
        log.warn("JSON techniques file '{}' not found. Starting with an empty list.", resourceName);
        return;
      }
      try (InputStream in = res.getInputStream()) {
        var mapper = new ObjectMapper();
        var root = mapper.readTree(in);

        if (!root.isArray()) {
          log.warn("Expected array at root of '{}', got {}", resourceName, root.getNodeType());
          return;
        }

        for (JsonNode n : root) {
          Technique t = new Technique();
          t.setId(n.path("id").isNumber() ? n.get("id").asLong() : seq.getAndIncrement());
          t.setName(n.path("name").asText(null));
          t.setDescription(n.path("description").asText(null));
          t.setVideoUrl(n.path("videoUrl").asText(null));
          t.setThumbnailUrl(n.path("thumbnailUrl").asText(null));

          t.setPosition(parseEnumOrNull(Position.class, n.path("position").asText(null)));
          t.setCategory(parseEnumOrNull(Category.class, n.path("category").asText(null)));
          t.setBelt(parseEnumOrNull(Belt.class, n.path("belt").asText(null)));
          // default ruleset to BOTH if missing
          Ruleset rs = parseEnumOrNull(Ruleset.class, n.path("ruleset").asText(null));
          t.setRuleset(rs == null ? Ruleset.BOTH : rs);

          save(t); // adds to in-memory store
        }

        log.info("Loaded {} techniques from JSON.", all.size());
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to load techniques from " + resourceName, e);
    }
  }

  /* ===================== Helpers ===================== */

  private static String orEmpty(String s) { return s == null ? "" : s; }

  /** very light relevance: exact prefix beats contains-in-name beats contains-in-desc */
  private static int relevance(Technique t, String q) {
    if (q == null || q.isBlank()) return 1000;
    String name = orEmpty(t.getName()).toLowerCase();
    String desc = orEmpty(t.getDescription()).toLowerCase();
    if (name.startsWith(q)) return 0;
    if (name.contains(q)) return 1;
    if (desc.contains(q)) return 2;
    return 3;
  }

  /** Normalize "Half Guard", "half_guard", "HALF-GUARD" -> "HALF_GUARD". */
  private static String norm(String s) {
    if (s == null) return null;
    return s.trim()
        .replace('/', '_')
        .replace('-', '_')
        .replace(' ', '_')
        .toUpperCase();
  }

  private static <E extends Enum<E>> E parseEnumOrNull(Class<E> type, String raw) {
    if (raw == null || raw.isBlank()) return null;
    String key = norm(raw);
    try {
      return Enum.valueOf(type, key);
    } catch (IllegalArgumentException ex) {
      // a few common aliases for convenience
      if (type == Ruleset.class) {
        if ("NO_GI".equals(key) || "NOGI".equals(key)) return (E) Ruleset.NOGI;
        if ("GI_ONLY".equals(key)) return (E) Ruleset.GI;
        if ("ANY".equals(key)) return (E) Ruleset.BOTH;
      }
      log.warn("Unknown {} enum value '{}'; storing null", type.getSimpleName(), raw);
      return null;
    }
  }
}
