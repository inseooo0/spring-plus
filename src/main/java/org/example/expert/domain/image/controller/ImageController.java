package org.example.expert.domain.image.controller;

import com.amazonaws.services.s3.AmazonS3Client;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.image.service.ImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class ImageController {

    private final AmazonS3Client amazonS3Client;
    private final ImageService imageService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @PostMapping("/users/images")
    public ResponseEntity<?> uploadUserProfileImage(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        String profileImageUrl = imageService.uploadUserProfileImage(authUser.getId(), image);
        return ResponseEntity.ok(profileImageUrl);
    }

    @DeleteMapping("/users/images")
    public ResponseEntity<?> deleteUserProfileImage(@AuthenticationPrincipal AuthUser authUser) {
        imageService.deleteUserProfileImage(authUser.getId());
        return ResponseEntity.ok("프로필 이미지가 삭제되었습니다.");
    }

    @GetMapping("/users/images")
    public ResponseEntity<?> getUserProfileImage(@AuthenticationPrincipal AuthUser authUser) {
        String profileImageUrl = imageService.getUserProfileImage(authUser.getId());
        return ResponseEntity.ok(profileImageUrl);
    }
}
