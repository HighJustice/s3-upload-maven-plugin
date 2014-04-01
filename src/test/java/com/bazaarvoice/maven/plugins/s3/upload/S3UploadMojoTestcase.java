package com.bazaarvoice.maven.plugins.s3.upload;

import java.io.File;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class S3UploadMojoTestcase extends AbstractMojoTestCase {
	private static final String UPLOAD_FILE_NAME = "upload-test.txt";
    private static final String UPLOAD_FILE_NAME2 = "upload-test2.txt";
    private static final String UPLOAD_FILE_NAME3 = "upload-test3.txt";
    private static final String UPLOAD_FILE_NAME4 = "upload-test4.txt";

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

    public void test_pubACL() throws Exception {
        try {
            S3UploadMojo s3m = new S3UploadMojo();
            setVariableValueToObject(s3m, "source", new File(getBasedir(), "target/test-classes/" + UPLOAD_FILE_NAME2).getAbsolutePath());
            setVariableValueToObject(s3m, "bucketName", "bloomfire-artifacts");
            setVariableValueToObject(s3m, "destination", "screencast-installer/2.0.0/" + UPLOAD_FILE_NAME2);
            setVariableValueToObject(s3m, "useAttachmentContentDisposition", true);
            setVariableValueToObject(s3m, "pubAcl", true);
            setVariableValueToObject(s3m, "useReducedRedundancy", true);
            s3m.execute();
        } catch (Throwable ex) {
            assertNull("PubACL. Exception raised: ", ex);
        }
    }
};
