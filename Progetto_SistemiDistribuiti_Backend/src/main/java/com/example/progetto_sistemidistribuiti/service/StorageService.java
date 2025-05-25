package com.example.progetto_sistemidistribuiti.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
    String upload(MultipartFile file) throws IOException;
}
