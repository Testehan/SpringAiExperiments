package com.testehan.springai.immobiliare.constants;

public class AmazonS3Constants {
    public static final String S3_BASE_URI;
    public static final String BUCKET_NAME;
    public static final String ACCESS_KEY_ID;
    public static final String SECRET_ACCESS_KEY;

    static {
        BUCKET_NAME = System.getenv("AWS_IMOBIL_BUCKET_NAME");
        var region = System.getenv("AWS_IMOBIL_REGION");
        var pattern = "https://%s.s3.%s.amazonaws.com";

        S3_BASE_URI = BUCKET_NAME == null ? "" : String.format(pattern, BUCKET_NAME, region);

        ACCESS_KEY_ID = System.getenv("AWS_IMOBIL_ACCESS_KEY_ID");
        SECRET_ACCESS_KEY = System.getenv("AWS_IMOBIL_SECRET_ACCESS_KEY");

    }

}
