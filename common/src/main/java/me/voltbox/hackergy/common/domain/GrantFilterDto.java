package me.voltbox.hackergy.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GrantFilterDto {
    String entity;
    String region;
    String category;
    String type;
    String social;
}
