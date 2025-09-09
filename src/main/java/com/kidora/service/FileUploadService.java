package com.kidora.service;


import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadService {

    private final String uploadDir = "uploads/products/";
    private final String baseUrl = "http://localhost:8080/uploads/products/";

    public FileUploadService() {
        // Create upload directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    public String uploadSingleFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            if(originalFilename == null || !originalFilename.contains(".")) {
                throw new RuntimeException("Invalid file name");
            }
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // Save file to upload directory
            Path targetPath = Paths.get(uploadDir + uniqueFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Return the URL to access the file
            return baseUrl + uniqueFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public List<String> uploadMultipleFiles(MultipartFile[] files) {
        List<String> fileUrls = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                fileUrls.add(uploadSingleFile(file));
            }
        }
        
        return fileUrls;
    }

    public boolean deleteFile(String fileUrl) {
        try {
            // Extract filename from URL
            String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(uploadDir + filename);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }
}
