package med.voll.api.controller;

import med.voll.api.service.CloudStorageManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobTargetOption;
import com.google.cloud.storage.Storage.PredefinedAcl;
import com.google.cloud.storage.StorageOptions;

import java.io.IOException;
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

    @RequestMapping("/downloadFolder")
    public String downloadFolder(@RequestParam(name = "bucketName") String bucketName,
                                  @RequestParam(name = "objectName") String objectName,
                                  @RequestParam(name = "filePath") String filePath) throws IOException {

        return cloudStorageManagerService.downloadFolder(bucketName, objectName, filePath);
    }
}
