package com.pixserve.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Base64;

public class ImageHashUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(ImageHashUtil.class);

    /**
     * Compute SHA-256 file hash (exact duplicate detection).
     */
    public static String getFileHash(File file) throws Exception {
        return getFileHash(file.toPath());
    }

    /**
     * Compute SHA-256 file hash (exact duplicate detection).
     */
    public static String getFileHash(Path filePath) {

        try {

            MessageDigest md = MessageDigest.getInstance("SHA-256");

            try (InputStream inputStream = Files.newInputStream(filePath);
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, md)) {

                byte[] buffer = new byte[8192];
                while (digestInputStream.read(buffer) != -1) {
                    // consume stream
                }
            }

            byte[] digest = md.digest();
            String sha256Hash = Base64.getEncoder().encodeToString(digest);
            LOGGER.info("SHA-256 calculated : {}",sha256Hash);

            return sha256Hash;

        } catch (Exception exception) {
            LOGGER.error("exception while finding hash",exception);
            return "";
        }
    }
//    public static String getFileHash(Path filePath)  {
//        try {
//            byte[] fileBytes = Files.readAllBytes(filePath);
//            MessageDigest md = MessageDigest.getInstance("SHA-256");
//            byte[] digest = md.digest(fileBytes);
//            String sha256Hash = Base64.getEncoder().encodeToString(digest);
//            LOGGER.info("SHA-256 calculated : {}", sha256Hash);
//            return sha256Hash;
//        } catch (Exception exception) {
//            LOGGER.error("exception while finding hash" ,exception);
//            return "";
//        }
//    }

    /**
     * Compute perceptual hash (average hash method).
     * Detects near-duplicates (resized/compressed versions).
     */
    public static String getPerceptualHash(File file) throws Exception {
        BufferedImage img = ImageIO.read(file);
        if (img == null) throw new IllegalArgumentException("Not a valid image: " + file);

        // Resize to 8x8 grayscale
        Image scaled = img.getScaledInstance(8, 8, Image.SCALE_SMOOTH);
        BufferedImage grayImg = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayImg.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();

        // Compute brightness average
        int[] pixels = new int[64];
        int sum = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int rgb = grayImg.getRGB(x, y) & 0xFF; // gray value
                pixels[y * 8 + x] = rgb;
                sum += rgb;
            }
        }
        int avg = sum / 64;

        // Build binary hash
        StringBuilder hash = new StringBuilder();
        for (int p : pixels) {
            hash.append(p >= avg ? "1" : "0");
        }

        return hash.toString();
    }

    /**
     * Compare two perceptual hashes with Hamming distance.
     */
    public static int hammingDistance(String hash1, String hash2) {
        if (hash1.length() != hash2.length()) {
            throw new IllegalArgumentException("Hashes must be of equal length");
        }
        int dist = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) dist++;
        }
        return dist;
    }

    /**
     * Check if two images are exact duplicates.
     */
    public static boolean areExactDuplicates(File file1, File file2) throws Exception {
        return getFileHash(file1).equals(getFileHash(file2));
    }

    /**
     * Check if two images are near duplicates (perceptual hash + threshold).
     */
    public static boolean areNearDuplicates(File file1, File file2, int threshold) throws Exception {
        String h1 = getPerceptualHash(file1);
        String h2 = getPerceptualHash(file2);
        return hammingDistance(h1, h2) <= threshold;
    }


    public static void main(String[] args) throws Exception {


        File file1 = new File("/Users/arjunbhalekar/pix-serve-dev/storage/videos/2026/01/ps_IMG_0414.MOV");
        String fileHash1 = getFileHash(file1);

        File file2 = new File("/Users/arjunbhalekar/pix-media-storage/videos/1970/01/ps_IMG_1580.MOV.mp4");
        String fileHash2 = getFileHash(file2.toPath());

        //N38Hsn7FsAoGhOw4S9G3IpwqoDqxt2bsHFss50zOCAI=

        System.out.println("fileHash1 : " + fileHash1);
        System.out.println("fileHash2 : " + fileHash2);

    }
}
