package com.example.progetto_sistemidistribuiti.service;

import com.example.progetto_sistemidistribuiti.model.Document;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXParser;
import org.jbibtex.Key;
import org.jbibtex.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

@Service
public class BibtexParserService {

    public Document parse(MultipartFile bib) throws Exception {
        Reader reader = new InputStreamReader(bib.getInputStream());
        BibTeXDatabase db = new BibTeXParser().parse(reader);
        BibTeXEntry e = db.getEntries().values().iterator().next();

        Document doc = new Document();
        Value title = e.getField(BibTeXEntry.KEY_TITLE);
        doc.setTitle(title != null ? title.toUserString() : null);

        Value author = e.getField(BibTeXEntry.KEY_AUTHOR);
        if (author != null) {
            List<String> list = Arrays.asList(author.toUserString().split(" and "));
            doc.setAuthors(String.join(", ", list));
        }

        Value year = e.getField(BibTeXEntry.KEY_YEAR);
        if (year != null) doc.setYear(Integer.parseInt(year.toUserString()));

        Value paper = e.getField(new Key("journal"));
        //conferenze
        Value conference = e.getField(new Key("conference"));

        doc.setPaper(conference != null ? conference.toUserString() : (paper != null ? paper.toUserString() : ""));

        return doc;
    }
}