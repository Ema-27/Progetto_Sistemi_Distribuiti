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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Service
public class DocumentParserService {

    private static final Pattern LATEX_COMMAND_PATTERN = Pattern.compile("\\\\[a-zA-Z]+\\*?(?:\\[[^\\]]*\\])?(?:\\{[^\\}]*\\})*");
    private static final Pattern LATEX_COMMENT_PATTERN = Pattern.compile("%.*$", Pattern.MULTILINE);
    private static final Pattern LATEX_MATH_PATTERN = Pattern.compile("\\$[^\\$]*\\$|\\$\\$[^\\$]*\\$\\$");
    private static final Pattern LATEX_ENVIRONMENT_PATTERN = Pattern.compile("\\\\begin\\{[^\\}]*\\}.*?\\\\end\\{[^\\}]*\\}", Pattern.DOTALL);

    public String extractText(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if(filename.endsWith(".tex") || filename.endsWith(".latex"))
            return extractLatexText(file);
        String text = extractWithTika(file);
        if (text == null || text.isBlank()) {
            if (filename != null && filename.endsWith(".pdf")) {
                System.out.println("[FALLBACK] PDF vuoto da Tika, provo PDFBox");
                return extractWithPDFBox(file);
            } else if (filename != null && filename.endsWith(".docx")) {
                System.out.println("[FALLBACK] DOCX vuoto da Tika, provo Apache POI");
                return extractWithPOI(file);
            } else if (filename != null && (filename.endsWith(".tex") || filename.endsWith(".latex"))) {
                System.out.println("[FALLBACK] LaTeX vuoto da Tika, provo estrazione manuale");
                return extractLatexText(file);
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


    private String extractLatexText(MultipartFile file) throws Exception {
        StringBuilder content = new StringBuilder();

        try (InputStream inputStream = file.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String latexContent = content.toString();


        latexContent = LATEX_COMMENT_PATTERN.matcher(latexContent).replaceAll("");

        latexContent = LATEX_MATH_PATTERN.matcher(latexContent).replaceAll(" ");

        latexContent = removeSpecificEnvironments(latexContent);

        latexContent = cleanLatexCommands(latexContent);

        return latexContent.replaceAll("\\s+", " ").trim();
    }


    private String removeSpecificEnvironments(String content) {
        String[] environmentsToRemove = {
                "equation", "align", "gather", "multline", "split",
                "verbatim", "lstlisting", "minted", "tikzpicture",
                "tabular", "array", "matrix"
        };

        for (String env : environmentsToRemove) {
            Pattern pattern = Pattern.compile(
                    "\\\\begin\\{" + env + "\\*?\\}.*?\\\\end\\{" + env + "\\*?\\}",
                    Pattern.DOTALL | Pattern.CASE_INSENSITIVE
            );
            content = pattern.matcher(content).replaceAll(" ");
        }

        return content;
    }

    private String cleanLatexCommands(String content) {
        content = content.replaceAll("\\\\textbf\\{([^\\}]*)\\}", "$1"); // grassetto
        content = content.replaceAll("\\\\textit\\{([^\\}]*)\\}", "$1"); // corsivo
        content = content.replaceAll("\\\\emph\\{([^\\}]*)\\}", "$1");   // enfasi
        content = content.replaceAll("\\\\underline\\{([^\\}]*)\\}", "$1"); // sottolineato
        content = content.replaceAll("\\\\title\\{([^\\}]*)\\}", "$1");  // titolo
        content = content.replaceAll("\\\\author\\{([^\\}]*)\\}", "$1"); // autore
        content = content.replaceAll("\\\\section\\*?\\{([^\\}]*)\\}", "$1"); // sezioni
        content = content.replaceAll("\\\\subsection\\*?\\{([^\\}]*)\\}", "$1");
        content = content.replaceAll("\\\\subsubsection\\*?\\{([^\\}]*)\\}", "$1");
        content = content.replaceAll("\\\\chapter\\*?\\{([^\\}]*)\\}", "$1");
        content = content.replaceAll("\\\\paragraph\\{([^\\}]*)\\}", "$1");
        content = content.replaceAll("\\\\caption\\{([^\\}]*)\\}", "$1");
        content = content.replaceAll("\\\\label\\{([^\\}]*)\\}", ""); // rimuovi label

        content = LATEX_COMMAND_PATTERN.matcher(content).replaceAll(" ");

        content = content.replaceAll("[\\{\\}]", " ");

        return content;
    }
}