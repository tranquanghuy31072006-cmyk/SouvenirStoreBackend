package com.learning.souvenirstorebackend.dto.request;

import jakarta.validation.constraints.Past;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {

    String fullName;

    @Past(message = "Date of birth is invalid")
    LocalDate dateOfBirth;

    String phone;
}