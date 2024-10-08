package org.example.expert.domain.image.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.IOUtils;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    private final AmazonS3Client amazonS3Client;
    private final UserRepository userRepository;

    public String upload(MultipartFile image) {
        //입력받은 이미지 파일이 빈 파일인지 검증
        if(image == null || image.isEmpty() || Objects.isNull(image.getOriginalFilename())){
            throw new InvalidRequestException("empty_file");
        }
        //uploadImage를 호출하여 S3에 저장된 이미지의 public url을 반환한다.
        return this.uploadImage(image);
    }

    private String uploadImage(MultipartFile image) {
        this.validateImageFileExtension(image.getOriginalFilename());
        try {
            return this.uploadImageToS3(image);
        } catch (IOException e) {
            throw new InvalidRequestException("image upload exception");
        }
    }

    private void validateImageFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new InvalidRequestException("wrong extension");
        }

        String extension = filename.substring(lastDotIndex + 1).toLowerCase();
        List<String> allowedExtentionList = Arrays.asList("jpg", "jpeg", "png");

        if (!allowedExtentionList.contains(extension)) {
            throw new InvalidRequestException("wrong extension");
        }
    }

    private String uploadImageToS3(MultipartFile image) throws IOException {
        String originalFilename = image.getOriginalFilename(); //원본 파일 명
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")); //확장자 명

        String s3FileName = UUID.randomUUID().toString().substring(0, 10) + originalFilename; //변경된 파일 명

        InputStream is = image.getInputStream();
        byte[] bytes = IOUtils.toByteArray(is); //image를 byte[]로 변환

        ObjectMetadata metadata = new ObjectMetadata(); //metadata 생성
        metadata.setContentType("image/" + extension);
        metadata.setContentLength(bytes.length);

        //S3에 요청할 때 사용할 byteInputStream 생성
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);

        try{
            //S3로 putObject 할 때 사용할 요청 객체
            //생성자 : bucket 이름, 파일 명, byteInputStream, metadata
            PutObjectRequest putObjectRequest =
                    new PutObjectRequest(bucketName, s3FileName, byteArrayInputStream, metadata)
                            .withCannedAcl(CannedAccessControlList.PublicRead);

            //실제로 S3에 이미지 데이터를 넣는 부분이다.
            amazonS3Client.putObject(putObjectRequest); // put image to S3
        }catch (Exception e){
            e.printStackTrace();
            throw new InvalidRequestException("s3 upload 오류");
        }finally {
            byteArrayInputStream.close();
            is.close();
        }

        return amazonS3Client.getUrl(bucketName, s3FileName).toString();
    }

    public void deleteImageFromS3(String imageUrl) {
        try {
            // S3 파일 이름 추출 (S3 버킷 URL 이후 경로)
            String fileName = extractFileNameFromUrl(imageUrl);

            // S3에서 이미지 삭제
            amazonS3Client.deleteObject(bucketName, fileName);
        } catch (Exception e) {
            throw new InvalidRequestException("이미지 삭제 오류");
        }
    }

    public String getUserProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidRequestException("not found user"));

        String profileImageUrl = user.getProfileImageUrl();

        if (profileImageUrl == null || profileImageUrl.isEmpty()) {
            throw new InvalidRequestException("profile image not exist");
        }

        return profileImageUrl;
    }

    private String extractFileNameFromUrl(String imageUrl) throws UnsupportedEncodingException {
        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);

        // URL 디코딩을 통해 한글 또는 특수 문자가 포함된 파일 이름 처리
        return URLDecoder.decode(fileName, StandardCharsets.UTF_8);
    }

    @Transactional
    public String uploadUserProfileImage(Long userId, MultipartFile image) {
        String profileImageUrl = upload(image); // 이미지 업로드 후 URL 반환
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidRequestException("not found user"));

        user.updateProfileImage(profileImageUrl); // 유저 프로필 이미지 업데이트
        return profileImageUrl;
    }

    @Transactional
    public void deleteUserProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidRequestException("not found user"));

        String profileImageUrl = user.getProfileImageUrl();

        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            // S3에서 이미지 삭제
            deleteImageFromS3(profileImageUrl);

            // 유저의 프로필 이미지 URL 제거
            user.updateProfileImage(null);
        } else {
            throw new InvalidRequestException("profile image not exist");
        }
    }
}
