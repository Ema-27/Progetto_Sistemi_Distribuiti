package com.example.progetto_sistemidistribuiti.service;

import com.example.progetto_sistemidistribuiti.dto.DocumentDto;
import com.example.progetto_sistemidistribuiti.model.Document;
import com.example.progetto_sistemidistribuiti.model.UserProfile;
import com.example.progetto_sistemidistribuiti.repository.DocumentRepository;
import com.example.progetto_sistemidistribuiti.repository.UserProfileRepository;
import com.example.progetto_sistemidistribuiti.support.BibTeXParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.List;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserProfileRepository userRepo;

    public Document saveDocument(DocumentDto documentDto, String userId, String fullName) throws IOException {
        // Recupera o crea il profilo dell'utente
        UserProfile user = userRepo.findById(userId).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setId(userId);
            profile.setFullName(fullName);
            return userRepo.save(profile);
        });

        Document document = new Document();

        if (documentDto.getBibtex() != null && !documentDto.getBibtex().isEmpty()) {
            try {
                Map<String, String> metadata = BibTeXParser.parseBibTeX(documentDto.getBibtex().getInputStream());
                document.setTitle(metadata.get("title"));
                document.setAuthors(metadata.get("authors"));
                document.setYear(Integer.parseInt(metadata.get("year")));
                document.setPaper(metadata.get("paper"));
            } catch (Exception e) {
                throw new RuntimeException("Errore parsing BibTeX: " + e.getMessage());
            }
        } else {
            document.setTitle(documentDto.getTitle());
            document.setAuthors(documentDto.getAuthors());
            document.setYear(documentDto.getYear());
            document.setPaper(documentDto.getPaper());
        }

        document.setFileUrl(documentDto.getFileUrl());
        document.setOwner(user);
        document.setKeywords(documentDto.getKeywords());

        return documentRepository.save(document);
    }

    public List<Document> searchByTitle(String title) {
        return documentRepository.findByTitleContainingIgnoreCase(title);
    }
}
