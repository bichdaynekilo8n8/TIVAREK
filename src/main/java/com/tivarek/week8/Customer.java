package com.tivarek.week8;
import java.time.Instant;
public record Customer(Long customerId, String customerCode, String fullName,
                       String phone, String email, Instant createdAt) {}
