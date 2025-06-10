package com.example.progetto_sistemidistribuiti.controllers;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.example.progetto_sistemidistribuiti.model.Document;
import com.example.progetto_sistemidistribuiti.dto.DocumentDto;
import com.example.progetto_sistemidistribuiti.model.UserProfile;
import com.example.progetto_sistemidistribuiti.service.PublicationService;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.progetto_sistemidistribuiti.service.*;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/publications")
public class PublicationController {

    private static final Logger logger = LoggerFactory.getLogger(PublicationController.class);

    private final PublicationService service;
    private final DocumentParserService parser;
    private final NLPService nlp;
    private final RakeKeywordService rakeKeywordService;
    private final UserService userService;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    public PublicationController(PublicationService service,
                                 DocumentParserService parser,
                                 NLPService nlp,
                                 RakeKeywordService rakeKeywordService,
                                 UserService userService) {
        this.service = service;
        this.parser = parser;
        this.nlp = nlp;
        this.rakeKeywordService = rakeKeywordService;
        this.userService = userService;
    }


    @GetMapping("/")
    public ResponseEntity<List<Document>> getAllDocuments() {
        List<Document> documents = service.listPublications();
        return new ResponseEntity<>(documents, HttpStatus.OK);

    }


    @RolesAllowed("user")
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Document> upload(
            @RequestPart("metadata") DocumentDto dto,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "bibtex", required = false) MultipartFile bibtex,
            @RequestPart String email
    ) throws Exception {

        UserProfile user = userService.getUserByEmail(email);
        String nome = user.getFullName();
        dto.setFile(file);
        dto.setBibtex(bibtex);

        Document saved = service.ingest(dto, nome, email);
        return ResponseEntity.ok(saved);
    }


    @GetMapping("/search")
    public ResponseEntity<List<Document>> search(@RequestParam String title) {
        List<Document> results = service.searchByTitle(title);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/keywords")
    public ResponseEntity<List<Document>> searchKeywords(@RequestParam String keywords) {
        List<String> keywordList = Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .toList();

        List<Document> results = service.searchByKeyword(keywordList);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadFileStream(@PathVariable Integer id) {
        Optional<Document> docOpt = service.searchById(id);
        if (docOpt.isEmpty()) {
            logger.warn("Document with id {} not found", id);
            return ResponseEntity.notFound().build();
        }

        Document doc = docOpt.get();

        if (doc.getFileUrl() == null || doc.getFileUrl().isEmpty()) {
            logger.warn("Document with id {} has no fileUrl", id);
            return ResponseEntity.notFound().build();
        }

        try {
            String bucket = extractBucket(doc.getFileUrl());
            String key = extractKey(doc.getFileUrl());

            S3Object s3Object = amazonS3.getObject(bucket, key);
            try (S3ObjectInputStream s3is = s3Object.getObjectContent()) {
                byte[] content = s3is.readAllBytes();

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key.substring(key.lastIndexOf('/') + 1) + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(content);
            }
        } catch (URISyntaxException e) {
            logger.error("Invalid S3 URL syntax for document id {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            logger.error("I/O error while reading S3 object for document id {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            logger.error("Unexpected error during file download for document id {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private String extractBucket(String s3Url) throws URISyntaxException {
        URI uri = new URI(s3Url);
        String host = uri.getHost();
        return host.substring(0, host.indexOf('.'));
    }

    private String extractKey(String s3Url) throws URISyntaxException {
        URI uri = new URI(s3Url);
        String path = uri.getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    @RolesAllowed("user")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePublication(
            @PathVariable Integer id,
            JwtAuthenticationToken token) {

        String loggedEmail = token.getTokenAttributes().get("email").toString();

        Optional<Document> docOpt = service.searchById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Document doc = docOpt.get();
        if (doc.getOwner() == null || !doc.getOwner().getEmail().equalsIgnoreCase(loggedEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Non puoi eliminare una pubblicazione inserita da un altro utente.");
        }

        // Elimina da S3
        if (doc.getFileUrl() != null && !doc.getFileUrl().isEmpty()) {
            try {
                String bucket = extractBucket(doc.getFileUrl());
                String key = extractKey(doc.getFileUrl());
                amazonS3.deleteObject(bucket, key);
            } catch (Exception e) {
                logger.error("Errore durante l'eliminazione da S3: " + e.getMessage());
            }
        }

        UserProfile owner = doc.getOwner();
        if (owner != null) {
            owner.getDocuments().remove(doc);
        }
        service.deleteDocumentById(id);

        return ResponseEntity.ok(Collections.singletonMap("message", "Pubblicazione eliminata."));


    }

    @PostMapping("/extract-keywords")
    public ResponseEntity<List<String>> extractKeywords(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "useRake", defaultValue = "false") boolean useRake
    ) throws Exception {
        String text = parser.extractText(file);
        List<String> phrases = useRake
                ? rakeKeywordService.extractKeyPhrases(text, "en")
                : nlp.extractKeyPhrases(text, "auto");
        return ResponseEntity.ok(phrases);
    }


}
