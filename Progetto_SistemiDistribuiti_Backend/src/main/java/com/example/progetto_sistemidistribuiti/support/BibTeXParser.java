package com.example.progetto_sistemidistribuiti.support;


import org.jbibtex.*;

import java.io.*;
import java.util.*;

public class BibTeXParser {

    public static Map<String, String> parseBibTeX(InputStream bibtexInputStream) throws IOException, ParseException {
        Reader reader = new InputStreamReader(bibtexInputStream);
        BibTeXDatabase database = new org.jbibtex.BibTeXParser().parseFully(reader);

        Map<Key, BibTeXEntry> entries = database.getEntries();
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("BibTeX file contains no entries");
        }

        BibTeXEntry entry = entries.values().iterator().next();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("title", entry.getField(BibTeXEntry.KEY_TITLE).toUserString());
        metadata.put("authors", entry.getField(BibTeXEntry.KEY_AUTHOR).toUserString());
        metadata.put("year", entry.getField(BibTeXEntry.KEY_YEAR).toUserString());
        //riviste
        Value paper = entry.getField(new Key("paper"));
        //conferenze
        Value conference = entry.getField(new Key("conference"));

        metadata.put("paper", conference != null ? conference.toUserString() : (paper != null ? paper.toUserString() : ""));

        return metadata;
    }
}