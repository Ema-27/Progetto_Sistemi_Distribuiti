package com.example.progetto_sistemidistribuiti.repository;

import com.example.progetto_sistemidistribuiti.model.Document;
import com.example.progetto_sistemidistribuiti.model.UserProfile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {

    List<Document> findByTitleContainingIgnoreCase(String title);

    List<Document> findByYear(int year);

    List<Document> findAll();

    @Query("SELECT DISTINCT d FROM Document d JOIN d.keywords k WHERE k IN :keywords")
    List<Document> findByMatchingKeywords(@Param("keywords") List<String> keywords);

    Optional<Document> findById(Integer id);

    List<Document> findByOwner(UserProfile user);



}
