package uk.gov.ons.census.fwmt.outcomeservice.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.gov.census.ffa.storage.utils.StorageUtils;

@SuppressFBWarnings(value = {"DM_EXIT", "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"},
    justification = "App shouldnt start up; try-with-resources on storage stream")
@Configuration
public class GpgConfig {

  @Autowired
  private StorageUtils storageUtils;

  @Value("${outcomeservice.pgp.fwmtPublicKey}")
  private String fwmtPgpPublicKey;

  @Value("${outcomeservice.pgp.midlPublicKey}")
  private String midlPgpPublicKey;

  private static boolean isClasspathLocation(String location) {
    return location != null && location.regionMatches(true, 0, "classpath:", 0, 10);
  }

  private static byte[] readClasspathResource(String classpathLocation) throws IOException {
    String path = classpathLocation.substring("classpath:".length());
    try (InputStream in = new ClassPathResource(path).getInputStream()) {
      return in.readAllBytes();
    }
  }

  @Bean
  public byte[] fwmtPgpPublicKeyByteArray() throws IOException {
    try {
      if (isClasspathLocation(fwmtPgpPublicKey)) {
        return readClasspathResource(fwmtPgpPublicKey);
      }
      URI fwmtPgpPublicKeyUri = URI.create(fwmtPgpPublicKey);
      try (InputStream fileInputStream = storageUtils.getFileInputStream(fwmtPgpPublicKeyUri)) {
        return fileInputStream.readAllBytes();
      }
    } catch (IOException e) {
      System.exit(128);
      throw e;
    }

  }

  @Bean
  public byte[] midlPgpPublicKeyByteArray() throws IOException {
    try {
      if (isClasspathLocation(midlPgpPublicKey)) {
        return readClasspathResource(midlPgpPublicKey);
      }
      URI midlPgpPublicKeyyUri = URI.create(midlPgpPublicKey);
      try (InputStream fileInputStream = storageUtils.getFileInputStream(midlPgpPublicKeyyUri)) {
        return fileInputStream.readAllBytes();
      }
    } catch (IOException e) {
      System.exit(128);
      throw e;
    }

  }

  
}
