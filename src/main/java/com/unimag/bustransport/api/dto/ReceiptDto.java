package com.unimag.bustransport.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReceiptDto {
    private Long purchaseId;
    private String receiptNumber;
    private LocalDateTime date;
    private String buyerName;
    private String buyerEmail;
    private String buyerPhone;
    private String routeName;
    private String origin;
    private String destination;
    private LocalDate tripDate;
    private List<String> seatNumbers;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String paymentReference;
    private List<TicketDtos.TicketResponse> tickets;
}