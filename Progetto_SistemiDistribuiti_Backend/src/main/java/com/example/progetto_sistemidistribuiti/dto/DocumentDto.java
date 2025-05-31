package com.example.progetto_sistemidistribuiti.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class DocumentDto {

    private String title;

    private String authors;

    private int year;

    private String paper;

    private String fileUrl;

    private MultipartFile file;

    //per il caricamento del .bib
    private MultipartFile bibtex;

    private List<String> keywords;

    //per usare Rake o Comprehend
    private boolean useRake = false;

}
