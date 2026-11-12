package com.example.login.post.image;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {
    private final ImageService imageService;


    @PostMapping
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file){
        String storeFilename = imageService.storeImage(file);
        String imageUrl = "/api/v1/images/" + storeFilename;

        //created 코드 및 location 헤더, JSON 반환
        return ResponseEntity.created(URI.create(imageUrl)).body(Map.of("url", imageUrl));
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> saveImage(
            @PathVariable("fileName") String fileName
    ){
        Resource resource = imageService.loadImageAsResource(fileName);
        String contentType = "application/octet-stream";
        try {
            String detectedType = Files.probeContentType(Paths.get(resource.getURI()));
            if (detectedType != null) {
                contentType = detectedType;
            }
        } catch (IOException ignored) {
            //
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
