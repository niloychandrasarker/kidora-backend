package com.kidora.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class LocalObjectStorageService {
	private final Path fileStorageLocation;
	
	public LocalObjectStorageService(@Value("${file.local-upload-dir}") String uploadDir) throws Exception {
		this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
		try {
			Files.createDirectories(this.fileStorageLocation);
		} catch (Exception ex) {
			throw new Exception("Could not create the directory for local storage.");
		}
	}
	
	public String storeFile(MultipartFile file) {
		String original = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
		try {
			if (original.contains("..")) {
				throw new RuntimeException("Invalid path sequence in filename: " + original);
			}
			String ext = "";
			int dot = original.lastIndexOf('.');
			if (dot >= 0) ext = original.substring(dot);
			String uniqueName = UUID.randomUUID().toString() + ext;
			Path targetLocation = this.fileStorageLocation.resolve(uniqueName);
			Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
			return uniqueName;
		} catch (IOException ex) {
			throw new RuntimeException("Could not store file " + original + ": " + ex.getMessage());
		}
	}

	public byte[] getFileAsBytes(String fileName) {
		try {
			Path filePath = fileStorageLocation.resolve(fileName).normalize();
			// Read the file into a byte array
			byte[] fileBytes = Files.readAllBytes(filePath);
			return fileBytes;
		} catch (IOException ex) {
			// Handle the exception by throwing a LocalStorageException
			throw new RuntimeException("Could not read the file: " + fileName + ":::: " + ex.getMessage());
		}
	}

	public Resource loadFileAsResource(String fileName) {
		try {
			// Resolve the file path
			Path filePath = fileStorageLocation.resolve(fileName).normalize();
			
			// Create a UrlResource from the file path
			Resource resource = new UrlResource(filePath.toUri());
			
			// Check if the resource exists
			if (resource.exists()) {
				return resource;
			} else {
				// Throw an exception if the file does not exist
				throw new RuntimeException("File not found: " + fileName);
			}
		} catch (MalformedURLException ex) {
			// Handle MalformedURLException by throwing a LocalStorageException
			throw new RuntimeException("Error accessing file " + fileName + ": " + ex.getMessage());
		}
	}
	

	public void deleteFile(String fileName) {
		validateFileName(fileName);
		Path fileToDeletePath = fileStorageLocation.resolve(fileName).normalize();
		try {
			// Check if the file exists
			if (!Files.exists(fileToDeletePath)) {
				throw new RuntimeException(String.format("File not found: %s", fileName));
			}
			
			// Delete the file
			Files.delete(fileToDeletePath);
		} catch (IOException e) {
			// Handle any IO exceptions that may occur during deletion
			throw new RuntimeException(String.format("Could not delete file: %s. Reason: %s", fileName, e.getMessage()));
		}
	}
	
	private void validateFileName(String fileName) {
		if (fileName == null || fileName.trim().isEmpty()) {
			throw new RuntimeException("File name cannot be null or empty");
		}
	}
	
	

	public List<String> listAllFiles() {
		List<String> fileList = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(fileStorageLocation)) {
			for (Path path : stream) {
				// Only include files (not directories) in the list
				if (!Files.isDirectory(path)) {
					fileList.add(path.getFileName().toString());
				}
			}
		} catch (IOException e) {
			// Handle any IO exceptions that may occur during listing
			throw new RuntimeException("Could not list files: " + e.getMessage());
		}
		return fileList;
	}

	
	public String getFileAsBase64(String fileName) {
		Path filePath = fileStorageLocation.resolve(fileName).normalize();
		try {
			byte[] fileBytes = Files.readAllBytes(filePath);
			String base64EncodedString = Base64.getEncoder().encodeToString(fileBytes);
			return base64EncodedString;
		} catch (IOException ex) {
			throw new RuntimeException("Could not retrieve and encode file: " + fileName);
		}
	}
	
}
