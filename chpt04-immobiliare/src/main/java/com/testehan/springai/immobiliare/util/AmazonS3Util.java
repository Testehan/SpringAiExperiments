package com.testehan.springai.immobiliare.util;

import com.testehan.springai.immobiliare.configuration.BeanConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;


@Component
public class AmazonS3Util {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonS3Util.class);

    private final String S3_BASE_URI;

    private BeanConfig beanConfig;

    public AmazonS3Util(BeanConfig beanConfig) {
        var pattern = "https://%s.s3.%s.amazonaws.com";
        this.beanConfig = beanConfig;
        S3_BASE_URI = beanConfig.getBucketName() == null ? "" : String.format(pattern, beanConfig.getBucketName(), beanConfig.getRegionName());
    }

    public S3Client createAmazomS3() {

        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(beanConfig.getAwsAccessKeyId(), beanConfig.getAwsAccessSecret());

        S3Client client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();

        return client;
    }

    public ListObjectsRequest listAllFilesInAmazonS3(String folderName) {
        ListObjectsRequest listRequest = ListObjectsRequest.builder()
                .bucket(beanConfig.getBucketName()).prefix(folderName).build();

        return listRequest;
    }

    public List<String> listFolder(String folderName) {

        S3Client client = createAmazomS3();

        ListObjectsRequest listRequest = listAllFilesInAmazonS3(folderName);
        ListObjectsResponse response = client.listObjects(listRequest);
        List<S3Object> contents = response.contents();
        ListIterator<S3Object> listIterator = contents.listIterator();

        List<String> listKeys = new ArrayList<>();
        while (listIterator.hasNext()) {
            S3Object object = listIterator.next();
            listKeys.add(object.key());
        }

        return listKeys;

    }

    public void uploadFile(String folderName, String fileName, InputStream inputStream, String contentType) {

        S3Client client = createAmazomS3();
        PutObjectRequest request = putFileInAmazonS3(folderName, fileName, contentType);

        try (inputStream) {
            int contentLength = inputStream.available();
            client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

        } catch (IOException ex) {
            LOGGER.error("Could not upload file to Amazon S3", ex);
        }
    }

    public PutObjectRequest putFileInAmazonS3(String folderName, String fileName, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder().bucket(beanConfig.getBucketName())
                .key(folderName + "/" + fileName).contentType(contentType).acl("public-read").build();

        return request;
    }

    public void deleteFile(String fileName) {
        S3Client client = createAmazomS3();
        DeleteObjectRequest request = deleteFileFromAmazonS3(fileName);
        client.deleteObject(request);
    }

    public DeleteObjectRequest deleteFileFromAmazonS3(String fileName) {
        DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(beanConfig.getBucketName())
                .key(fileName).build();

        return request;
    }

    public void removeFolder(String folderName) {

        S3Client client = createAmazomS3();
        ListObjectsRequest listRequest = listAllFilesInAmazonS3ForRemove(folderName);
        ListObjectsResponse response = client.listObjects(listRequest);

        List<S3Object> contents = response.contents();
        ListIterator<S3Object> listIterator = contents.listIterator();

        while (listIterator.hasNext()) {
            S3Object object = listIterator.next();
            DeleteObjectRequest request = deleteFileFromAmazonS3(object.key());
            client.deleteObject(request);
        }
    }

    public ListObjectsRequest listAllFilesInAmazonS3ForRemove(String folderName) {
        ListObjectsRequest listRequest = ListObjectsRequest.builder()
                .bucket(beanConfig.getBucketName()).prefix(folderName + "/").build();

        return listRequest;
    }

    public String getS3_BASE_URI() {
        return S3_BASE_URI;
    }
}
