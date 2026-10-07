package uk.gov.ons.census.fwmt.outcomeservice.messaging.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.spring.pubsub.core.PubSubConfiguration;
import com.google.cloud.spring.pubsub.core.PubSubOperations;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.DefaultPublisherFactory;
import com.google.cloud.spring.pubsub.support.DefaultSubscriberFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class RmPubSubConfig {

  @Bean("rmPubSubTemplate")
  public PubSubOperations rmPubSubTemplate(
      @Value("${app.messaging.pubsub.rm-project-id:}") String rmProjectId,
      @Value("${spring.cloud.gcp.project-id}") String fwmtProjectId,
      CredentialsProvider credentialsProvider,
      @Qualifier("publisherTransportChannelProvider") TransportChannelProvider publisherChannel,
      @Qualifier("subscriberTransportChannelProvider") TransportChannelProvider subscriberChannel,
      Environment environment) {
    String projectId = resolveProjectId(rmProjectId, fwmtProjectId, environment);
    DefaultPublisherFactory publisherFactory = new DefaultPublisherFactory(() -> projectId);
    publisherFactory.setCredentialsProvider(credentialsProvider);
    publisherFactory.setChannelProvider(publisherChannel);
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
    pubSubConfiguration.initialize(projectId);
    DefaultSubscriberFactory subscriberFactory = new DefaultSubscriberFactory(
        () -> projectId, pubSubConfiguration);
    subscriberFactory.setCredentialsProvider(credentialsProvider);
    subscriberFactory.setChannelProvider(subscriberChannel);
    return new PubSubTemplate(publisherFactory, subscriberFactory);
  }

  private static String resolveProjectId(
      String rmProjectId, String fwmtProjectId, Environment environment) {
    if (StringUtils.hasText(rmProjectId)) {
      return rmProjectId;
    }
    if (environment.containsProperty("KUBERNETES_SERVICE_HOST")) {
      throw new IllegalStateException("RM_PUBSUB_PROJECT must be configured for Kubernetes deployments");
    }
    return fwmtProjectId;
  }
}