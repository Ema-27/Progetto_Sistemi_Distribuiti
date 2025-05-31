package com.example.progetto_sistemidistribuiti.controllers;

import com.example.progetto_sistemidistribuiti.dto.DocumentDto;
import com.example.progetto_sistemidistribuiti.model.Document;
import com.example.progetto_sistemidistribuiti.service.DocumentService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @RolesAllowed("user")
    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(
            @RequestPart("metadata") DocumentDto documentDto,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "bibtex", required = false) MultipartFile bibtex,
            JwtAuthenticationToken token) {

        try {
            documentDto.setFile(file);
            documentDto.setBibtex(bibtex);

            // Dati dal token Keycloak
            String userId = token.getToken().getSubject(); // UUID Keycloak (sub)
            String fullName = token.getToken().getClaimAsString("name"); // nome utente (se configurato)

            documentService.saveDocument(documentDto, userId, fullName);

            return ResponseEntity.status(HttpStatus.CREATED).body("Document has been added");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Document>> searchByTitle(@RequestParam String title) {
        List<Document> results = documentService.searchByTitle(title);
        return ResponseEntity.ok(results);
    }
}
