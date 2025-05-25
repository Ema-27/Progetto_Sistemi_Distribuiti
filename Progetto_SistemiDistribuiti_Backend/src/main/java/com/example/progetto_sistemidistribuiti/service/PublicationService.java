package com.example.progetto_sistemidistribuiti.service;
import com.example.progetto_sistemidistribuiti.dto.DocumentDto;
import com.example.progetto_sistemidistribuiti.model.Document;
import com.example.progetto_sistemidistribuiti.repository.DocumentRepository;
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
    public Document ingest(DocumentDto dto) throws Exception {
        Document doc;
        if (dto.getBibtex() != null && !dto.getBibtex().isEmpty()) {
            doc = bibtexParser.parse(dto.getBibtex());
        } else {
            doc = new Document();
            doc.setTitle(dto.getTitle());
            doc.setAuthors(dto.getAuthors());
            doc.setYear(dto.getYear());
            doc.setPaper(dto.getPaper());
        }
        String url = storage.upload(dto.getFile());
        doc.setFileUrl(url);
        String text = parser.extractText(dto.getFile());
        //devo inserire la scelta se usare RAKE o Comprehend
        List<String> phrases = nlp.extractKeyPhrases(text, "en");

        doc.setKeywords(phrases);

        Document saved = repo.save(doc);
        return saved;
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
}
