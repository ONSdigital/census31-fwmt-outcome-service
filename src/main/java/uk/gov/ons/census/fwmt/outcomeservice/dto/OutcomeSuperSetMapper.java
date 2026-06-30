package uk.gov.ons.census.fwmt.outcomeservice.dto;

import org.mapstruct.Mapper;
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

@Mapper(componentModel = "spring", uses = OutcomeNestedDtoMapper.class)
public interface OutcomeSuperSetMapper {

  OutcomeSuperSetDto toOutcomeSuperSetDto(CEOutcome source);

  OutcomeSuperSetDto toOutcomeSuperSetDto(CENewUnitAddress source);

  OutcomeSuperSetDto toOutcomeSuperSetDto(CENewStandaloneAddress source);

  OutcomeSuperSetDto toOutcomeSuperSetDto(SPGOutcome source);

  OutcomeSuperSetDto toOutcomeSuperSetDto(SPGNewUnitAddress source);

  OutcomeSuperSetDto toOutcomeSuperSetDto(SPGNewStandaloneAddress source);

  OutcomeSuperSetDto toOutcomeSuperSetDto(HHOutcome source);

  OutcomeSuperSetDto toOutcomeSuperSetDto(HHNewSplitAddress source);

  OutcomeSuperSetDto toOutcomeSuperSetDto(HHNewStandaloneAddress source);

  OutcomeSuperSetDto toOutcomeSuperSetDto(CCSPropertyListingOutcome source);

  OutcomeSuperSetDto toOutcomeSuperSetDto(CCSInterviewOutcome source);

  OutcomeSuperSetDto toOutcomeSuperSetDto(NCOutcome source);
}
