package com.example.progetto_sistemidistribuiti.service;
import com.example.progetto_sistemidistribuiti.dto.DocumentDto;
import com.example.progetto_sistemidistribuiti.model.Document;
import com.example.progetto_sistemidistribuiti.model.UserProfile;
import com.example.progetto_sistemidistribuiti.repository.DocumentRepository;
import com.example.progetto_sistemidistribuiti.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PublicationService {

    private final DocumentRepository repo;
    private final StorageService storage;
    private final DocumentParserService parser;
    private final BibtexParserService bibtexParser;
    private final NLPService nlp;
    private final RakeKeywordService rakeKeywordService;
    //private final SearchService search;

    @Autowired
    private UserProfileRepository userRepo;

    public PublicationService(DocumentRepository repo,
                              StorageService storage,
                              DocumentParserService parser,
                              BibtexParserService bibtexParser,
                              NLPService nlp,
                              RakeKeywordService rakeKeywordService
    ) {
        this.repo = repo;
        this.storage = storage;
        this.parser = parser;
        this.bibtexParser = bibtexParser;
        this.nlp = nlp;
        this.rakeKeywordService = rakeKeywordService;
        //this.search = search;

    }

    @Transactional
    public Document ingest(DocumentDto dto, String name, String email) throws Exception {
        Document doc;
        // Se il file BibTeX è presente, estrai i metadati da BibTeX
        if (dto.getBibtex() != null && !dto.getBibtex().isEmpty()) {
            doc = bibtexParser.parse(dto.getBibtex());
        } else {
            doc = new Document();
            doc.setTitle(dto.getTitle());
            doc.setAuthors(dto.getAuthors());
            doc.setYear(dto.getYear());
            doc.setPaper(dto.getPaper());
        }

        // Carica il file e salva la URL in S3
        String url = storage.upload(dto.getFile());
        doc.setFileUrl(url);

        // 1. Se le keyword sono fornite dal frontend (selezionate/modificate dall’utente), usale direttamente
        if (dto.getKeywords() != null && !dto.getKeywords().isEmpty()) {
            doc.setKeywords(dto.getKeywords());
        } else {
            // 2. Se NON sono state fornite dal frontend, estrai dal testo
            String text = parser.extractText(dto.getFile());
            List<String> phrases = dto.isUseRake()
                    ? rakeKeywordService.extractKeyPhrases(text, "en")
                    : nlp.extractKeyPhrases(text, "auto");
            doc.setKeywords(phrases);
        }

        // Recupera l’utente proprietario dal repository tramite email
        UserProfile user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
        doc.setOwner(user);

        // Salva e restituisci il documento
        return repo.save(doc);
    }





    @Transactional(readOnly = true)
    public List<Document> listPublications() {
        List<Document> docs = repo.findAll();
        return docs;
    }

    public List<Document> searchByTitle(String title) {
        return repo.findByTitleContainingIgnoreCase(title);
    }

    public List<Document> searchByYear(int year) {
        return repo.findByYear(year);
    }

    public List<Document> searchByKeyword(List<String> keywords) {
        return repo.findByMatchingKeywords(keywords);
    }

    public Optional<Document> searchById(Integer id) {
        return repo.findById(id);
    }

    public void deleteDocumentById(Integer id) {
        // Puoi aggiungere anche qui logica extra se serve
        repo.deleteById(id);
    }

}
