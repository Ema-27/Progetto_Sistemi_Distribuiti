package com.example.progetto_sistemidistribuiti.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;

import java.io.InputStream;

@Service
public class DocumentParserService {

    public String extractText(MultipartFile file) throws Exception {
        String text = extractWithTika(file);
        if (text == null || text.isBlank()) {
            String filename = file.getOriginalFilename();
            if (filename != null && filename.endsWith(".pdf")) {
                System.out.println("[FALLBACK] PDF vuoto da Tika, provo PDFBox");
                return extractWithPDFBox(file);
            } else if (filename != null && filename.endsWith(".docx")) {
                System.out.println("[FALLBACK] DOCX vuoto da Tika, provo Apache POI");
                return extractWithPOI(file);
            } else {
                System.out.println("[WARN] Tika non ha estratto testo utile.");
                return "";
            }
        }
        return text;
    }

    private String extractWithTika(MultipartFile file) throws Exception {
        try (InputStream stream = file.getInputStream()) {
            ContentHandler handler = new BodyContentHandler(-1); // no char limit
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            ParseContext context = new ParseContext();
            parser.parse(stream, handler, metadata, context);

            String content = handler.toString().replaceAll("\\s+", " ").trim();

            return content;
        }
    }

    private String extractWithPDFBox(MultipartFile file) throws Exception {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ").trim();
        }
    }

    private String extractWithPOI(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            XWPFDocument doc = new XWPFDocument(inputStream);
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            return extractor.getText().replaceAll("\\s+", " ").trim();
        }
    }
}
