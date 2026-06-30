package uk.gov.ons.census.fwmt.outcomeservice.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCache;
import uk.gov.ons.census.fwmt.outcomeservice.repository.GatewayCacheRepository;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GatewayCacheServiceTest {

  @InjectMocks
  private GatewayCacheService gatewayCacheService;

  @Mock
  private GatewayCacheRepository repository;

  @DisplayName("save flushes eagerly so same-transaction processors see the row")
  @Test
  void shouldSaveAndFlush() {
    GatewayCache cache = GatewayCache.builder().caseId("case-1").delivered(true).build();

    gatewayCacheService.save(cache);

    verify(repository).saveAndFlush(cache);
  }
}
