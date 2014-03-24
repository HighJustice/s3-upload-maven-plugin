package com.bazaarvoice.maven.plugins.s3.upload;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.Grantee;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

@Mojo(name = "s3-upload")
public class S3UploadMojo extends AbstractMojo
{
  /** Access key for S3. */
  @Parameter(property = "s3-upload.accessKey")
  private String accessKey;

  /** Secret key for S3. */
  @Parameter(property = "s3-upload.secretKey")
  private String secretKey;

  /**
   *  Execute all steps up except the upload to the S3.
   *  This can be set to true to perform a "dryRun" execution.
   */
  @Parameter(property = "s3-upload.doNotUpload", defaultValue = "false")
  private boolean doNotUpload;

  /** The file/folder to upload. */
  @Parameter(property = "s3-upload.source", required = true)
  private String source;

  /** The bucket to upload into. */
  @Parameter(property = "s3-upload.bucketName", required = true)
  private String bucketName;

  /** The file/folder (in the bucket) to create. */
  @Parameter(property = "s3-upload.destination", required = true)
  private String destination;

  /** Force override of endpoint for S3 regions such as EU. */
  @Parameter(property = "s3-upload.endpoint")
  private String endpoint;

  /** In the case of a directory upload, recursively upload the contents. */
  @Parameter(property = "s3-upload.recursive", defaultValue = "false")
  private boolean recursive;

  /** The file/folder to upload. */
  @Parameter(property = "s3-upload.metaDataMap", required = false)
  private Map<String, String> metaDataMap;

  /** The file/folder to upload. */
  @Parameter(property = "s3-upload.useAttachmentContentDisposition", defaultValue = "false")
  private boolean useAttachmentContentDisposition;

  @Parameter(property = "s3-upload.useReducedRedundancy", defaultValue = "false")
  private boolean useReducedRedundancy;

  @Parameter(property = "s3-upload.readGrantees", required=false)
  private List<String> readGrantees;

  @Parameter(property = "s3-upload.propagateBucketAcl", defaultValue = "false")
  private boolean propagateBucketAcl;

  @Override
  public void execute() throws MojoExecutionException
  {
    File sourceFile = new File(source);
    if (!sourceFile.exists()) {
      throw new MojoExecutionException("File/folder doesn't exist: " + source);
    }

    AmazonS3 s3 = getS3Client(accessKey, secretKey);
    if (endpoint != null) {
      s3.setEndpoint(endpoint);
    }

    if (!s3.doesBucketExist(bucketName)) {
      throw new MojoExecutionException("Bucket doesn't exist: " + bucketName);
    }

    if (doNotUpload) {
      getLog().info(String.format("File %s would have be uploaded to s3://%s/%s (dry run)",
        sourceFile, bucketName, destination));
      return;
    }

    boolean success = upload(s3, sourceFile);
    if (!success) {
      throw new MojoExecutionException("Unable to upload file to S3.");
    }

    getLog().info(String.format("File %s uploaded to s3://%s/%s",
      sourceFile, bucketName, destination));
  }

  private static AmazonS3 getS3Client(String accessKey, String secretKey)
  {
    AWSCredentialsProvider provider;
    if (accessKey != null && secretKey != null) {
      AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
      provider = new StaticCredentialsProvider(credentials);
    } else {
      provider = new DefaultAWSCredentialsProviderChain();
    }
    return new AmazonS3Client(provider);
  }

  private boolean upload(AmazonS3 s3, File sourceFile) throws MojoExecutionException
  {
    TransferManager mgr = new TransferManager(s3);
    Transfer transfer;
    ObjectMetadata metaData = new ObjectMetadata();
    int lastIndex = destination.lastIndexOf("/");
	String fileName = destination.substring(lastIndex+1);
    if (sourceFile.isFile()) {
      PutObjectRequest por = new PutObjectRequest(bucketName, destination, sourceFile);
      if (useReducedRedundancy)
    	  	getLog().info("applying "+StorageClass.ReducedRedundancy+" to: "+fileName);
      		por.setStorageClass(StorageClass.ReducedRedundancy);
      if (metaDataMap!=null)
      	metaData.setUserMetadata(metaDataMap);
      if (useAttachmentContentDisposition){
    	  String disposition = String.format(" attachment; filename=\"%s\"",fileName);
    	  metaData.setContentDisposition(disposition);
    	  getLog().info("ContentDisposition of: "+fileName+" is set to: "+disposition);
      }
      por.setMetadata(metaData);
      transfer = mgr.upload(por);
    } else if (sourceFile.isDirectory()) {
      transfer = mgr.uploadDirectory(bucketName, destination, sourceFile, recursive);
    } else {
      throw new MojoExecutionException("File is neither a regular file nor a directory " + sourceFile);
    }
    try {
      getLog().debug("Transferring " + transfer.getProgress().getTotalBytesToTransfer() + " bytes...");
      AmazonClientException ace = transfer.waitForException();
      getLog().info("Transferred " + transfer.getProgress().getBytesTransfered() + " bytes.");

      if (ace !=null){
    	  getLog().error("Exception risen during upload: "+ace);
    	  throw ace;
      }

      if (propagateBucketAcl){
    	  getLog().info("propagating ACL from bucket: "+bucketName+" to "+fileName);
    	  s3.setObjectAcl(bucketName, destination, s3.getBucketAcl(bucketName));
      }

      if (readGrantees!= null){
    	  getLog().info("applying additional security permissions..");
    	  AccessControlList acl = s3.getObjectAcl(bucketName, destination);
    	  for (String readGrantee: readGrantees){
    		  getLog().info("\t read permission for grantee ID: "+readGrantee);
    		  Grantee grantee = new CanonicalGrantee(readGrantee);
    		  acl.grantPermission(grantee, Permission.Read);
    	  }
    	  s3.setObjectAcl(bucketName, destination, acl);
      }

    } catch (InterruptedException e) {
      return false;
    }
    return true;
  }
}
