package uk.gov.ons.census.fwmt.outcomeservice.messaging.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.spring.pubsub.core.PubSubConfiguration;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.DefaultPublisherFactory;
import com.google.cloud.spring.pubsub.support.DefaultSubscriberFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

class RmPubSubConfigTest {

    private final TransportChannelProvider publisherChannel = mock(TransportChannelProvider.class);
    private final TransportChannelProvider subscriberChannel = mock(TransportChannelProvider.class);

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(RmPubSubConfig.class)
            .withBean(CredentialsProvider.class, () -> mock(CredentialsProvider.class))
            .withBean("publisherTransportChannelProvider", TransportChannelProvider.class, () -> publisherChannel)
            .withBean("subscriberTransportChannelProvider", TransportChannelProvider.class, () -> subscriberChannel);

  @Test
  void createsTemplateForConfiguredRmProject() {
    contextRunner.withPropertyValues("app.messaging.pubsub.rm-project-id=c31-rm-app-int")
        .run(context -> {
          PubSubTemplate template = (PubSubTemplate) context.getBean("rmPubSubTemplate");
          DefaultPublisherFactory publisherFactory =
              (DefaultPublisherFactory) template.getPublisherFactory();
          DefaultSubscriberFactory subscriberFactory =
              (DefaultSubscriberFactory) template.getSubscriberFactory();

          assertThat(ReflectionTestUtils.getField(publisherFactory, "projectId"))
              .isEqualTo("c31-rm-app-int");
          assertThat(ReflectionTestUtils.getField(subscriberFactory, "projectId"))
              .isEqualTo("c31-rm-app-int");
          assertThat(ReflectionTestUtils.getField(publisherFactory, "channelProvider"))
              .isSameAs(publisherChannel);
          assertThat(ReflectionTestUtils.getField(subscriberFactory, "channelProvider"))
              .isSameAs(subscriberChannel);
          PubSubConfiguration subscriberConfiguration =
              (PubSubConfiguration) ReflectionTestUtils.getField(subscriberFactory, "pubSubConfiguration");
          assertThat(subscriberConfiguration.computePullEndpoint(
              "event_action-instruction_fwmtg", "c31-rm-app-int")).isNull();
        });
  }
}