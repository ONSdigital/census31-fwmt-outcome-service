package uk.gov.ons.census.fwmt.outcomeservice.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCaseRecord;

@Repository
public interface GatewayCaseRecordRepository extends JpaRepository<GatewayCaseRecord, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  GatewayCaseRecord findByCaseId(String caseId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  GatewayCaseRecord findByOriginalCaseId(String caseId);

}
