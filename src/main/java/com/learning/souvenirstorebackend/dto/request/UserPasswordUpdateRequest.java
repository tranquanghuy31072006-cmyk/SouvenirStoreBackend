package com.learning.souvenirstorebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserPasswordUpdateRequest {
    @NotBlank(message = "Old password must not be blank")
    String oldPassword;

    @NotBlank(message = "New password must not be blank")
    @Size(min = 5, max = 50, message = "New password must be between 5 and 50 characters")
    String newPassword;
}
