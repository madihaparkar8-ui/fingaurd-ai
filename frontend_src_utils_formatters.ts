import { RiskLevel } from '../types';

export const formatINR = (amount: number | null | undefined): string => {
  if (amount === null || amount === undefined || isNaN(amount)) {
    return '₹0';
  }
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(amount);
};

export const formatDate = (dateStr: string | null | undefined): string => {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  } catch {
    return dateStr;
  }
};

export const getRiskBandDetails = (score: number): {
  level: RiskLevel;
  label: string;
  color: string;
  bgColor: string;
  borderColor: string;
  description: string;
} => {
  if (score <= 25) {
    return {
      level: 'LOW',
      label: 'Low Risk',
      color: '#5FA37A',
      bgColor: '#E6F2EA',
      borderColor: '#A4D1B5',
      description: 'Transaction fits standard historical investment ranges.',
    };
  }
  if (score <= 50) {
    return {
      level: 'MODERATE',
      label: 'Moderate Risk',
      color: '#D9A441',
      bgColor: '#FBF1DB',
      borderColor: '#F0D49C',
      description: 'Mild escalation or minor deviation from baseline norms.',
    };
  }
  if (score <= 75) {
    return {
      level: 'HIGH',
      label: 'High Risk',
      color: '#D9784A',
      bgColor: '#FBE9E0',
      borderColor: '#F0BC9F',
      description: 'Substantial multi-fold capital jump or velocity acceleration.',
    };
  }
  return {
    level: 'CRITICAL',
    label: 'Critical Risk',
    color: '#B4414B',
    bgColor: '#F6E2E4',
    borderColor: '#E29DA3',
    description: 'High-risk behavioural anomaly and coercive grooming patterns detected.',
  };
};
