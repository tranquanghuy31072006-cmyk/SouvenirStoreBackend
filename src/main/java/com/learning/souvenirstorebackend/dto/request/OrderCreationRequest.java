package com.learning.souvenirstorebackend.dto.request;

import com.learning.souvenirstorebackend.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreationRequest {

    @NotBlank(message = "Receiver name must not be blank")
    @Size(max = 255, message = "Receiver name must not exceed 255 characters")
    private String receiverName;

    @NotBlank(message = "Receiver phone must not be blank")
    private String receiverPhone;

    @NotBlank(message = "Receiver address must not be blank")
    @Size(max = 500, message = "Receiver address must not exceed 500 characters")
    private String receiverAddress;

    @NotNull(message = "Payment method must not be null")
    private PaymentMethod paymentMethod;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;

    private List<Long> cartItemIds;

    private Long buyNowProductVariantId;

    private Integer buyNowQuantity;
}
