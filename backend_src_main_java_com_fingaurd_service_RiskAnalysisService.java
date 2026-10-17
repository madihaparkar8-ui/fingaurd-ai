package com.fingaurd.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fingaurd.dto.request.AnalyseRequest;
import com.fingaurd.dto.response.*;
import com.fingaurd.exception.ResourceNotFoundException;
import com.fingaurd.model.*;
import com.fingaurd.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RiskAnalysisService {

    private final InvestmentRepository investmentRepository;
    private final JourneyService journeyService;
    private final JourneyFeatureService journeyFeatureService;
    private final DeviationAnomalyDetector anomalyDetector;
    private final InfluenceNlpService influenceNlpService;
    private final RiskFusionEngine riskFusionEngine;
    private final ExplainabilityService explainabilityService;
    private final RiskReportRepository riskReportRepository;
    private final RiskSignalRepository riskSignalRepository;
    private final ConsentRecordRepository consentRecordRepository;
    private final CommunicationRepository communicationRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public RiskAnalysisService(InvestmentRepository investmentRepository,
                               JourneyService journeyService,
                               JourneyFeatureService journeyFeatureService,
                               DeviationAnomalyDetector anomalyDetector,
                               InfluenceNlpService influenceNlpService,
                               RiskFusionEngine riskFusionEngine,
                               ExplainabilityService explainabilityService,
                               RiskReportRepository riskReportRepository,
                               RiskSignalRepository riskSignalRepository,
                               ConsentRecordRepository consentRecordRepository,
                               CommunicationRepository communicationRepository,
                               AuditService auditService,
                               ObjectMapper objectMapper) {
        this.investmentRepository = investmentRepository;
        this.journeyService = journeyService;
        this.journeyFeatureService = journeyFeatureService;
        this.anomalyDetector = anomalyDetector;
        this.influenceNlpService = influenceNlpService;
        this.riskFusionEngine = riskFusionEngine;
        this.explainabilityService = explainabilityService;
        this.riskReportRepository = riskReportRepository;
        this.riskSignalRepository = riskSignalRepository;
        this.consentRecordRepository = consentRecordRepository;
        this.communicationRepository = communicationRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RiskReportResponse analyseProposedCommitment(User user, AnalyseRequest req) {
        Journey journey = journeyService.getOrCreateUserJourney(user);
        List<Investment> history = investmentRepository.findByUser_UserIdOrderByInvestedOnAsc(user.getUserId());
        history = journeyFeatureService.normaliseAndSort(history);

        // 1. Consent handling
        boolean consent = Boolean.TRUE.equals(req.getConsentGranted());
        boolean hasComm = req.getCommunicationText() != null && !req.getCommunicationText().trim().isEmpty();
        boolean commUsed = consent && hasComm;

        if (consent) {
            ConsentRecord consentRecord = new ConsentRecord(user.getUserId(), "COMMUNICATION_ANALYSIS", true, LocalDateTime.now());
            consentRecordRepository.save(consentRecord);
        }

        // 2. Extract Journey Features
        JourneyFeatures features = journeyFeatureService.extractFeatures(history, req.getAmount(), req.getDate(), req.getEntity());

        // 3. Anomaly Ensemble
        AnomalyEnsembleDto anomalyEnsemble = anomalyDetector.calculateAnomalyEnsemble(features);

        // 4. Rule-based NLP Influence Analysis
        InfluenceAnalysisResponse influenceResult = null;
        if (commUsed) {
            influenceResult = influenceNlpService.analyse(req.getCommunicationText(), true);
            try {
                String signalsJson = objectMapper.writeValueAsString(influenceResult.getDetectedCategories());
                Communication comm = new Communication(user, req.getCommunicationText(), LocalDateTime.now(), signalsJson);
                communicationRepository.save(comm);
            } catch (Exception ignored) {}
        }

        double influenceScore = (influenceResult != null) ? influenceResult.getScoreContribution() : 0.0;

        // 5. Risk Fusion Engine
        RiskFusionEngine.FusionResult fusionResult = riskFusionEngine.computeRisk(features, influenceScore, commUsed);

        // 6. Persist RiskReport
        String componentJson = "{}";
        try {
            componentJson = objectMapper.writeValueAsString(fusionResult.getComponentScores());
        } catch (Exception ignored) {}

        RiskReport report = new RiskReport(
                journey,
                req.getAmount(),
                req.getEntity(),
                componentJson,
                anomalyEnsemble.getAnomalyScore(),
                fusionResult.getOverallScore(),
                fusionResult.getRiskLevel(),
                commUsed
        );
        report = riskReportRepository.save(report);

        // 7. Generate & Persist Risk Signals
        List<RiskSignal> signals = explainabilityService.generateSignals(features, anomalyEnsemble, influenceResult, history);
        for (RiskSignal s : signals) {
            report.addSignal(s);
            riskSignalRepository.save(s);
        }

        // 8. Detect Behavioural Biases
        List<BehaviouralBiasDto> biases = explainabilityService.detectBehaviouralBiases(features, influenceResult, history);

        // 9. Record Audit
        auditService.recordEvent(user, user.getEmail(), "ANALYSE_COMMITMENT", "REPORT#" + report.getReportId(),
                fusionResult.getRiskLevel() + " (" + fusionResult.getOverallScore() + ")");

        // 10. Format response
        return toReportResponse(report, fusionResult.getComponentScores(), anomalyEnsemble, signals, biases);
    }

    @Transactional(readOnly = true)
    public RiskReportResponse getReportById(User user, Long reportId) {
        RiskReport report = riskReportRepository.findByReportIdAndJourney_User_UserId(reportId, user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Risk report not found with ID: " + reportId));

        List<RiskSignal> signals = riskSignalRepository.findByRiskReport_ReportId(report.getReportId());
        List<Investment> history = investmentRepository.findByUser_UserIdOrderByInvestedOnAsc(user.getUserId());
        history = journeyFeatureService.normaliseAndSort(history);

        JourneyFeatures features = journeyFeatureService.extractFeatures(
                history,
                report.getTargetInvestment(),
                report.getCreatedAt().toLocalDate(),
                report.getTargetEntity()
        );

        List<BehaviouralBiasDto> biases = explainabilityService.detectBehaviouralBiases(features, null, history);
        AnomalyEnsembleDto anomalyEnsemble = anomalyDetector.calculateAnomalyEnsemble(features);

        ComponentScoresDto scoresDto = null;
        try {
            scoresDto = objectMapper.readValue(report.getComponentScores(), ComponentScoresDto.class);
        } catch (Exception ignored) {}

        return toReportResponse(report, scoresDto, anomalyEnsemble, signals, biases);
    }

    @Transactional(readOnly = true)
    public RiskSignalGroupResponse getSignalsByReportId(User user, Long reportId) {
        RiskReport report = riskReportRepository.findByReportIdAndJourney_User_UserId(reportId, user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Risk report not found with ID: " + reportId));

        List<RiskSignal> signals = riskSignalRepository.findByRiskReport_ReportId(report.getReportId());
        return explainabilityService.groupSignals(signals);
    }

    @Transactional(readOnly = true)
    public List<RiskReportResponse> getUserReportsHistory(Long userId) {
        List<RiskReport> reports = riskReportRepository.findByJourney_User_UserIdOrderByCreatedAtDesc(userId);
        List<RiskReportResponse> result = new ArrayList<>();
        for (RiskReport r : reports) {
            ComponentScoresDto scoresDto = null;
            try {
                scoresDto = objectMapper.readValue(r.getComponentScores(), ComponentScoresDto.class);
            } catch (Exception ignored) {}
            result.add(toReportResponse(r, scoresDto, null, null, null));
        }
        return result;
    }

    private RiskReportResponse toReportResponse(RiskReport report,
                                               ComponentScoresDto scoresDto,
                                               AnomalyEnsembleDto anomalyEnsemble,
                                               List<RiskSignal> signals,
                                               List<BehaviouralBiasDto> biases) {
        RiskReportResponse resp = new RiskReportResponse();
        resp.setReportId(report.getReportId());
        resp.setJourneyId(report.getJourneyId());
        resp.setTargetInvestment(report.getTargetInvestment());
        resp.setTargetEntity(report.getTargetEntity());
        resp.setComponentScores(report.getComponentScores());
        resp.setAnomalyScore(report.getAnomalyScore());
        resp.setOverallScore(report.getOverallScore());
        resp.setRiskLevel(report.getRiskLevel());
        resp.setCommunicationUsed(report.getCommunicationUsed());
        resp.setCreatedAt(report.getCreatedAt());

        resp.setComponentScoresDetail(scoresDto);
        resp.setAnomalyEnsemble(anomalyEnsemble);

        if (signals != null) {
            List<RiskSignalDto> dtos = new ArrayList<>();
            for (RiskSignal s : signals) {
                dtos.add(RiskSignalDto.fromEntity(s, null));
            }
            resp.setSignals(dtos);
            resp.setGroupedSignals(explainabilityService.groupSignals(signals));
        }

        if (biases != null) {
            resp.setBehaviouralBiases(biases);
        }

        return resp;
    }
}
