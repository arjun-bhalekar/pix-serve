package com.pixserve.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.pixserve.model.*;
import com.pixserve.service.MediaMetadataService;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetadataExtractorUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataExtractorUtil.class);

    public static void extractMetadata(MediaMetadata mediaMetadata) {

        if (mediaMetadata.getMediaType().equals(MediaType.IMAGE)) {
            extractImageMetadata(mediaMetadata);
        } else if (mediaMetadata.getMediaType().equals(MediaType.VIDEO)) {
            extractVideoMetadata(mediaMetadata);
        }
    }


    private static void extractImageMetadata(MediaMetadata mediaMetadata) {


        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new File(mediaMetadata.getMediaPath()));


            // Extract DateTimeOriginal
            ExifSubIFDDirectory exifDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifDir != null) {
                String dateTimeStr = exifDir.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);  // ex: "2019:10:27 23:57:27"
                if (dateTimeStr != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
                    LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
                    mediaMetadata.setTakenInfo(toTakenInfo(localDateTime));
                }
            }

            // Extract GPS
            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null && gpsDir.getGeoLocation() != null) {
                double lat = gpsDir.getGeoLocation().getLatitude();
                double lon = gpsDir.getGeoLocation().getLongitude();

                LocationInfo locationInfo = new LocationInfo();
                locationInfo.setLatitude(String.valueOf(lat));
                locationInfo.setLongitude(String.valueOf(lon));
                mediaMetadata.setLocationInfo(locationInfo);
            }

            // Extract Camera Info
            Directory dir = metadata.getFirstDirectoryOfType(com.drew.metadata.exif.ExifIFD0Directory.class);
            if (dir != null) {
                String make = dir.getString(com.drew.metadata.exif.ExifIFD0Directory.TAG_MAKE);
                String model = dir.getString(com.drew.metadata.exif.ExifIFD0Directory.TAG_MODEL);
                CameraInfo cameraInfo = new CameraInfo();
                cameraInfo.setMake(make);
                cameraInfo.setModel(model);
                mediaMetadata.setCameraInfo(cameraInfo);
            }


            //set current timeTaken if unable to fetch from image
            if (Objects.isNull(mediaMetadata.getTakenInfo())) {
                TakenInfo takenInfo = getTakenInfoDefaultSystem();
                mediaMetadata.setTakenInfo(takenInfo);

            }

        } catch (Exception e) {
            LOGGER.error("Exception occurred while extracting image metadata.", e);
        }
    }


    private static void extractVideoMetadata(MediaMetadata mediaMetadata) {

        try (FFmpegFrameGrabber grabber =
                     new FFmpegFrameGrabber(mediaMetadata.getMediaPath())) {

            grabber.start();
            Map<String, String> metadata = grabber.getMetadata();
            /*
             * Resolution
             */
            mediaMetadata.setWidth(grabber.getImageWidth());
            mediaMetadata.setHeight(grabber.getImageHeight());

            /*
             * Duration
             * FFmpeg returns microseconds
             */
            mediaMetadata.setDurationSeconds(
                    Math.round(
                            grabber.getLengthInTime() / 1_000_000.0
                    )
            );

            /*
             * Taken Date
             */
            String creationDate =
                    metadata.get("com.apple.quicktime.creationdate");

            if (creationDate == null) {
                creationDate = metadata.get("creation_time");
            }

            if (creationDate != null) {
                if (creationDate.startsWith("1970-")) {
                    LOGGER.warn("The creation date is from 1970 format. Skipping.");
                    creationDate = null;
                }
                if(creationDate !=null) {
                    TakenInfo takenInfo = getTakenInfoFromVideoCreationDate(creationDate);
                    mediaMetadata.setTakenInfo(takenInfo);
                }
            }

            /*
             * Camera Info
             */
            String make = metadata.get("com.apple.quicktime.make");
            String model = metadata.get("com.apple.quicktime.model");

            if (make != null || model != null) {
                CameraInfo cameraInfo = new CameraInfo();
                cameraInfo.setMake(make);
                cameraInfo.setModel(model);
                mediaMetadata.setCameraInfo(cameraInfo);
            }

            /*
             * GPS Location
             */
            String iso6709 = metadata.get("com.apple.quicktime.location.ISO6709");

            if (iso6709 != null) {
                double[] coordinates =
                        parseIso6709(iso6709);
                if (coordinates != null) {
                    LocationInfo locationInfo = new LocationInfo();
                    locationInfo.setLatitude(
                            String.valueOf(coordinates[0])
                    );
                    locationInfo.setLongitude(
                            String.valueOf(coordinates[1])
                    );

                    mediaMetadata.setLocationInfo(locationInfo);
                }
            }

            /*
             * Fallback Taken Date
             */
            if (Objects.isNull(mediaMetadata.getTakenInfo())) {

                TakenInfo takenInfo = getTakenInfoDefaultSystem();
                mediaMetadata.setTakenInfo(takenInfo);
            }

            grabber.stop();

        } catch (Exception e) {
            LOGGER.error("Exception occurred while extracting video metadata.", e);
        }
    }

    @NonNull
    private static TakenInfo getTakenInfoDefaultSystem() {
        LocalDateTime now = LocalDateTime.now();
        return toTakenInfo(now);
    }

    @NonNull
    private static TakenInfo getTakenInfoFromVideoCreationDate(String creationDate) {

        LocalDateTime localDateTime;

        if (creationDate.contains("+") || creationDate.contains("-") || creationDate.endsWith("Z")) {

            // This custom formatter accepts formats with OR without colons in the offset
            try {
                DateTimeFormatter flexibleFormatter = new DateTimeFormatterBuilder()
                        .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true) // Safely parses optional decimals (.000 to .000000000) if present
                        .appendOptional(DateTimeFormatter.ofPattern("XXX"))  // Handles offsets like +05:30 or Z
                        .appendOptional(DateTimeFormatter.ofPattern("Z"))    // Handles offsets like +0530
                        .toFormatter();

                OffsetDateTime offsetDateTime = OffsetDateTime.parse(creationDate, flexibleFormatter);
                localDateTime = offsetDateTime.toLocalDateTime();
            } catch (Exception exception) {
                LOGGER.error("Exception occurred while extracting taken info from video creation. falling to current time", exception);
                localDateTime = LocalDateTime.now();
            }
        } else {

            Instant instant = Instant.parse(creationDate);

            localDateTime =
                    LocalDateTime.ofInstant(
                            instant,
                            ZoneId.systemDefault()
                    );
        }

        TakenInfo takenInfo = new TakenInfo();

        takenInfo.setDateTime(
                String.valueOf(
                        localDateTime
                                .toInstant(ZoneOffset.UTC)
                                .toEpochMilli()
                )
        );

        takenInfo.setYear(localDateTime.getYear());
        takenInfo.setMonth(localDateTime.getMonthValue());
        takenInfo.setDay(localDateTime.getDayOfMonth());
        return takenInfo;
    }

    private static double[] parseIso6709(String value) {

        Pattern pattern = Pattern.compile(
                "([+-]\\d+\\.\\d+)([+-]\\d+\\.\\d+).*"
        );

        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            return new double[]{
                    Double.parseDouble(matcher.group(1)),
                    Double.parseDouble(matcher.group(2))
            };
        }
        return null;
    }

    private static TakenInfo toTakenInfo(LocalDateTime localDateTime) {
        TakenInfo takenInfo = new TakenInfo();
        takenInfo.setDateTime(String.valueOf(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()));
        takenInfo.setYear(localDateTime.getYear());
        takenInfo.setMonth(localDateTime.getMonthValue());
        takenInfo.setDay(localDateTime.getDayOfMonth());
        return takenInfo;
    }


    public static void main(String[] args) {

        MediaMetadata mediaMetadata = new MediaMetadata();
        mediaMetadata.setMediaPath("/Users/arjunbhalekar/pix-media-storage/videos/1970/01/ps_IMG_1580.MOV.mp4");
        mediaMetadata.setMediaType(MediaType.VIDEO);
        MetadataExtractorUtil.extractMetadata(mediaMetadata);

        System.out.println(mediaMetadata.getTakenInfo().toString());
    }
}
