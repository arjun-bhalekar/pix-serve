package com.pixserve.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.pixserve.model.CameraInfo;
import com.pixserve.model.ImageMetadata;
import com.pixserve.model.LocationInfo;
import com.pixserve.model.TakenInfo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

public class MetadataExtractorUtil {

    public static void extractMetadata(ImageMetadata imageMetadata) {


        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new File(imageMetadata.getImagePath()));

            // Extract DateTimeOriginal
            ExifSubIFDDirectory exifDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifDir != null) {
                String dateTimeStr = exifDir.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);  // ex: "2019:10:27 23:57:27"
                if (dateTimeStr != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
                    LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);

                    TakenInfo takenInfo = new TakenInfo();
                    takenInfo.setDateTime(String.valueOf(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()));
                    takenInfo.setYear(localDateTime.getYear());
                    takenInfo.setMonth(localDateTime.getMonthValue());
                    takenInfo.setDay(localDateTime.getDayOfMonth());

                    imageMetadata.setTakenInfo(takenInfo);
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
                imageMetadata.setLocationInfo(locationInfo);
            }

            // Extract Camera Info
            Directory dir = metadata.getFirstDirectoryOfType(com.drew.metadata.exif.ExifIFD0Directory.class);
            if (dir != null) {
                String make = dir.getString(com.drew.metadata.exif.ExifIFD0Directory.TAG_MAKE);
                String model = dir.getString(com.drew.metadata.exif.ExifIFD0Directory.TAG_MODEL);
                CameraInfo cameraInfo = new CameraInfo();
                cameraInfo.setMake(make);
                cameraInfo.setModel(model);
                imageMetadata.setCameraInfo(cameraInfo);
            }


            //set current timeTaken if unable to fetch from image
            if(Objects.isNull(imageMetadata.getTakenInfo())){
                LocalDateTime localDateTime =LocalDateTime.now();

                TakenInfo takenInfo = new TakenInfo();
                takenInfo.setDateTime(String.valueOf(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()));
                takenInfo.setYear(localDateTime.getYear());
                takenInfo.setMonth(localDateTime.getMonthValue());
                takenInfo.setDay(localDateTime.getDayOfMonth());

                imageMetadata.setTakenInfo(takenInfo);

            }

        } catch (Exception e) {
            System.err.println("Error extracting metadata: " + e.getMessage());
        }
    }
}
