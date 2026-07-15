package uk.gov.ons.census.fwmt.outcomeservice.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.data.ccs.CCSInterviewOutcome;
import uk.gov.ons.census.fwmt.common.data.ccs.CCSPropertyListingOutcome;
import uk.gov.ons.census.fwmt.common.data.ce.CENewStandaloneAddress;
import uk.gov.ons.census.fwmt.common.data.ce.CENewUnitAddress;
import uk.gov.ons.census.fwmt.common.data.ce.CEOutcome;
import uk.gov.ons.census.fwmt.common.data.household.HHNewSplitAddress;
import uk.gov.ons.census.fwmt.common.data.household.HHNewStandaloneAddress;
import uk.gov.ons.census.fwmt.common.data.household.HHOutcome;
import uk.gov.ons.census.fwmt.common.data.nc.NCOutcome;
import uk.gov.ons.census.fwmt.common.data.spg.SPGNewStandaloneAddress;
import uk.gov.ons.census.fwmt.common.data.spg.SPGNewUnitAddress;
import uk.gov.ons.census.fwmt.common.data.spg.SPGOutcome;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.config.OutcomeFeatureFlagConfig;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetMapper;
import uk.gov.ons.census.fwmt.outcomeservice.service.OutcomeService;

import java.util.UUID;

@Slf4j
@Component
public class OutcomePreprocessingReceiver {

  public static final String PREPROCESSING_HH_OUTCOME = "PREPROCESSING_HH_OUTCOME";

  public static final String PREPROCESSING_HH_SPLITADDRESS_OUTCOME = "PREPROCESSING_HH_SPLITADDRESS_OUTCOME";
  
  public static final String PREPROCESSING_HH_STANDALONE_OUTCOME = "PREPROCESSING_HH_STANDALONE_OUTCOME";
  
  public static final String PREPROCESSING_CE_OUTCOME = "PREPROCESSING_CE_OUTCOME";
  
  public static final String PREPROCESSING_CE_UNITADDRESS_OUTCOME = "PREPROCESSING_CE_UNITADDRESS_OUTCOME";
  
  public static final String PREPROCESSING_CE_STANDALONE_OUTCOME = "PREPROCESSING_CE_STANDALONE_OUTCOME";
  
  public static final String PREPROCESSING_SPG_OUTCOME = "PREPROCESSING_SPG_OUTCOME";
  
  public static final String PREPROCESSING_SPG_UNITADDRESS_OUTCOME = "PREPROCESSING_SPG_UNITADDRESS_OUTCOME";
  
  public static final String PREPROCESSING_SPG_STANDALONE_OUTCOME = "PREPROCESSING_SPG_STANDALONE_OUTCOME";
  
  public static final String PREPROCESSING_CCS_PL_OUTCOME = "PREPROCESSING_CCS_PL_OUTCOME";
  
  public static final String PREPROCESSING_CCS_INT_OUTCOME = "PREPROCESSING_CCS_INT_OUTCOME";

  public static final String PREPROCESSING_NC_OUTCOME = "PREPROCESSING_NC_OUTCOME";
  public static final String PREPROCESSING_OUTCOME_IGNORED_BY_FEATURE_FLAG = "PREPROCESSING_OUTCOME_IGNORED_BY_FEATURE_FLAG";

  @Autowired
  private OutcomeService delegate;

  @Autowired
  private OutcomeSuperSetMapper outcomeSuperSetMapper;

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Autowired
  private OutcomeFeatureFlagConfig outcomeFeatureFlagConfig;

  public void processMessage(SPGOutcome spgOutcome) throws GatewayException {
    OutcomeSuperSetDto outcomeDTO = outcomeSuperSetMapper.toOutcomeSuperSetDto(spgOutcome);
    if (!isSurveyEnabled(String.valueOf(outcomeDTO.getCaseId()), "SPG", outcomeDTO.getOutcomeCode())) {
      return;
    }
    gatewayEventManager.triggerEvent(String.valueOf(outcomeDTO.getCaseId()), PREPROCESSING_SPG_OUTCOME,
        "Survey type", "SPG",
        "Outcome code", outcomeDTO.getOutcomeCode(),
        "Secondary Outcome", outcomeDTO.getSecondaryOutcomeDescription());
    delegate.createSpgOutcomeEvent(outcomeDTO);
  }

  public void processMessage(SPGNewUnitAddress newUnitAddress) throws GatewayException {
    OutcomeSuperSetDto outcomeDTO = outcomeSuperSetMapper.toOutcomeSuperSetDto(newUnitAddress);
    outcomeDTO.setCaseId(UUID.randomUUID());
    if (!isSurveyEnabled(String.valueOf(outcomeDTO.getCaseId()), "SPG", outcomeDTO.getOutcomeCode())) {
      return;
    }
    gatewayEventManager.triggerEvent(String.valueOf(outcomeDTO.getCaseId()), PREPROCESSING_SPG_UNITADDRESS_OUTCOME,
        "Survey type", "SPG",
        "Outcome code", outcomeDTO.getOutcomeCode(),
        "Secondary Outcome", outcomeDTO.getSecondaryOutcomeDescription());
    delegate.createSpgOutcomeEvent(outcomeDTO);
  }

  public void processMessage(SPGNewStandaloneAddress standaloneAddress) throws GatewayException {
    OutcomeSuperSetDto outcomeDTO = outcomeSuperSetMapper.toOutcomeSuperSetDto(standaloneAddress);
    outcomeDTO.setCaseId(UUID.randomUUID());
    if (!isSurveyEnabled(String.valueOf(outcomeDTO.getCaseId()), "SPG", outcomeDTO.getOutcomeCode())) {
      return;
    }
    gatewayEventManager.triggerEvent(String.valueOf(outcomeDTO.getCaseId()), PREPROCESSING_SPG_STANDALONE_OUTCOME,
        "Survey type", "SPG",
        "Outcome code", outcomeDTO.getOutcomeCode(),
        "Secondary Outcome", outcomeDTO.getSecondaryOutcomeDescription());
    delegate.createSpgOutcomeEvent(outcomeDTO);
  }

  public void processMessage(CEOutcome CeOutcome) throws GatewayException {
    OutcomeSuperSetDto outcomeDTO = outcomeSuperSetMapper.toOutcomeSuperSetDto(CeOutcome);
    if (!isSurveyEnabled(String.valueOf(outcomeDTO.getCaseId()), "CE", outcomeDTO.getOutcomeCode())) {
      return;
    }
    gatewayEventManager.triggerEvent(String.valueOf(outcomeDTO.getCaseId()), PREPROCESSING_CE_OUTCOME,
        "Survey type", "CE",
        "Outcome code", outcomeDTO.getOutcomeCode(),
        "Secondary Outcome", outcomeDTO.getSecondaryOutcomeDescription());
    delegate.createCeOutcomeEvent(outcomeDTO);
  }

  public void processMessage(CENewUnitAddress newUnitAddress) throws GatewayException {
    OutcomeSuperSetDto outcomeDTO = outcomeSuperSetMapper.toOutcomeSuperSetDto(newUnitAddress);
    outcomeDTO.setCaseId(UUID.randomUUID());
    if (!isSurveyEnabled(String.valueOf(outcomeDTO.getCaseId()), "CE", outcomeDTO.getOutcomeCode())) {
      return;
    }
    gatewayEventManager.triggerEvent(String.valueOf(outcomeDTO.getCaseId()), PREPROCESSING_CE_UNITADDRESS_OUTCOME,
        "Survey type", "CE",
        "Outcome code", outcomeDTO.getOutcomeCode(),
        "Secondary Outcome", outcomeDTO.getSecondaryOutcomeDescription());
    delegate.createCeOutcomeEvent(outcomeDTO);
  }

  public void processMessage(CENewStandaloneAddress standaloneAddress) throws GatewayException {
    OutcomeSuperSetDto outcomeDTO = outcomeSuperSetMapper.toOutcomeSuperSetDto(standaloneAddress);
    outcomeDTO.setCaseId(UUID.randomUUID());
    if (!isSurveyEnabled(String.valueOf(outcomeDTO.getCaseId()), "CE", outcomeDTO.getOutcomeCode())) {
      return;
    }
    gatewayEventManager.triggerEvent(String.valueOf(outcomeDTO.getCaseId()), PREPROCESSING_CE_STANDALONE_OUTCOME,
        "Survey type", "CE",
        "Outcome code", outcomeDTO.getOutcomeCode(),
        "Secondary Outcome", outcomeDTO.getSecondaryOutcomeDescription());
    delegate.createCeOutcomeEvent(outcomeDTO);
  }

  public void processMessage(HHOutcome hhOutcome) throws GatewayException {
    OutcomeSuperSetDto outcomeDTO = outcomeSuperSetMapper.toOutcomeSuperSetDto(hhOutcome);
    if (!isSurveyEnabled(String.valueOf(outcomeDTO.getCaseId()), "HH", outcomeDTO.getOutcomeCode())) {
      return;
    }
    gatewayEventManager.triggerEvent(String.valueOf(outcomeDTO.getCaseId()), PREPROCESSING_HH_OUTCOME,
        "Survey type", "HH",
        "Outcome code", outcomeDTO.getOutcomeCode(),
        "Secondary Outcome", outcomeDTO.getSecondaryOutcomeDescription());
    delegate.createHhOutcomeEvent(outcomeDTO);
  }

  public void processMessage(HHNewSplitAddress splitAddress) throws GatewayException {
    OutcomeSuperSetDto outcomeDTO = outcomeSuperSetMapper.toOutcomeSuperSetDto(splitAddress);
    outcomeDTO.setCaseId(UUID.randomUUID());
    if (!isSurveyEnabled(String.valueOf(outcomeDTO.getCaseId()), "HH", outcomeDTO.getOutcomeCode())) {
      return;
    }
    gatewayEventManager.triggerEvent(String.valueOf(outcomeDTO.getCaseId()), PREPROCESSING_HH_SPLITADDRESS_OUTCOME,
        "Survey type", "HH",
        "Outcome code", outcomeDTO.getOutcomeCode(),
        "Secondary Outcome", outcomeDTO.getSecondaryOutcomeDescription());
    delegate.createHhOutcomeEvent(outcomeDTO);
  }

  public void processMessage(HHNewStandaloneAddress standaloneAddress) throws GatewayException {
    OutcomeSuperSetDto outcomeDTO = outcomeSuperSetMapper.toOutcomeSuperSetDto(standaloneAddress);
    outcomeDTO.setCaseId(UUID.randomUUID());
    if (!isSurveyEnabled(String.valueOf(outcomeDTO.getCaseId()), "HH", outcomeDTO.getOutcomeCode())) {
      return;
    }
    gatewayEventManager.triggerEvent(String.valueOf(outcomeDTO.getCaseId()), PREPROCESSING_HH_STANDALONE_OUTCOME,
        "Survey type", "HH",
        "Outcome code", outcomeDTO.getOutcomeCode(),
        "Secondary Outcome", outcomeDTO.getSecondaryOutcomeDescription());
    delegate.createHhOutcomeEvent(outcomeDTO);
  }

  public void processMessage(CCSPropertyListingOutcome ccsPropertyListingOutcome) throws GatewayException {
    OutcomeSuperSetDto outcomeDTO = outcomeSuperSetMapper.toOutcomeSuperSetDto(ccsPropertyListingOutcome);
    if (!isSurveyEnabled(String.valueOf(outcomeDTO.getCaseId()), "CCS PL", outcomeDTO.getOutcomeCode())) {
      return;
    }
    gatewayEventManager.triggerEvent(String.valueOf(outcomeDTO.getCaseId()), PREPROCESSING_CCS_PL_OUTCOME,
        "Survey type", "CCS PL",
        "Outcome code", outcomeDTO.getOutcomeCode(),
        "Secondary Outcome", outcomeDTO.getSecondaryOutcomeDescription());
    delegate.createCcsPropertyListingOutcomeEvent(outcomeDTO);
  }

  public void processMessage(CCSInterviewOutcome ccsInterviewOutcome) throws GatewayException {
    OutcomeSuperSetDto outcomeDTO = outcomeSuperSetMapper.toOutcomeSuperSetDto(ccsInterviewOutcome);
    if (!isSurveyEnabled(String.valueOf(outcomeDTO.getCaseId()), "CCS INT", outcomeDTO.getOutcomeCode())) {
      return;
    }
    gatewayEventManager.triggerEvent(String.valueOf(outcomeDTO.getCaseId()), PREPROCESSING_CCS_INT_OUTCOME,
        "Survey type", "CCS INT",
        "Outcome code", outcomeDTO.getOutcomeCode(),
        "Secondary Outcome", outcomeDTO.getSecondaryOutcomeDescription());
    delegate.createCcsInterviewOutcomeEvent(outcomeDTO);
  }

  public void processMessage(NCOutcome ncOutcome) throws GatewayException {
    OutcomeSuperSetDto outcomeDTO = outcomeSuperSetMapper.toOutcomeSuperSetDto(ncOutcome);
    if (!isSurveyEnabled(String.valueOf(outcomeDTO.getCaseId()), "NC", outcomeDTO.getOutcomeCode())) {
      return;
    }
    gatewayEventManager.triggerEvent(String.valueOf(outcomeDTO.getCaseId()), PREPROCESSING_NC_OUTCOME,
        "Survey type", "NC",
        "Outcome code", outcomeDTO.getOutcomeCode(),
        "Secondary Outcome", outcomeDTO.getSecondaryOutcomeDescription());
    delegate.createNcOutcomeEvent(outcomeDTO);
  }

  private boolean isSurveyEnabled(String caseId, String survey, String outcomeCode) {
    if (outcomeFeatureFlagConfig.isEnabledForSurvey(survey)) {
      return true;
    }

    gatewayEventManager.triggerEvent(caseId, PREPROCESSING_OUTCOME_IGNORED_BY_FEATURE_FLAG,
        "Survey type", survey,
        "Outcome code", String.valueOf(outcomeCode));
    return false;
  }
}
