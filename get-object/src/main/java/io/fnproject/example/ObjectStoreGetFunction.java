package io.fnproject.example;

import com.google.common.base.Supplier;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class ObjectStoreGetFunction {

    private ObjectStorage objStoreClient = null;

    public ObjectStoreGetFunction() {

        try {
            String privateKey = System.getenv().get("OCI_PRIVATE_KEY_FILE_NAME");
            Supplier<InputStream> privateKeySupplier = () -> {
                InputStream is = null;
                String ociPrivateKeyPath = "/function/" + privateKey;
                System.err.println("Private key location - " + ociPrivateKeyPath);

                try {
                    is = new FileInputStream(ociPrivateKeyPath);
                } catch (FileNotFoundException ex) {
                    System.err.println("Problem accessing OCI private key at " + ociPrivateKeyPath + " - " + ex.getMessage());
                }

                return is;

            };

            AuthenticationDetailsProvider provider
                    = SimpleAuthenticationDetailsProvider.builder()
                            .tenantId(System.getenv().get("TENANCY"))
                            .userId(System.getenv().get("USER"))
                            .fingerprint(System.getenv().get("FINGERPRINT"))
                            .passPhrase(System.getenv().get("PASSPHRASE"))
                            .privateKeySupplier(privateKeySupplier)
                            .build();

            objStoreClient = new ObjectStorageClient(provider);
            objStoreClient.setRegion(System.getenv().get("REGION"));

        } catch (Throwable ex) {
            System.err.println("Error occurred in ObjectStoreListFunction constructor - " + ex.getMessage());

        }
    }

    public static class GetObjectInfo {

        private String bucketName;
        private String name;

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public String handle(GetObjectInfo objectInfo) {

        String result = "FAILED";

        if (objStoreClient == null) {
            System.err.println("There was a problem creating the ObjectStorageClient object. Please check logs");
            return result;
        }
        try {

            String nameSpace = System.getenv().get("NAMESPACE");

            GetObjectRequest gor = GetObjectRequest.builder()
                    .namespaceName(nameSpace)
                    .bucketName(objectInfo.getBucketName())
                    .objectName(objectInfo.getName())
                    .build();
            System.err.println("Getting content for object " + objectInfo.getName() + " from bucket " + objectInfo.getBucketName());

            GetObjectResponse response = objStoreClient.getObject(gor);
            result = new BufferedReader(new InputStreamReader(response.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));

            System.err.println("Finished reading content for object " + objectInfo.getName());

        } catch (Throwable e) {
            System.err.println("Error fetching object " + e.getMessage());
            result = "Error fetching object " + e.getMessage();
        }

        return result;
    }
}
