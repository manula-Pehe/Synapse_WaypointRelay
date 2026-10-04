package com.synapse.waypoint.core.file.controller;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.core.file.dto.FileContent;
import com.synapse.waypoint.core.file.service.FileService;

/** Serves stored photos and signatures. Open to every signed-in role; uploads go through the driver and issue endpoints. */
@RestController
@RequestMapping("/api/files")
class FileController {

    private static final Duration CACHE_FOR = Duration.ofDays(1);

    private final FileService files;

    FileController(FileService files) {
        this.files = files;
    }

    @GetMapping("/{id}")
    ResponseEntity<byte[]> get(@PathVariable String id) {
        FileContent file = files.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.maxAge(CACHE_FOR).cachePrivate())
                .body(file.bytes());
    }
}
