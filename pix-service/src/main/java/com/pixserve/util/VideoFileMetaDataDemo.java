package com.pixserve.util;

import org.bytedeco.javacv.FFmpegFrameGrabber;

public class VideoFileMetaDataDemo {


    public static void debugVideoMetadata(String filePath) {

        try (FFmpegFrameGrabber grabber =
                     new FFmpegFrameGrabber(filePath)) {

            grabber.start();

            System.out.println("Format: " + grabber.getFormat());
            System.out.println("Length (us): " + grabber.getLengthInTime());
            System.out.println("Width: " + grabber.getImageWidth());
            System.out.println("Height: " + grabber.getImageHeight());

            System.out.println("Video Metadata:");

            grabber.getVideoMetadata().forEach(
                    (k, v) -> System.out.println(k + " = " + v)
            );

            System.out.println("Audio Metadata:");

            grabber.getAudioMetadata().forEach(
                    (k, v) -> System.out.println(k + " = " + v)
            );

            System.out.println("Container Metadata:");

            grabber.getMetadata().forEach(
                    (k, v) -> System.out.println(k + " = " + v)
            );

            grabber.stop();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {


        debugVideoMetadata("/Users/arjunbhalekar/pix-serve-dev/storage/videos/2026/01/ps_IMG_0414.MOV");

    }
}
