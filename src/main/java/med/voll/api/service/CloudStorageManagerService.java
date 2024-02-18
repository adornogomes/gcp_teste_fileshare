package med.voll.api.service;

import org.springframework.stereotype.Service;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CloudStorageManagerService {

    // The ID of your GCP project
    String projectId = "coherent-voice-414615";

    // The ID of your GCS bucket
    String bucketName = "bucket_teste_gcp_api_adorno";

    public List<String> listObjects() {
        List<String> result = new ArrayList<String>();

        Storage storage = StorageOptions.newBuilder().setProjectId(this.projectId).build().getService();

        Page<Blob> blobs = storage.list(this.bucketName);

        for (Blob blob : blobs.iterateAll()) {
            result.add(blob.getName());
        }

        return result;
    }

    public List<String> listObjects2(String bucketName) {
        List<String> result = new ArrayList<String>();

        Storage storage = StorageOptions.newBuilder().setProjectId(this.projectId).build().getService();

        Page<Blob> blobs = storage.list(bucketName);

        for (Blob blob : blobs.iterateAll()) {
            result.add(blob.getName());
        }

        return result;
    }

    public String uploadObject(String bucketName, String objectName, String filePath) throws IOException {
        Storage storage = StorageOptions.newBuilder().setProjectId(this.projectId).build().getService();
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        // Optional: set a generation-match precondition to avoid potential race
        // conditions and data corruptions. The request returns a 412 error if the
        // preconditions are not met.
        Storage.BlobWriteOption precondition;
        if (storage.get(bucketName, objectName) == null) {
            // For a target object that does not yet exist, set the DoesNotExist precondition.
            // This will cause the request to fail if the object is created before the request runs.
            precondition = Storage.BlobWriteOption.doesNotExist();
        } else {
            // If the destination already exists in your bucket, instead set a generation-match
            // precondition. This will cause the request to fail if the existing object's generation
            // changes before the request runs.
            precondition =
                    Storage.BlobWriteOption.generationMatch(
                            storage.get(bucketName, objectName).getGeneration());
        }
        storage.createFrom(blobInfo, Paths.get(filePath), precondition);

        return blobInfo.getMediaLink();
    }

    public String downloadObject(String bucketName, String objectName, String destFilePath) {

        Storage storage = StorageOptions.newBuilder().setProjectId(this.projectId).build().getService();

        Blob blob = storage.get(BlobId.of(bucketName, objectName));
        blob.downloadTo(Paths.get(destFilePath));

        return "Downloaded object "
                        + objectName
                        + " from bucket name "
                        + bucketName
                        + " to "
                        + destFilePath;
    }

    public String downloadFolder(String bucketName, String folderName, String destinationDirectory) throws IOException {
        Storage storage = StorageOptions.newBuilder().setProjectId(this.projectId).build().getService();

        // List blobs in the specified folder
        storage.list(bucketName, Storage.BlobListOption.prefix(folderName + "/"))
                .iterateAll()
                .forEach(blob -> {
                    try {
                        // Download each blob in the folder
                        downloadBlob(storage, blob, destinationDirectory);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        return "Folder " + folderName + " downloaded from bucket " + bucketName;
    }

    private void downloadBlob(Storage storage, Blob blob, String destinationDirectory) throws IOException {
        if(!blob.getName().endsWith("/")) {

            // Extract the filename from the blob
            String[] parts = blob.getName().split("/");
            String fileName = parts[parts.length - 1];
            System.out.println("blob: " + blob.getName());
            System.out.println("filename: " + fileName);

            // Create the destination file
            FileOutputStream outputStream = new FileOutputStream(destinationDirectory + "/" + fileName);

            // Download the blob to the file
            storage.get(blob.getBlobId()).downloadTo(outputStream);

            outputStream.close();

        }
    }



}
