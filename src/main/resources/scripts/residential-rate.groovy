/**
 * Residential tiered rate calculation script.
 *
 * Bound variables:
 *   meterRead - the MeterRead entity
 *   account   - the Account entity
 *   tariff    - the Tariff entity
 *   usage     - BigDecimal total usage in kWh
 *
 * Returns: BigDecimal charge amount
 *
 * This script demonstrates the CC&B service script pattern:
 * billing analysts can modify rate logic without redeploying Java.
 */

def charge = BigDecimal.ZERO
def remaining = usage

// Tier 1: first 500 kWh at base rate
def tier1Limit = tariff.tier1Limit ?: 500
def tier1Rate = tariff.tier1Rate ?: new BigDecimal("0.08")
def tier1Usage = remaining.min(new BigDecimal(tier1Limit))
charge = charge.add(tier1Usage.multiply(tier1Rate))
remaining = remaining.subtract(tier1Usage).max(BigDecimal.ZERO)

// Tier 2: 501-1000 kWh at mid rate
if (remaining > BigDecimal.ZERO) {
    def tier2Limit = (tariff.tier2Limit ?: 1000) - tier1Limit
    def tier2Rate = tariff.tier2Rate ?: new BigDecimal("0.12")
    def tier2Usage = remaining.min(new BigDecimal(tier2Limit))
    charge = charge.add(tier2Usage.multiply(tier2Rate))
    remaining = remaining.subtract(tier2Usage).max(BigDecimal.ZERO)
}

// Tier 3: above 1000 kWh at premium rate
if (remaining > BigDecimal.ZERO) {
    def tier3Rate = tariff.tier3Rate ?: new BigDecimal("0.18")
    charge = charge.add(remaining.multiply(tier3Rate))
}

// Summer surcharge (June-September)
def month = java.time.LocalDate.now().monthValue
if (month >= 6 && month <= 9) {
    charge = charge.multiply(new BigDecimal("1.15")) // 15% summer premium
}

return charge.setScale(2, java.math.RoundingMode.HALF_UP)
