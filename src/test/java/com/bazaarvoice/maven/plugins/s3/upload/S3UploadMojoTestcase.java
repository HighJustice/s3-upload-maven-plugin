package com.bazaarvoice.maven.plugins.s3.upload;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class S3UploadMojoTestcase extends AbstractMojoTestCase {
	private static final String UPLOAD_FILE_NAME = "upload-test.txt";

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
};
