package com.pixserve.service;

import com.pixserve.model.TakenInfo;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class ImageStorageService {

    @Value("${base.dir.path}")
    private String baseDirPath;


    public List<String> saveOriginalAndGenThumbnail(Path srcPath, TakenInfo takenInfo, String fileOriginalName) throws IOException {
        // Step 1: Determine year and month
        int year, month;
        if (takenInfo != null) {
            year = takenInfo.getYear();
            month = takenInfo.getMonth();
        } else {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }

        String folder = year + "/" + String.format("%02d", month);
        String filename = "ps_"+fileOriginalName;

        // Step 2: Construct full paths
        Path imageDir = Path.of(baseDirPath, "images", folder);
        Path thumbDir = Path.of(baseDirPath, "thumbs", folder);

        Files.createDirectories(imageDir);
        Files.createDirectories(thumbDir);

        // Step 3: Save original image
        Path targetImgPath = imageDir.resolve(filename);
        Files.copy(srcPath, targetImgPath, StandardCopyOption.REPLACE_EXISTING);

        // Step 4: Generate thumbnail
        Path targetThumbPath = thumbDir.resolve(filename);
        Thumbnails.of(targetImgPath.toFile()).size(200, 200).toFile(targetThumbPath.toFile());

        // Step 5: Return relative paths
        String relativeImagePath = "images/" + folder + "/" + filename;
        String relativeThumbPath = "thumbs/" + folder + "/" + filename;

        return List.of(relativeImagePath, relativeThumbPath, filename);
    }


//    public List<String> saveOriginalAndGenThumbnail(MultipartFile file) throws IOException {
//        Path imageDir = Path.of(baseDirPath, "images");
//        Files.createDirectories(imageDir);
//        Path targetImgPath = imageDir.resolve(file.getOriginalFilename());
//        file.transferTo(targetImgPath.toFile());
//        Path targetThumbnailPath = generateThumbnail(targetImgPath.toFile());
//        return Arrays.asList(targetImgPath.toString(), targetThumbnailPath.toString());
//    }
//
//    private Path generateThumbnail(File srcImageFile) throws IOException {
//        Path thumbDir = Path.of(baseDirPath, "thumbs");
//        Files.createDirectories(thumbDir);
//        Path thumbPath = thumbDir.resolve(srcImageFile.getName());
//        //Thumbnails.of(file.getInputStream()).size(200, 200).toFile(thumbPath.toFile());
//        Thumbnails.of(srcImageFile).size(200, 200).toFile(thumbPath.toFile());
//        return thumbPath;
//    }
}
