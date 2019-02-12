# Oracle Functions + OCI Object Storage

This example shows how Oracle Functions can interact with [Oracle Cloud Infrastructure Object Storage](https://docs.cloud.oracle.com/iaas/Content/Object/Concepts/objectstorageoverview.htm) to execute operations such as putting a new object in a storage bucket, listing the objects of a storage bucket and getting the contents of a specific object. 

Individual functions cater to the put, get and list capabilities i.e. there are three functions as a part of a single application which you will deploy to Oracle Functions. These are Java functions which use the Object Storage APIs in the [OCI Java SDK](https://docs.cloud.oracle.com/iaas/Content/API/SDKDocs/javasdk.htm) for programatic interaction.

This example also demonstrates the usage of a custom `Dockerfile` to build your functions i.e. how you can tailor the creation of your function using the Function Java FDK Docker image as the base image and add in the parts you want to customize e.g. in this specific case, the OCI Java SDK JAR file is extracted from the GitHub releases (since its currently not available via Maven) and the inclusion of the OCI private key inside the function Docker container

## Pre-requisites

- Object Storage - You need to create a storage bucket - please refer to the [details in the documentation](https://docs.cloud.oracle.com/iaas/Content/Object/Tasks/managingbuckets.htm?TocPath=Services%7CObject%20Storage%7C_____2). You also need to ensure that the user account which you configure in the application (details to follow) have the required Object Store privileges to execute get, list and put operations. If not, you might see `404` (authorization related) errors while invoking the function e.g. `Error fetching object (404, BucketNotFound, false) Either the bucket named 'test' does not exist in the namespace 'test-namespace' or you are not authorized to access it`. Please refer to the [IAM Policies section in the documentation](https://docs.cloud.oracle.com/iaas/Content/Identity/Concepts/commonpolicies.htm) for further details
- Ensure you are using the latest version of the Fn CLI. To update simply run the following command - `curl -LSs https://raw.githubusercontent.com/fnproject/cli/master/install | sh`
- Oracle Functions setup - Configure the Oracle Functions service along with your development environment and switch to the correct Fn context using `fn use context <context-name>` 

Last but not the least, clone (`git clone https://github.com/abhirockzz/oracle-functions-oci-object-store-basic`) or download this repository before proceeding further

## Create an application

Create an application with required configuration - all your functions will be a part of this application

`fn create app --annotation oracle.com/oci/subnetIds='["<OCI_SUBNET_OCIDs>"]' --config TENANCY=<TENANCY_OCID> --config USER=<USER_OCID --config FINGERPRINT=<PUBLIC_KEY_FINGERPRINT> --config PASSPHRASE=<PASSPHRASE> --config REGION=<OCI_REGION> --config NAMESPACE=<NAMESPACE> fn-object-store-app`

Summary of the configuration parameters

- `OCI_SUBNET_OCIDs` - the OCID(s) of the subnet where you want your functions to be deployed
- `TENANCY` - OCID of your tenancy
- `USER` - OCID of the user which will be used to execute the Object Storage put, get, list operations (should have required privileges)
- `FINGERPRINT` - public key fingerprint of the user
- `PASSPHRASE` - passphrase of the private key
- `REGION` - region of your Object Storage service
- `NAMESPACE` - Object Storage namespace (see [this](https://docs.cloud.oracle.com/iaas/Content/Object/Tasks/understandingnamespaces.htm))

For e.g.

`fn create app --annotation oracle.com/oci/subnetIds='["ocid1.subnet.oc1.phx.aaaaaaaabrg5uf2uzc3ni4jkz4vhqwprofmlmo2mpumnuddd7iandssruohq"]' --config TENANCY=ocid1.tenancy.oc1..aaaaaaaaydrjm42otncda2xn7qtv7l3hqnd3zxn2u2siwdhniibwfv4wwhta --config USER=ocid1.user.oc1..aaaaaaaa4seqx6jeyma42ldy4cbuv35q4l42scz5p4rkz3rauuoioo26qwmq --config FINGERPRINT=42:82:5f:44:ca:a1:2e:58:d2:42:6a:af:52:d5:3d:04 --config PASSPHRASE=4242 --config REGION=us-phoenix-1 --config NAMESPACE=foobar fn-object-store-app`

## Deploy the functions

Note: Before deploying the function(s), please copy your OCI private key file to each of the function folders.

This example uses version [`1.3.5`](https://github.com/oracle/oci-java-sdk/releases/tag/v1.3.5) of the OCI Java SDK (latest at the time of writing). If you want wish to use a different version (see [releases](https://github.com/oracle/oci-java-sdk/releases)), you should make sure

- to add `--build-arg OCI_JAVA_SDK_VERSION=<required_version>` to the `fn deploy` command
- and also, update the version in `pom.xml`(s) of all the functions

		<dependency>
			<groupId>com.oracle.oci.sdk</groupId>
			<artifactId>oci-java-sdk</artifactId>
			<version>required_version</version>
		</dependency>

Change into the top level directory - `cd oracle-functions-oci-object-store-basic`

`fn -v deploy --build-arg PRIVATE_KEY_NAME=<PRIVATE_KEY_NAME> --app fn-object-store-app --all` 

`PRIVATE_KEY_NAME` is the name of the private key (`.pem`) file which you copied to all the functions

e.g. `fn -v deploy --build-arg PRIVATE_KEY_NAME=oci_private_key.pem --app fn-object-store-app --all`

To use a different OCI Java SDK version (e.g. `1.3.4`) - `fn -v deploy --build-arg PRIVATE_KEY_NAME=oci_private_key.pem --build-arg OCI_JAVA_SDK_VERSION=1.3.4 --app fn-object-store-app`

> The above command(s) deploys **all** the functions (notice `--all` the the end). If you want to deploy one function at a time, enter the respective directory and use the same command as above **without the `--all` directive**

### Sanity check

Run `fn inspect app fn-object-store-app` to check your app (and its config) and `fn list functions fn-object-store-app` to check associated functions

## Testing

You can now test drive the capabilities which the functions provide

Note: This example works with `String` data type for the `content` attribute

### Put

To store a file with text content, use the below command

`echo -n '{"name": "<filename>", "bucketName":"<bucket-name>", "content": "<text content>"}' | fn invoke fn-object-store-app put`

e.g.

`echo -n '{"name": "file1.txt", "bucketName":"test", "content": "This file was created in OCI object storage bucket using Oracle Functions"}' | fn invoke fn-object-store-app put`

### Get

To fetch the contents of a file in a bucket (the one you stored in the previous step)

`echo -n '{"name": "<filename>", "bucketName":"<bucket-name>"}' | fn invoke fn-object-store-app get`

e.g. to get the content of the file you stored in the previous step

`echo -n '{"name": "file1.txt", "bucketName":"test"}' | fn invoke fn-object-store-app get`

You should get the contents of the file in the response

	This file was created in OCI object storage bucket using Oracle Functions

### List
	
Finally, to list the files (file names) in a bucket

`echo -n '<bucket-name>' | fn invoke fn-object-store-app list`

e.g. to list the file names in bucket `test-bucket` 

`echo -n 'test' | fn invoke fn-object-store-app list`

you should see a JSON response with list of all file/objects (names)

	[
	    "file1.txt",
	    "lorem.txt",
	    "README.md"
	]
