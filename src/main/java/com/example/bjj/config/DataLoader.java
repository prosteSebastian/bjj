package com.example.bjj.config;

import com.example.bjj.model.*;
import com.example.bjj.repo.TechniqueRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class DataLoader {
  @Bean CommandLineRunner seed(TechniqueRepository repo){
    return args -> {
      if(repo.count()>0) return;
      repo.save(Technique.builder().name("Kimura from Closed Guard").position(Position.CLOSED_GUARD)
        .category(Category.SUBMISSION).belt(Belt.WHITE).ruleset(Ruleset.BOTH)
        .description("Control wrist, sit up, figure-four grip, pivot hips, finish perpendicular.")
        .videoUrl("https://www.youtube.com/watch?v=6r0c6nX6Z5U").tags(Set.of("shoulder lock","classic")).build());
      repo.save(Technique.builder().name("Scissor Sweep").position(Position.CLOSED_GUARD)
        .category(Category.SWEEP).belt(Belt.WHITE).ruleset(Ruleset.BOTH)
        .description("Collar-sleeve grips, hip out, shin across, chop & pull to mount.")
        .tags(Set.of("basic sweep","collar & sleeve")).build());
      repo.save(Technique.builder().name("Upa (Bridge & Roll) Escape").position(Position.MOUNT_BOTTOM)
        .category(Category.ESCAPE).belt(Belt.WHITE).ruleset(Ruleset.BOTH)
        .description("Trap arm & foot, bridge high, roll into closed guard or half.")
        .tags(Set.of("trap arm","trap foot")).build());
    };
  }
}
