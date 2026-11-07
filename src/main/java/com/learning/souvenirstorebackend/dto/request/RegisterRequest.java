package com.learning.souvenirstorebackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    String username;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
    String password;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email format is invalid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email;

    @NotBlank(message = "Phone must not be blank")
    @Size(min = 10, max = 10, message = "The phone number must contain exactly 10 digits.")
    String phone;

    @NotBlank(message = "Full name must not be blank")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    String fullName;

    @Past(message = "Date of birth is invalid")
    LocalDate dateOfBirth;
}
