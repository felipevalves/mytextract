package br.com.felipe.mytextract.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;

//@Configuration
public class AwsConfig {
    @Value("${aws.region.norte-virginia}")
    private String regionNorteVirginia;
    /**
     * DEFAULT - MILI-DESENVOLVIMENTO
     *
     */
    @Bean("S3ClientDefaultNorteVirginia")
    public TextractClient getAuthenticationNorteVirginia() {
        return TextractClient.builder().region(Region.of(regionNorteVirginia)).credentialsProvider(ProfileCredentialsProvider.create("default")).build();
    }


}
