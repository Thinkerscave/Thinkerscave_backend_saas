package com.thinkerscave.shared.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.upload")
public class FileUploadProperties {

    /** Absolute or relative upload root directory. */
    private String baseDir = "uploads";

    /** Max file size in bytes (default 5 MB). */
    private long maxFileSizeBytes = 5 * 1024 * 1024L;

    private List<String> allowedContentTypes = new ArrayList<>(List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "text/csv"
    ));
}
