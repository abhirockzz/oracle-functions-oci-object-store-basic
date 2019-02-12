package io.fnproject.example;

import com.google.common.base.Supplier;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ObjectStorePutFunction {

    private ObjectStorage objStoreClient = null;

    public ObjectStorePutFunction() {
        System.err.println("Inside ObjectStorePutFunction ctor");
        try {
            String privateKey = System.getenv().getOrDefault("OCI_PRIVATE_KEY_FILE_NAME", "oci_api_key.pem");
            System.err.println("Private key " + privateKey);
            Supplier<InputStream> privateKeySupplier = () -> {
                InputStream is = null;
                String ociPrivateKeyPath = "/function/" + privateKey;
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

            System.err.println("AuthenticationDetailsProvider setup");

            objStoreClient = new ObjectStorageClient(provider);
            objStoreClient.setRegion(System.getenv().get("REGION"));

            System.err.println("ObjectStorage client setup");
        } catch (Exception ex) {
            System.err.println("Error occurred in ObjectStorePutFunction ctor " + ex.getMessage());
        }
    }

    public static class ObjectInfo {

        private String name;
        private String bucketName;
        private String content;

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public ObjectInfo() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

    }


    public String handle(ObjectInfo objectInfo) {
        System.err.println("Inside ObjectStorePutFunction/handle");
        String result = "FAILED";

        if (objStoreClient == null) {
            return result;
        }
        try {

            String nameSpace = System.getenv().getOrDefault("NAMESPACE", "test-namespace");

            PutObjectRequest por = PutObjectRequest.builder()
                    .namespaceName(nameSpace)
                    .bucketName(objectInfo.bucketName)
                    .objectName(objectInfo.name)
                    .putObjectBody(new ByteArrayInputStream(objectInfo.content.getBytes(StandardCharsets.UTF_8)))
                    .build();

            PutObjectResponse poResp = objStoreClient.putObject(por);
            result = "OPC ID for upload operation for object " + objectInfo.name + " - " + poResp.getOpcRequestId();

        } catch (Exception e) {
            System.err.println("Error invoking object store API " + e.getMessage());
            result = "Error invoking object store API " + e.getMessage();
        }

        return result;
    }
}
