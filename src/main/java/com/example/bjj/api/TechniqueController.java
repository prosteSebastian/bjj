package com.example.bjj.api;

import com.example.bjj.model.*;
import com.example.bjj.service.TechniqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/techniques")
@RequiredArgsConstructor
@CrossOrigin
public class TechniqueController {
  private final TechniqueService service;

  @GetMapping
  public List<Technique> list(
      @RequestParam(required = false) Position position,
      @RequestParam(required = false) Set<Category> category,
      @RequestParam(required = false) Belt belt, // max level
      @RequestParam(required = false) Boolean giOnly,
      @RequestParam(required = false) Boolean nogiOnly,
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "relevance") String sort
  ){
    return service.find(position, category, belt, giOnly, nogiOnly, q, sort);
  }

  @PostMapping public Technique create(@RequestBody Technique t){ return service.save(t); }
  @PutMapping("/{id}") public Technique update(@PathVariable Long id, @RequestBody Technique t){ t.setId(id); return service.save(t); }
  @DeleteMapping("/{id}") public void delete(@PathVariable Long id){ service.delete(id); }
}
