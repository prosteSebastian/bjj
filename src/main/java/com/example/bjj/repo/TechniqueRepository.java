package com.example.bjj.repo;

import com.example.bjj.model.Technique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TechniqueRepository extends JpaRepository<Technique, Long>, JpaSpecificationExecutor<Technique> { }
