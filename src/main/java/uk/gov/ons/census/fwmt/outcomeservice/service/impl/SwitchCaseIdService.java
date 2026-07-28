package uk.gov.ons.census.fwmt.outcomeservice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCaseRecord;

import org.springframework.transaction.annotation.Transactional;

@Component
public class SwitchCaseIdService {

    @Autowired
    private GatewayCaseRecordService gatewayCacheService;

    @Transactional
    public String fromNcToOriginal(String caseID) {
        GatewayCaseRecord gatewayCache = gatewayCacheService.getById(caseID);
        return gatewayCache.getOriginalCaseId();
    }

    @Transactional
    public String fromIdOriginalToNc(String caseId) {
        GatewayCaseRecord gatewayCache = gatewayCacheService.getByOriginalId(caseId);
        return gatewayCache.getCaseId();
    }
}
