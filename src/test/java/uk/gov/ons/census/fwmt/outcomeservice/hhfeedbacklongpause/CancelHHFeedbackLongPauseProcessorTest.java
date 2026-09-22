package uk.gov.ons.census.fwmt.outcomeservice.hhfeedbacklongpause;

import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtCancelActionInstruction;
import uk.gov.ons.census.fwmt.common.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.config.OutcomeSetup;
import uk.gov.ons.census.fwmt.outcomeservice.converter.RefusalEncryptionLookup;
import uk.gov.ons.census.fwmt.outcomeservice.converter.impl.CancelHHFeedbackLongPauseProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCaseRecord;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.helpers.HardRefusalHelper;
import uk.gov.ons.census.fwmt.outcomeservice.message.PubSubFieldworkActionInstructionPublisher;
import uk.gov.ons.census.fwmt.outcomeservice.template.TemplateCreator;

import java.text.DateFormat;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CancelHHFeedbackLongPauseProcessorTest {

  @InjectMocks
  private CancelHHFeedbackLongPauseProcessor cancelHHFeedbackLongPauseProcessor;

  @Mock
  private PubSubFieldworkActionInstructionPublisher fieldworkActionInstructionPublisher;

  @Mock
  private GatewayCaseRecord gatewayCache;

  @Mock
  private GatewayEventManager eventManager;

  @Mock
  private TemplateCreator temmplateCreator;

  @Mock
  private RefusalEncryptionLookup refusalEncryptionLookup;

  @Mock
  private DateFormat dateFormat;

  @Mock
  private Date date;

  @Mock
  private OutcomeSetup outcomeSetup;

  @Captor
  private ArgumentCaptor<FwmtCancelActionInstruction> longPause;

  @Test
  @DisplayName("Should publish FwmtCancelActionInstruction to the internal action-instruction topic")
  public void shouldPublishFwmtCancelActionInstructionToInternalTopic() throws GatewayException {
    final OutcomeSuperSetDto outcome = new HardRefusalHelper().createHardRefusalOutcomne();
    Assertions.assertEquals(outcome.getCaseId(), cancelHHFeedbackLongPauseProcessor.process(outcome, outcome.getCaseId(), "HH"));
  }

  @Test
  @DisplayName("Should publish FwmtCancelActionInstruction with caseId rather than siteCaseId")
  public void shouldPublishCaseIdInsteadOfSiteCaseId() throws GatewayException, JSONException {
    final OutcomeSuperSetDto outcome = new HardRefusalHelper().createHardRefusalOutcomeWithSite();
    cancelHHFeedbackLongPauseProcessor.process(outcome, outcome.getCaseId(), "HH");
    verify(fieldworkActionInstructionPublisher).publish(longPause.capture(), any());
    FwmtCancelActionInstruction sentPause = longPause.getValue();
    Assertions.assertEquals(outcome.getCaseId().toString(), sentPause.getCaseId());
  }
}
