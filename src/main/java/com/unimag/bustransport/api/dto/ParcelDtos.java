package com.unimag.bustransport.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

public class ParcelDtos {
    public record ParcelCreateRequest(
            @NotNull BigDecimal price,
            @NotBlank String senderName,
            String senderPhone,
            @NotBlank String receiverName,
            String receiverPhone,
            @NotNull Long fromStopId,
            @NotNull Long toStopId,
            Long tripId
    ) implements Serializable {}

    public record ParcelUpdateRequest(
            String senderName,
            String senderPhone,
            String receiverName,
            String receiverPhone,
            BigDecimal price,
            Long fromStopId,
            Long toStopId
    ) implements Serializable {}

    public record ParcelResponse(
            Long id,
            String code,
            BigDecimal price,
            String status,
            String proofPhotoUrl,
            String deliveryOtp,
            String senderName,
            String senderPhone,
            String receiverName,
            String receiverPhone,
            TripSummary trip,
            StopSummary fromStop,
            StopSummary toStop
    ) implements Serializable {
        public record TripSummary(Long id, String origin, String destination) implements Serializable {}
        public record StopSummary(Long id, String name, String city) implements Serializable {}
    }
}
