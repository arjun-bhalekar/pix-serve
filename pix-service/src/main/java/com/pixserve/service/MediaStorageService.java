package com.pixserve.service;

import com.pixserve.model.MediaType;
import com.pixserve.model.TakenInfo;
import net.coobird.thumbnailator.Thumbnails;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;

@Service
public class MediaStorageService {

    private final static Logger LOGGER = LoggerFactory.getLogger(MediaStorageService.class);

    @Value("${base.dir.path}")
    private String baseDirPath;


    public List<String> saveOriginalAndGenThumbnail(MediaType mediaType, Path srcPath, TakenInfo takenInfo, String fileOriginalName) throws IOException {
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
        String mediaDirName="";
        if(mediaType.equals(MediaType.IMAGE)){
            mediaDirName = "images";
        }else if(mediaType.equals(MediaType.VIDEO)){
            mediaDirName = "videos";
        }

        String folder = year + "/" + String.format("%02d", month);
        String filename = "ps_"+fileOriginalName;

        // Step 2: Construct full paths
        Path mediaDir = Path.of(baseDirPath, mediaDirName, folder);
        Path thumbDir = Path.of(baseDirPath, "thumbs", folder);

        Files.createDirectories(mediaDir);
        Files.createDirectories(thumbDir);

        // Step 3: Save original image/video
        Path targetMediaPath = mediaDir.resolve(filename);
        Files.copy(srcPath, targetMediaPath, StandardCopyOption.REPLACE_EXISTING);

        // Step 4: Generate thumbnail
        String relativeThumbPath = "";
        if(mediaType.equals(MediaType.IMAGE)){
            Path targetThumbPath = thumbDir.resolve(filename);
            Thumbnails.of(targetMediaPath.toFile()).size(200, 200).toFile(targetThumbPath.toFile());
            relativeThumbPath = "thumbs/" + folder + "/" + filename;
            LOGGER.info("thumbnail generated for image");
        }else if(mediaType.equals(MediaType.VIDEO)){

            generateThumbnailForVideo(targetMediaPath, thumbDir, filename);
            relativeThumbPath = "thumbs/" + folder + "/" + filename + ".jpg";
        }

        // Step 5: Return relative paths
        String relativeMediaPath = mediaDirName +"/" + folder + "/" + filename;


        return List.of(relativeMediaPath, relativeThumbPath, filename);
    }

    private static void generateThumbnailForVideo(Path targetMediaPath, Path thumbDir, String filename) {
        // FFmpegFrameGrabber handles HEVC, H.264, and complex iPhone metadata tracks natively

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(targetMediaPath.toFile())) {

            // Available in 1.5.11+ (Perfect for your 1.5.13 upgrade)
            //grabber.setVideoON

            grabber.setOption("analyzeduration", "10000000");
            grabber.setOption("probesize", "50000000");
            grabber.setOption("autorotate", "1");

            grabber.start();

            // Skip frame calculations on heavy Dolby Vision streams
            grabber.setVideoFrameNumber(1);

            Frame frame = grabber.grabImage();
            if (frame != null) {
                Java2DFrameConverter converter = new Java2DFrameConverter();
                BufferedImage bufferedImage = converter.getBufferedImage(frame);

                if (bufferedImage != null) {
                    Path targetThumbPath = thumbDir.resolve(filename + ".jpg");

                    // OPTIONAL: Resize the video frame to exactly 200x200 using Thumbnailator
                    // to perfectly match your application's image thumbnail sizes!
                    Thumbnails.of(bufferedImage)
                            .size(200, 200)
                            .toFile(targetThumbPath.toFile());

                    LOGGER.info("successfully generated 200x200 thumbnail for video file");
                }
            }
            grabber.stop();
        } catch (Exception e) {
            LOGGER.error("Failed to generate thumbnail for video file :", e);
        }


    }

}
