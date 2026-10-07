package uk.gov.ons.census.fwmt.outcomeservice.messaging.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.DefaultPublisherFactory;
import com.google.cloud.spring.pubsub.support.DefaultSubscriberFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

class RmPubSubConfigTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(RmPubSubConfig.class)
      .withBean(CredentialsProvider.class, () -> mock(CredentialsProvider.class));

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
        });
  }
}