package com.tags_generation_service.tags_generation_service.Controllers;

import com.tags_generation_service.tags_generation_service.Model.FileMetadataPostgres;
import com.tags_generation_service.tags_generation_service.Service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class FileControllers {
 //   @PostMapping("/share/{fileId}")
//    public Boolean shareFiles();
}