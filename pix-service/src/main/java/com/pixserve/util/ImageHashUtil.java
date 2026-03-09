package com.pixserve.util;

import com.pixserve.controller.ImageController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;

public class ImageHashUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(ImageHashUtil.class);
    /**
     * Compute SHA-256 file hash (exact duplicate detection).
     */
    public static String getFileHash(File file) throws Exception {
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(fileBytes);
        return Base64.getEncoder().encodeToString(digest);
    }

    /**
     * Compute SHA-256 file hash (exact duplicate detection).
     */
    public static String getFileHash(Path filePath)  {
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(fileBytes);
            String sha256Hash = Base64.getEncoder().encodeToString(digest);
            LOGGER.info("SHA-256 calculated : {}", sha256Hash);
            return sha256Hash;
        } catch (Exception exception) {
            LOGGER.error("exception while finding hash" ,exception);
            return "";
        }
    }

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

        File file1 = new File("D:\\pix-serve-storage\\images\\2021\\05\\ps_IMG_16221921620005018.jpg");
        String fileHash1 = getFileHash(file1);

        File file2 = new File("D:\\pix-serve-storage\\images\\2021\\05\\ps_IMG_16221921620008679.jpg");
        String fileHash2 = getFileHash(file1);

        //N38Hsn7FsAoGhOw4S9G3IpwqoDqxt2bsHFss50zOCAI=

        System.out.println("fileHash1 : " + fileHash1);
        System.out.println("fileHash2 : " + fileHash2);

    }
}
