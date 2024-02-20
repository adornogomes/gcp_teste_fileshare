package med.voll.api.service;

import com.google.cloud.ReadChannel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;


import java.io.*;
import java.nio.channels.Channels;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class CloudStorageManagerService {

    @Value("${temp.dir}")
    private String tempDirectoryPath;

    // The ID of your GCP project
    String projectId = "winged-line-414815";

    // The ID of your GCS bucket
    String bucketName = "bucket_de_pdfs";

    public List<String> listObjects() {
        List<String> result = new ArrayList<String>();

        Storage storage = StorageOptions.getDefaultInstance().getService();

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

    public String downloadObject(String bucketName, String objectName, String destFilePath) throws IOException {

        Storage storage = StorageOptions.newBuilder().setProjectId(this.projectId).build().getService();

        System.out.println("Downloaded object "
                + objectName
                + " from bucket name "
                + bucketName
                + " to "
                + destFilePath);

        Blob blob = storage.get(BlobId.of(bucketName, objectName));
        blob.downloadTo(Paths.get(destFilePath + "/"+ objectName));

        return "Downloaded object "
                        + objectName
                        + " from bucket name "
                        + bucketName
                        + " to "
                        + destFilePath;
    }

    public Path downloadFolder(String bucketName, String folderName) throws IOException {
        Storage storage = StorageOptions.newBuilder().setProjectId(this.projectId).build().getService();

        // List blobs in the specified folder
        storage.list(bucketName, Storage.BlobListOption.prefix(folderName + "/"))
                .iterateAll()
                .forEach(blob -> {
                    try {
                        // Download each blob in the folder
                        downloadBlob(storage, blob, tempDirectoryPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });



        File zipDirPath = new File(tempDirectoryPath, folderName);
        zipDirectory(String.valueOf(zipDirPath), tempDirectoryPath + "/" + folderName + ".zip");
        deleteDirectory(zipDirPath);


        return Paths.get(tempDirectoryPath + "/" + folderName + ".zip");
    }

    private void downloadBlob(Storage storage, Blob blob, String destinationDirectory) throws IOException {
        //String path = destinationDirectory;

        if(blob.getName().endsWith("/")) {

                // Create a temporary directory to store downloaded files
                File tempDir = new File(destinationDirectory, blob.getName());
                tempDir.mkdirs();
                //path = tempDir.getAbsolutePath();
                //System.out.println("path: " + path);
            } else {

            // Extract the filename from the blob
            String[] parts = blob.getName().split("/");
            String fileName = parts[parts.length - 1];
            System.out.println("blob: " + blob.getName());
            System.out.println("filename: " + fileName);

            // Create the destination file
            //FileOutputStream outputStream = new FileOutputStream(destinationDirectory + "/" + fileName);
            FileOutputStream outputStream = new FileOutputStream(destinationDirectory + "/" + blob.getName());

            // Download the blob to the file
            storage.get(blob.getBlobId()).downloadTo(outputStream);

            outputStream.close();

        }
    }


    public InputStream getSpecificFile(String bucketName, String objectName) {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        Blob blob = storage.get(bucketName, objectName);

        try {
        ReadChannel readChannel = blob.reader();

            InputStream inputStream = Channels.newInputStream(readChannel);
            return inputStream;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public void downloadFolderAsZip(String bucketName, String folderName, String destinationDirectory) throws IOException {
        Storage storage = StorageOptions.getDefaultInstance().getService();

        // Create a temporary directory to store downloaded files
        File tempDir = new File(destinationDirectory, "temp");
        tempDir.mkdirs();

        // List blobs in the specified folder and its subfolders
        storage.list(bucketName, Storage.BlobListOption.prefix(folderName + "/"))
                .iterateAll()
                .forEach(blob -> {
                    try {
                        // Download each blob in the folder to the temporary directory
                        downloadBlobFolder(storage, blob, tempDir.getAbsolutePath(), folderName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        // Create a zip file containing the contents of the temporary directory
        zipDirectory(tempDir.getAbsolutePath(), destinationDirectory + "/" + folderName + ".zip");

        // Delete the temporary directory
        //deleteDirectory(tempDir);

        System.out.println("Folder " + folderName + " downloaded from bucket " + bucketName + " and saved as zip file.");
    }

    private void downloadBlobFolder(Storage storage, Blob blob, String destinationDirectory, String folderName) throws IOException {
        // Extract the relative path from the blob
        String relativePath = blob.getName().substring(folderName.length() + 1);

        // Create the destination file
        File destinationFile = new File(destinationDirectory, relativePath);

        if (blob.isDirectory()) {
            // Create the directory if it does not exist
            destinationFile.mkdirs();
        } else {
            // Create parent directories if they do not exist
            destinationFile.getParentFile().mkdirs();

            // Download the blob to the file
            try (OutputStream outputStream = new FileOutputStream(destinationFile)) {
                storage.get(blob.getBlobId()).downloadTo(outputStream);
            }
        }
    }

    private void zipDirectory(String sourceDirectoryPath, String zipFilePath) throws IOException {
        FileOutputStream fos = new FileOutputStream(zipFilePath);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(sourceDirectoryPath);

        zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        fos.close();
    }

    private void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
