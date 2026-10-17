import { describe, it, expect } from 'vitest';
import { formatINR, formatDate, getRiskBandDetails } from '../utils/formatters';

describe('Frontend Formatters & Risk Band Logic', () => {
  it('formats Indian currency with rupee symbol and grouping', () => {
    const formatted = formatINR(400000);
    // Should include ₹ and 4,00,000
    expect(formatted).toContain('₹');
    expect(formatted.replace(/\s+/g, '')).toContain('4,00,000');
  });

  it('correctly maps risk bands across all 4 tiers', () => {
    // 0-25: LOW
    expect(getRiskBandDetails(15).level).toBe('LOW');
    expect(getRiskBandDetails(25).level).toBe('LOW');

    // 26-50: MODERATE
    expect(getRiskBandDetails(26).level).toBe('MODERATE');
    expect(getRiskBandDetails(50).level).toBe('MODERATE');

    // 51-75: HIGH (Golden scenario = 73)
    expect(getRiskBandDetails(73).level).toBe('HIGH');
    expect(getRiskBandDetails(75).level).toBe('HIGH');

    // 76-100: CRITICAL (Redistribution scenario = 97)
    expect(getRiskBandDetails(76).level).toBe('CRITICAL');
    expect(getRiskBandDetails(97).level).toBe('CRITICAL');
    expect(getRiskBandDetails(100).level).toBe('CRITICAL');
  });

  it('handles empty or invalid inputs gracefully', () => {
    expect(formatINR(0)).toContain('0');
    expect(formatINR(null)).toBe('₹0');
    expect(formatDate(null)).toBe('—');
  });
});
