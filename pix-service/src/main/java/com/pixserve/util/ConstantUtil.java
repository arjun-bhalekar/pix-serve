package com.pixserve.util;

import java.util.Set;

public class ConstantUtil {

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg",
            ".JPG",
            ".jpeg",
            ".png",
            ".gif",
            ".mov",
            ".MOV",
            ".mp4",
            ".MP4"
    );


    public static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg",
            ".JPG",
            ".jpeg",
            ".png",
            ".gif"
    );

    public static final Set<String> VIDEO_EXTENSIONS = Set.of(
            ".mov",
            ".MOV",
            ".mp4",
            ".MP4"
    );


}
