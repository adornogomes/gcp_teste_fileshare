package med.voll.api.controller;

import med.voll.api.service.CloudStorageManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;


import java.io.IOException;
import java.nio.file.Files;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/hello")
public class HelloController {

    @Autowired
    private CloudStorageManagerService cloudStorageManagerService;

    @GetMapping
    public String olaMundo() {
        return "Hello World Spring!";
    }

    @RequestMapping("/listar")
    public List<String> listObjects() {

            return cloudStorageManagerService.listObjects();
        }
    @RequestMapping("/listar2")
    public List<String> listObjects2(@RequestParam(name = "bucketName") String bucketName) {

        return cloudStorageManagerService.listObjects2(bucketName);
    }

    @RequestMapping("/upload")
    public String uploadObject(@RequestParam(name = "bucketName") String bucketName,
                                @RequestParam(name = "objectName") String objectName,
                                @RequestParam(name = "filePath") String filePath) throws IOException {

        return cloudStorageManagerService.uploadObject(bucketName, objectName, filePath);
    }

    @RequestMapping("/downloadObject")
    public String downloadObject(@RequestParam(name = "bucketName") String bucketName,
                                @RequestParam(name = "objectName") String objectName,
                                @RequestParam(name = "filePath") String filePath) throws IOException {

        return cloudStorageManagerService.downloadObject(bucketName, objectName, filePath);
    }

    /*@RequestMapping("/downloadFolder")
    public String downloadFolder(@RequestParam(name = "bucketName") String bucketName,
                                  @RequestParam(name = "folderName") String folderName) throws IOException {
                                 // @RequestParam(name = "filePath") String filePath) throws IOException {

        return cloudStorageManagerService.downloadFolder(bucketName, folderName);
    }*/

    @RequestMapping("/downloadFolderAsZip")
    public void downloadFolderAsZip(@RequestParam(name = "bucketName") String bucketName,
                                 @RequestParam(name = "folderName") String folderName,
                                 @RequestParam(name = "filePath") String filePath) throws IOException {

        cloudStorageManagerService.downloadFolderAsZip(bucketName, folderName, filePath);
    }
    @GetMapping(path = "/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<InputStreamResource> getSpecificFile(@RequestParam(name = "bucketName") String bucketName,
                                                               @RequestParam(name = "objectName") String objectName) {

        try {
            String contentType = Files.probeContentType(Paths.get(objectName));

            return ResponseEntity.ok().contentType(MediaType.valueOf(contentType))
                    .header("Content-Disposition", "attachment; filename=" + objectName)
                    .body(new InputStreamResource(cloudStorageManagerService.getSpecificFile(bucketName, objectName)));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping(path = "/downloadFolder", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadFolder(@RequestParam(name = "bucketName") String bucketName,
                                                   @RequestParam(name = "folderName") String folderName) {

        try {
            Path zipFilePath = cloudStorageManagerService.downloadFolder(bucketName, folderName);
            byte[] zipFileBytes = Files.readAllBytes(zipFilePath);
            Files.delete(zipFilePath);

            // Create a ByteArrayResource to wrap the byte array
            ByteArrayResource resource = new ByteArrayResource(zipFileBytes);

            // Set the response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", folderName + ".zip");

            // Return the zip file as a resource in the response entity
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(zipFileBytes.length)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
