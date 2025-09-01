package com.example.bjj.service;

import com.example.bjj.model.*;
import java.util.List;
import java.util.Set;

public interface TechniqueService {

  List<Technique> find(
      Position position,
      Set<Category> categories,
      Belt maxBelt,
      Boolean giOnly,
      Boolean nogiOnly,
      String query,
      String sort
  );

  // add these:
  Technique save(Technique t);

  void delete(Long id);
}
