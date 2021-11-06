package me.voltbox.hackergy.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnrichedGrantDto{
    String summary;
    GrantDto grantDto;
}
