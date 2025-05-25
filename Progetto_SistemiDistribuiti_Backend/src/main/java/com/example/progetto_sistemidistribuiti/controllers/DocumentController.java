package com.example.progetto_sistemidistribuiti.controllers;

import com.example.progetto_sistemidistribuiti.dto.DocumentDto;
import com.example.progetto_sistemidistribuiti.model.Document;
import com.example.progetto_sistemidistribuiti.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired private DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(
            @RequestPart("metadata") DocumentDto documentDto,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "bibtex", required = false) MultipartFile bibtex) {
        try {
            documentDto.setFile(file);
            documentDto.setBibtex(bibtex);
            documentService.saveDocument(documentDto);
            return new ResponseEntity("Document has been added", HttpStatus.CREATED);
        }
        catch (Exception e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Document>> searchByTitle(@RequestParam String title) {
        List<Document> results = documentService.searchByTitle(title);
        return ResponseEntity.ok(results);
    }
}
