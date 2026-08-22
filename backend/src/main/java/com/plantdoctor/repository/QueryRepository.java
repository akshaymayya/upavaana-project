package com.plantdoctor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plantdoctor.entity.Query;

@Repository
public interface QueryRepository extends JpaRepository<Query, Integer> {
}
