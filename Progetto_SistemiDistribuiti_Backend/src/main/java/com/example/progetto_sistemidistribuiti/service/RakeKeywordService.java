package com.example.progetto_sistemidistribuiti.service;

import io.github.crew102.rapidrake.RakeAlgorithm;
import io.github.crew102.rapidrake.data.SmartWords;
import io.github.crew102.rapidrake.model.RakeParams;
import io.github.crew102.rapidrake.model.Result;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

@Service
public class RakeKeywordService {

    private RakeAlgorithm rake;

    @PostConstruct
    public void init() throws IOException {
        String[] stopWords = new SmartWords().getSmartWords();
        String[] stopPOS = {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};

        RakeParams params = new RakeParams(stopWords, stopPOS, 4, true, "[-,.?():;\"!/]");

        File posModel = extractTempFile("model-bin/en-pos-maxent.bin");
        File sentModel = extractTempFile("model-bin/en-sent.bin");

        this.rake = new RakeAlgorithm(params, posModel.getAbsolutePath(), sentModel.getAbsolutePath());
    }

    private File extractTempFile(String resourcePath) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) throw new IOException("Model not found: " + resourcePath);
            File tempFile = File.createTempFile("model", ".bin");
            tempFile.deleteOnExit();
            Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        }
    }

    public List<String> extractKeyPhrases(String text, String lang) {
        if (text == null || text.isBlank()) return List.of();
        try {
            Result result = rake.rake(text);

            return Arrays.stream(result.getFullKeywords())
                    .map(String::trim)
                    .filter(k -> k.length() > 4)                           // almeno 5 caratteri
                    .filter(k -> k.split("\\s+").length <= 5)             // massimo 5 parole
                    .filter(k -> !k.matches(".*\\d.*"))                   // nessun numero
                    .filter(k -> k.matches(".*[a-zA-Z]{3,}.*"))           // almeno 3 lettere di fila
                    .distinct()
                    .limit(15)                                            // massimo 30 keyword
                    .toList();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }


    private boolean isKeywordValid(String k) {
        if (k.length() <= 4) return false;                               // almeno 5 caratteri
        if (k.split("\\s+").length > 5) return false;                    // max 5 parole
        if (k.matches(".*\\d.*")) return false;                          // esclude numeri
        return k.matches(".*[a-zA-Z]{3,}.*");                            // almeno una parola con 3 lettere
    }
}