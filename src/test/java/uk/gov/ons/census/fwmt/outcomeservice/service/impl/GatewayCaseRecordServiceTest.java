package uk.gov.ons.census.fwmt.outcomeservice.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCaseRecord;
import uk.gov.ons.census.fwmt.outcomeservice.repository.GatewayCaseRecordRepository;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GatewayCaseRecordServiceTest {

  @InjectMocks
  private GatewayCaseRecordService gatewayCacheService;

  @Mock
  private GatewayCaseRecordRepository repository;

  @DisplayName("save flushes eagerly so same-transaction processors see the row")
  @Test
  void shouldSaveAndFlush() {
    GatewayCaseRecord cache = GatewayCaseRecord.builder().caseId("case-1").delivered(true).build();

    gatewayCacheService.save(cache);

    verify(repository).saveAndFlush(cache);
  }
}
