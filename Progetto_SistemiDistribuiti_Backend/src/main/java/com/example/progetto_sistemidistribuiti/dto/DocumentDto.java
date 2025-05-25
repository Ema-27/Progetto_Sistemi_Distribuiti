package com.example.progetto_sistemidistribuiti.dto;

import com.example.progetto_sistemidistribuiti.model.Document;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@ToString
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

}
