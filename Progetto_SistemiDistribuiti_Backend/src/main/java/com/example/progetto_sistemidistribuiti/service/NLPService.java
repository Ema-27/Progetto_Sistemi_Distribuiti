package com.example.progetto_sistemidistribuiti.service;

import com.amazonaws.services.comprehend.AmazonComprehend;
import com.amazonaws.services.comprehend.model.*;
import io.github.crew102.rapidrake.RakeAlgorithm;
import io.github.crew102.rapidrake.data.SmartWords;
import io.github.crew102.rapidrake.model.RakeParams;
import io.github.crew102.rapidrake.model.Result;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NLPService {

    private final AmazonComprehend comprehend;

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
            "en", "es", "fr", "de", "it", "pt", "ja", "ko", "hi", "ar"
    );

    public NLPService(AmazonComprehend comprehend) {
        this.comprehend = comprehend;
    }

    public List<String> extractKeyPhrases(String rawText, String languageCode) {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalArgumentException("Il testo da analizzare non può essere nullo o vuoto.");
        }

        String cleanText = rawText.replaceAll("-\\r?\\n", "")
                .replaceAll("\\r?\\n", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String inputText = cleanText.length() > 5000 ? cleanText.substring(0, 5000) : cleanText;

        String langCode = languageCode;
        if (langCode == null || langCode.equalsIgnoreCase("auto")) {
            langCode = detectLanguage(cleanText);
        }

        if (!SUPPORTED_LANGUAGES.contains(langCode)) {
            System.out.println("[WARN] Lingua rilevata '" + langCode + "' non supportata da detectKeyPhrases. Uso fallback: 'en'.");
            langCode = "en";
        }

        DetectKeyPhrasesRequest request = new DetectKeyPhrasesRequest()
                .withText(inputText)
                .withLanguageCode(languageCode);

        DetectKeyPhrasesResult result = comprehend.detectKeyPhrases(request);

        return result.getKeyPhrases().stream()
                .filter(kp -> kp.getScore() != null && kp.getScore() >= 0.7f) // score minimo
                .map(KeyPhrase::getText)
                .map(String::trim)
                .filter(phrase -> phrase.length() > 4) // evita spezzoni tipo "As the"
                .filter(phrase -> phrase.split("\\s+").length <= 6) // max 6 parole
                .filter(phrase -> phrase.matches(".*[a-zA-Z]{3,}.*")) // deve contenere parole significative
                .filter(phrase -> !phrase.matches(".*\\d.*")) // esclude numeri puri
                .distinct()
                .limit(20) // opzionale: limita a 20 keyword
                .collect(Collectors.toList());
    }
    private String detectLanguage(String text) {
        DetectDominantLanguageRequest request = new DetectDominantLanguageRequest().withText(text);
        DetectDominantLanguageResult result = comprehend.detectDominantLanguage(request);

        return result.getLanguages().stream()
                .max(Comparator.comparing(DominantLanguage::getScore))
                .orElseThrow(() -> new RuntimeException("Impossibile rilevare la lingua del testo."))
                .getLanguageCode();
    }

}