package com.carddemo.codec;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Numeric value with sign-of-zero preserved.
 *
 * <p>COBOL DISPLAY-usage signed numerics distinguish +0 and -0 at the byte
 * level: the trailing overpunch is {@code '{'} for +0 and {@code '}'} for
 * -0. A pure {@link BigDecimal} cannot carry that distinction (BigDecimal
 * collapses both to {@code 0}), which would break the codec's round-trip
 * contract on any field that holds -0.
 *
 * <p>For most arithmetic, the {@link BigDecimal} returned by
 * {@link ZonedDecimalCodec#decode} is sufficient — CardDemo's daily-batch
 * code uses signed-zero in just one observed way (default initialization
 * of balance fields to {@code +0}), and the sample fixtures contain
 * {@code +0} but no {@code -0}. Callers that need strict byte-equivalent
 * round-trip can use {@link ZonedDecimalCodec#decodeFull} and
 * {@link ZonedDecimalCodec#encodeFull} which preserve sign-of-zero through
 * this wrapper.
 */
public final class ZonedValue {

    private final BigDecimal value;
    private final boolean negativeZero;

    /**
     * @param value        the numeric value
     * @param negativeZero true if the value is exactly zero and was encoded
     *                     with the {@code '}'} (negative zero) overpunch;
     *                     ignored if the value is nonzero
     */
    public ZonedValue(BigDecimal value, boolean negativeZero) {
        Objects.requireNonNull(value, "value");
        // negativeZero only matters when value is exactly zero. We don't
        // throw if a caller passes negativeZero=true with a nonzero value;
        // we just ignore the flag, because the actual sign already says
        // negative.
        this.value = value;
        this.negativeZero = (value.signum() == 0) && negativeZero;
    }

    /** Convenience: positive-zero or any nonzero value. */
    public static ZonedValue of(BigDecimal v) {
        return new ZonedValue(v, false);
    }

    /** Convenience: explicit negative zero with given scale. */
    public static ZonedValue negativeZero(int scale) {
        return new ZonedValue(BigDecimal.ZERO.setScale(scale), true);
    }

    public BigDecimal value() { return value; }

    /** True iff the value is exactly zero and was a "negative zero" on disk. */
    public boolean isNegativeZero() { return negativeZero; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZonedValue)) return false;
        ZonedValue z = (ZonedValue) o;
        return negativeZero == z.negativeZero && value.compareTo(z.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.stripTrailingZeros(), negativeZero);
    }

    @Override
    public String toString() {
        return negativeZero ? "-" + value.toPlainString() + "(neg0)" : value.toPlainString();
    }
}
