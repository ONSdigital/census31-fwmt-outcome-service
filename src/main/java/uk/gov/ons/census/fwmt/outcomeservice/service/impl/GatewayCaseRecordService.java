package uk.gov.ons.census.fwmt.outcomeservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCaseRecord;
import uk.gov.ons.census.fwmt.outcomeservice.repository.GatewayCaseRecordRepository;

/**
 * This class is bare-bones because it's a simple connector between the rest of the code and the caching implementation
 * Please don't subvert this class by touching the GatewayCaseRecordRepository
 * If we ever change from a database to redis, this class will form the breaking point
 */

@Slf4j
@Service
public class GatewayCaseRecordService {

  @Autowired
  private GatewayCaseRecordRepository repository;

  public GatewayCaseRecord getById(String caseId) {
    return repository.findByCaseId(caseId);
  }

  public GatewayCaseRecord getByOriginalId(String caseId) {
    return repository.findByOriginalCaseId(caseId);
  }

  public void save(GatewayCaseRecord cache) {
    // Flush eagerly. Several processors (e.g. FULFILMENT_REQUESTED then LINKED_QID)
    // can handle the same caseId within a single @Transactional outcome. Without a
    // flush, Hibernate defers each INSERT until commit, so every processor that
    // called getById and saw no row queues its own INSERT; they then collide on
    // gateway_case_record_pkey and roll back the whole outcome. Flushing here makes the
    // first save visible to the next processor's getById, which then performs an
    // UPDATE instead of a second INSERT.
    repository.saveAndFlush(cache);
  }

}
