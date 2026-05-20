package uk.gov.ons.census.fwmt.outcomeservice.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uk.gov.ons.census.fwmt.common.data.shared.CareCode;
import uk.gov.ons.census.fwmt.common.data.shared.CeDetails;
import uk.gov.ons.census.fwmt.common.data.shared.FulfilmentRequest;

@Mapper(componentModel = "spring")
public interface OutcomeNestedDtoMapper {

  CareCodeDto toCareCodeDto(CareCode careCode);

  FulfilmentRequestDto toFulfilmentRequestDto(FulfilmentRequest fulfilmentRequest);

  @Mapping(target = "establishmentSecure", source = "establishmentSecure", qualifiedByName = "booleanToString")
  @Mapping(target = "bedspaces", source = "bedspaces", qualifiedByName = "stringToInteger")
  @Mapping(target = "accessInfo", ignore = true)
  @Mapping(target = "careCodes", ignore = true)
  CeDetailsDto toCeDetailsDto(CeDetails ceDetails);

  @Named("booleanToString")
  default String booleanToString(boolean establishmentSecure) {
    return Boolean.toString(establishmentSecure);
  }

  @Named("stringToInteger")
  default Integer stringToInteger(String bedspaces) {
    if (bedspaces == null || bedspaces.isEmpty()) {
      return null;
    }
    return Integer.valueOf(bedspaces);
  }
}
