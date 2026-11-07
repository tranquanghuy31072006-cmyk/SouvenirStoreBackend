package com.learning.souvenirstorebackend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.learning.souvenirstorebackend.enums.CategoryStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {

    Long id;

    String name;

    String description;

    CategoryStatus status;
}