package com.example.bjj.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Technique {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @Enumerated(EnumType.STRING)
  private Position position;

  @Enumerated(EnumType.STRING)
  private Category category;

  @Enumerated(EnumType.STRING)
  private Belt belt; // suggested minimum

  @Enumerated(EnumType.STRING)
  private Ruleset ruleset; // GI / NOGI / BOTH

  @Column(length = 800)
  private String description;

  private String videoUrl;
  private String thumbnailUrl;

  @ElementCollection(fetch = FetchType.EAGER)
  private Set<String> tags;
}
