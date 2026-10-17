export type Role = 'USER' | 'ADMIN';

export type InvestmentStatus = 'ACTIVE' | 'RETURNED' | 'CLOSED';

export type RiskLevel = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';

export interface User {
  userId: number;
  name: string;
  email: string;
  role: Role;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  name: string;
  email: string;
  role: Role;
}

export interface Investment {
  investmentId: number;
  userId?: number;
  entityName: string;
  amount: number;
  investedOn: string;
  type: string;
  returnAmount?: number | null;
  status: InvestmentStatus;
}

export interface JourneyEvent {
  investmentId: number;
  entityName: string;
  amount: number;
  investedOn: string;
  type: string;
  returnAmount?: number | null;
  status: InvestmentStatus;
  stepRatio: number;
  badges: string[];
}

export interface Journey {
  journeyId: number;
  userId: number;
  startDate: string;
  endDate: string;
  eventCount: number;
  totalExposure: number;
  maxSingleCommitment: number;
  mean: number;
  standardDeviation: number;
  durationDays: number;
  events: JourneyEvent[];
}

export interface ComponentScores {
  escalationScore: number;
  escalationMax: number;
  baselineDeviationScore: number;
  baselineMax: number;
  velocityScore: number;
  velocityMax: number;
  influenceScore: number;
  influenceMax: number;
  redistributed: boolean;
}

export interface AnomalyEnsemble {
  zScoreSignal: number;
  stepChangeSignal: number;
  maxRatioSignal: number;
  anomalyScore: number;
  zScore: number;
  maxStepRatio: number;
  historicalMaxRatio: number;
  velocityPerDay: number;
  entityConcentration: number;
  returnFollowedByEscalation: boolean;
}

export interface RiskSignal {
  signalId?: number;
  type: string;
  value: string;
  explanation: string;
  sourceEvent?: number | null;
  category?: 'what_changed' | 'why_it_matters' | 'verify';
}

export interface GroupedSignals {
  whatChanged: RiskSignal[];
  whyItMatters: RiskSignal[];
  whatCanIVerify: RiskSignal[];
}

export interface BehaviouralBias {
  biasType: string;
  title: string;
  severity: 'LOW' | 'MODERATE' | 'HIGH';
  triggerEvidence: string;
  psychologicalMechanism: string;
  actionableCountermeasure: string;
}

export interface RiskReport {
  reportId: number;
  journeyId: number;
  targetInvestment: number;
  targetEntity: string;
  componentScores: string;
  anomalyScore: number;
  overallScore: number;
  riskLevel: RiskLevel;
  communicationUsed: boolean;
  createdAt: string;
  disclaimer: string;
  componentScoresDetail: ComponentScores;
  anomalyEnsemble: AnomalyEnsemble;
  signals: RiskSignal[];
  groupedSignals: GroupedSignals;
  behaviouralBiases?: BehaviouralBias[];
}

export interface InfluenceCategory {
  category: string;
  label: string;
  confidence: number;
  matchedEvidence: string[];
}

export interface InfluenceAnalysis {
  heading: string;
  text: string;
  indicatorsDetected: boolean;
  highestConfidence: number;
  scoreContribution: number;
  detectedCategories: InfluenceCategory[];
  disclaimer: string;
}

export interface AuditEvent {
  eventId: number;
  userId?: number;
  actor: string;
  action: string;
  resource: string;
  timestamp: string;
  result: string;
}

export interface ConsentRecord {
  consentId: number;
  userId: number;
  purpose: string;
  granted: boolean;
  timestamp: string;
}
