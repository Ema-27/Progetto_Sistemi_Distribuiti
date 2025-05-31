package com.example.progetto_sistemidistribuiti.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Entity
@Table(name = "document", schema = "sisdis")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false)
    private Integer id;

    @Basic
    @Column(nullable = false)
    private String title;

    @Basic
    @Column
    private String authors;

    @Basic
    @Column
    private int year;

    @Basic
    @Column
    private String paper;

    @Basic
    @Column(length = 2048)
    private String fileUrl;

    @Field(type = FieldType.Text)
    @ElementCollection
    @CollectionTable(name = "document_keywords", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "keyword")
    private List<String> keywords;


    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserProfile owner;

}
