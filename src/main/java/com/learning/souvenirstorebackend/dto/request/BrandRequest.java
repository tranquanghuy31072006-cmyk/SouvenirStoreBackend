package com.learning.souvenirstorebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandRequest {

    @NotBlank(message = "Brand name must not be blank")
    @Size(max = 255, message = "Brand name must not exceed 255 characters")
    private String name;

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String logo;
}
