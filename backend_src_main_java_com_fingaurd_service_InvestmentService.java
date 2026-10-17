package com.fingaurd.service;

import com.fingaurd.dto.request.InvestmentRequest;
import com.fingaurd.dto.response.CsvImportResultResponse;
import com.fingaurd.exception.BadRequestException;
import com.fingaurd.exception.ResourceNotFoundException;
import com.fingaurd.model.Investment;
import com.fingaurd.model.InvestmentStatus;
import com.fingaurd.model.User;
import com.fingaurd.repository.InvestmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final JourneyService journeyService;
    private final AuditService auditService;

    public InvestmentService(InvestmentRepository investmentRepository,
                             JourneyService journeyService,
                             AuditService auditService) {
        this.investmentRepository = investmentRepository;
        this.journeyService = journeyService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Investment> getUserInvestments(Long userId) {
        return investmentRepository.findByUser_UserIdOrderByInvestedOnDesc(userId);
    }

    @Transactional
    public Investment createInvestment(User user, InvestmentRequest req) {
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Investment amount must be positive.");
        }
        if (req.getInvestedOn() == null) {
            throw new BadRequestException("Investment date is required.");
        }
        if (req.getEntityName() == null || req.getEntityName().trim().isEmpty()) {
            throw new BadRequestException("Entity name cannot be empty.");
        }

        Investment inv = new Investment(
                user,
                req.getEntityName().trim(),
                req.getAmount(),
                req.getInvestedOn(),
                req.getType() != null ? req.getType().trim() : "EQUITY",
                req.getReturnAmount(),
                req.getStatus() != null ? req.getStatus() : InvestmentStatus.ACTIVE
        );

        inv = investmentRepository.save(inv);
        journeyService.getOrCreateUserJourney(user);
        auditService.recordEvent(user, user.getEmail(), "CREATE_INVESTMENT", "INVESTMENT#" + inv.getInvestmentId(), "SUCCESS");
        return inv;
    }

    @Transactional
    public Investment updateInvestment(User user, Long investmentId, InvestmentRequest req) {
        Investment inv = investmentRepository.findByInvestmentIdAndUser_UserId(investmentId, user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found or access denied."));

        if (req.getAmount() != null) {
            if (req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Investment amount must be positive.");
            }
            inv.setAmount(req.getAmount());
        }
        if (req.getEntityName() != null && !req.getEntityName().trim().isEmpty()) {
            inv.setEntityName(req.getEntityName().trim());
        }
        if (req.getInvestedOn() != null) {
            inv.setInvestedOn(req.getInvestedOn());
        }
        if (req.getType() != null) {
            inv.setType(req.getType());
        }
        if (req.getReturnAmount() != null) {
            inv.setReturnAmount(req.getReturnAmount());
        }
        if (req.getStatus() != null) {
            inv.setStatus(req.getStatus());
        }

        inv = investmentRepository.save(inv);
        journeyService.getOrCreateUserJourney(user);
        auditService.recordEvent(user, user.getEmail(), "UPDATE_INVESTMENT", "INVESTMENT#" + inv.getInvestmentId(), "SUCCESS");
        return inv;
    }

    @Transactional
    public void deleteInvestment(User user, Long investmentId) {
        Investment inv = investmentRepository.findByInvestmentIdAndUser_UserId(investmentId, user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found or access denied."));

        investmentRepository.delete(inv);
        journeyService.getOrCreateUserJourney(user);
        auditService.recordEvent(user, user.getEmail(), "DELETE_INVESTMENT", "INVESTMENT#" + investmentId, "SUCCESS");
    }

    @Transactional
    public CsvImportResultResponse importCsv(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a non-empty CSV file to upload.");
        }

        List<String> errors = new ArrayList<>();
        List<Investment> validInvestments = new ArrayList<>();
        int lineNum = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                // Strip quotes and split by comma
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (isHeader) {
                    isHeader = false;
                    // Check if it's indeed header
                    if (line.toLowerCase().contains("entity_name") || line.toLowerCase().contains("amount")) {
                        continue;
                    }
                }

                if (parts.length < 3) {
                    errors.add(String.format("Row %d: Missing required columns (minimum: entity_name, amount, invested_on).", lineNum));
                    continue;
                }

                String entityName = cleanCsvField(parts[0]);
                String amountStr = cleanCsvField(parts[1]);
                String dateStr = cleanCsvField(parts[2]);
                String type = parts.length > 3 ? cleanCsvField(parts[3]) : "EQUITY";
                String returnAmtStr = parts.length > 4 ? cleanCsvField(parts[4]) : null;
                String statusStr = parts.length > 5 ? cleanCsvField(parts[5]) : "ACTIVE";

                if (entityName.isEmpty()) {
                    errors.add(String.format("Row %d: Entity name cannot be empty.", lineNum));
                    continue;
                }

                BigDecimal amount;
                try {
                    amount = new BigDecimal(amountStr.replaceAll("[^0.0-9.]", ""));
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add(String.format("Row %d: Amount must be strictly greater than 0.", lineNum));
                        continue;
                    }
                } catch (Exception e) {
                    errors.add(String.format("Row %d: Invalid decimal format for amount '%s'.", lineNum, amountStr));
                    continue;
                }

                LocalDate date;
                try {
                    date = LocalDate.parse(dateStr);
                } catch (DateTimeParseException e) {
                    errors.add(String.format("Row %d: Invalid date format '%s'. Expected YYYY-MM-DD.", lineNum, dateStr));
                    continue;
                }

                BigDecimal returnAmount = null;
                if (returnAmtStr != null && !returnAmtStr.trim().isEmpty()) {
                    try {
                        returnAmount = new BigDecimal(returnAmtStr.replaceAll("[^0-9.]", ""));
                    } catch (Exception ignored) {}
                }

                InvestmentStatus status = InvestmentStatus.ACTIVE;
                try {
                    if (statusStr != null && !statusStr.trim().isEmpty()) {
                        status = InvestmentStatus.valueOf(statusStr.toUpperCase().trim());
                    }
                } catch (IllegalArgumentException e) {
                    status = InvestmentStatus.ACTIVE;
                }

                Investment inv = new Investment(user, entityName, amount, date, type.isEmpty() ? "EQUITY" : type, returnAmount, status);
                validInvestments.add(inv);
            }

            if (!validInvestments.isEmpty()) {
                investmentRepository.saveAll(validInvestments);
                journeyService.getOrCreateUserJourney(user);
                auditService.recordEvent(user, user.getEmail(), "IMPORT_CSV", "INVESTMENTS", "SUCCESS_COUNT_" + validInvestments.size());
            }

            return new CsvImportResultResponse(lineNum - 1, validInvestments.size(), errors.size(), errors);

        } catch (Exception ex) {
            throw new BadRequestException("Failed to parse CSV file: " + ex.getMessage());
        }
    }

    private String cleanCsvField(String field) {
        if (field == null) return "";
        String clean = field.trim();
        if (clean.startsWith("\"") && clean.endsWith("\"") && clean.length() >= 2) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }
        return clean;
    }
}
