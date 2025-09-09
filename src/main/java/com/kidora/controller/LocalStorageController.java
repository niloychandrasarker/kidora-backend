
package com.kidora.controller;


import com.kidora.service.LocalObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



@RestController
@RequestMapping("/api/local/storage")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LocalStorageController {
	private final LocalObjectStorageService localObjectStorageService;
	
	@PostMapping("/upload")
	public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
		localObjectStorageService.storeFile(file);
		return ResponseEntity.ok("File Uploaded successfully.");
	}
	
	@GetMapping("/download")
	public ResponseEntity<Resource> downloadFile(@RequestParam("file") String fileName) {
		Resource resource = localObjectStorageService.loadFileAsResource(fileName);
		return ResponseEntity.ok(resource);
	}
	
	@DeleteMapping("/delete")
	public ResponseEntity<String> deleteFile(@RequestParam("file") String fileName) {
		localObjectStorageService.deleteFile(fileName);
		return ResponseEntity.ok("File deleted successfully.");
	}
	
	@GetMapping("/base64")
	public ResponseEntity<String> getFileAsBase64(@RequestParam("file") String fileName) {
		String base64Data = localObjectStorageService.getFileAsBase64(fileName);
		return ResponseEntity.ok(base64Data);
	}
}