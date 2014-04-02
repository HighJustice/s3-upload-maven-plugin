package com.bazaarvoice.maven.plugins.s3.upload;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class S3UploadMojoTestcase extends AbstractMojoTestCase {
	private static final String UPLOAD_FILE_NAME = "upload-test.txt";
    private static final String UPLOAD_FILE_NAME_FOR_PUBLIC = "upload-test-for-public.txt";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void test_ACL_RR_ACD_MojoGoal() throws Exception{
		try{
		S3UploadMojo s3m = new S3UploadMojo();
		//Log log = s3m.getLog();
		setVariableValueToObject(s3m, "source", new File( getBasedir(), "target/test-classes/"+ UPLOAD_FILE_NAME).getAbsolutePath());
		setVariableValueToObject(s3m, "bucketName", "bloomfire-artifacts" );
		setVariableValueToObject(s3m, "destination", "screencast-installer/2.0.0/" +UPLOAD_FILE_NAME);
		setVariableValueToObject(s3m, "useAttachmentContentDisposition", true);
		setVariableValueToObject(s3m, "propagateBucketAcl", true);
		setVariableValueToObject(s3m, "useReducedRedundancy", true);
		s3m.execute();
		//Not doing any further verification except manual checking..
		} catch (Throwable exception){
			assertNull("Exception raised: ", exception);
		}
	}

    public void test_publicAccess() throws Exception {
        try {
            S3UploadMojo s3m = new S3UploadMojo();
            setVariableValueToObject(s3m, "source", new File(getBasedir(), "target/test-classes/" + UPLOAD_FILE_NAME_FOR_PUBLIC).getAbsolutePath());
            setVariableValueToObject(s3m, "bucketName", "bloomfire-artifacts");
            setVariableValueToObject(s3m, "destination", "screencast-installer/2.0.0/" + UPLOAD_FILE_NAME_FOR_PUBLIC);
            setVariableValueToObject(s3m, "useAttachmentContentDisposition", true);
            setVariableValueToObject(s3m, "publicAccess", true);
            setVariableValueToObject(s3m, "useReducedRedundancy", true);
            s3m.execute();

            AWSCredentialsProvider provider = new DefaultAWSCredentialsProviderChain();
            AmazonS3 s3 = new AmazonS3Client(provider);
            AccessControlList acl = s3.getObjectAcl("bloomfire-artifacts", "screencast-installer/2.0.0/" + UPLOAD_FILE_NAME_FOR_PUBLIC);
            Set<Grant> grants = acl.getGrants();
            List<String> slist = new ArrayList<String>();
            for (Grant g : grants) {
                Permission perm =  g.getPermission();
                slist.add(perm.name());
            }
            boolean b = slist.containsAll(new ArrayList<String>(){
                {
                    add("Read");
                    add("FullControl");
                }
            });
            assertTrue(b);
        } catch (Throwable ex) {
            assertNull("Public access. Exception raised: ", ex);
        }
    }

    public void test_deletePublicFile() throws Exception {
        try {
            AWSCredentialsProvider provider = new DefaultAWSCredentialsProviderChain();
            AmazonS3 s3 = new AmazonS3Client(provider);
            S3Object s3o = s3.getObject("bloomfire-artifacts", "screencast-installer/2.0.0/" + UPLOAD_FILE_NAME_FOR_PUBLIC);
            s3.deleteObject("bloomfire-artifacts", "screencast-installer/2.0.0/" + UPLOAD_FILE_NAME_FOR_PUBLIC);
        } catch (Throwable ex) {
            ex.printStackTrace();
            assertNull(ex);
        }
    }
};
