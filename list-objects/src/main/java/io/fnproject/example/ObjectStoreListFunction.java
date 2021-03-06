package io.fnproject.example;

import com.google.common.base.Supplier;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ObjectStoreListFunction {

    private ObjectStorage objStoreClient = null;

    public ObjectStoreListFunction() {
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

    public List<String> handle(String bucketName) {

        if (objStoreClient == null) {
            System.err.println("There was a problem creating the ObjectStorageClient object. Please check logs");
            return Collections.emptyList();
        }

        List<String> objNames = null;
        try {
            String nameSpace = System.getenv().get("NAMESPACE");

            ListObjectsRequest lor = ListObjectsRequest.builder()
                    .namespaceName(nameSpace)
                    .bucketName(bucketName)
                    .build();

            System.err.println("Listing objects from bucket " + bucketName);

            ListObjectsResponse response = objStoreClient.listObjects(lor);

            objNames = response.getListObjects().getObjects().stream()
                    .map((objSummary) -> objSummary.getName())
                    .collect(Collectors.toList());

            System.err.println("Got list of objects in bucket " + bucketName);

        } catch (Throwable e) {
            System.err.println("Error fetching object list from bucket " + e.getMessage());
        }

        return objNames;
    }
}
