package com.example.bjj.service;

import com.example.bjj.model.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

public class TechniqueSpecs {
  public static Specification<Technique> filter(Position position, Set<Category> categories, Belt maxBelt,
                                                Boolean giOnly, Boolean nogiOnly, String search) {
    return (root, q, cb) -> {
      var preds = new java.util.ArrayList<Predicate>();
      if (position != null) preds.add(cb.equal(root.get("position"), position));
      if (categories != null && !categories.isEmpty()) preds.add(root.get("category").in(categories));
      if (maxBelt != null) preds.add(cb.lessThanOrEqualTo(root.get("belt"), maxBelt));
      if (Boolean.TRUE.equals(giOnly)) preds.add(cb.notEqual(root.get("ruleset"), Ruleset.NOGI));
      if (Boolean.TRUE.equals(nogiOnly)) preds.add(cb.notEqual(root.get("ruleset"), Ruleset.GI));
      if (search != null && !search.isBlank()) {
        String like = "%" + search.toLowerCase() + "%";
        preds.add(cb.or(
          cb.like(cb.lower(root.get("name")), like),
          cb.like(cb.lower(root.get("description")), like)
        ));
      }
      return cb.and(preds.toArray(Predicate[]::new));
    };
  }
}
