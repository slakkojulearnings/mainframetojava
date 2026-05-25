package com.carddemo.codec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the IBM overpunch table.
 *
 * <p>Reference: the COBOL {@code SIGN TRAILING} convention with the sign on
 * the last digit. Worked example is {@code dailytran.txt} row 2's TRAN-AMT
 * field {@code 0000009190}}, which decodes to -919.00.
 */
class OverpunchTest {

    @ParameterizedTest
    @CsvSource({
        // Positive row: { A B C D E F G H I
        "'{', 0, POSITIVE",
        "A,   1, POSITIVE",
        "B,   2, POSITIVE",
        "C,   3, POSITIVE",
        "D,   4, POSITIVE",
        "E,   5, POSITIVE",
        "F,   6, POSITIVE",
        "G,   7, POSITIVE",
        "H,   8, POSITIVE",
        "I,   9, POSITIVE",
        // Negative row: } J K L M N O P Q R
        "'}', 0, NEGATIVE",
        "J,   1, NEGATIVE",
        "K,   2, NEGATIVE",
        "L,   3, NEGATIVE",
        "M,   4, NEGATIVE",
        "N,   5, NEGATIVE",
        "O,   6, NEGATIVE",
        "P,   7, NEGATIVE",
        "Q,   8, NEGATIVE",
        "R,   9, NEGATIVE",
        // Plain digits decode as positive with the digit value.
        "0,   0, POSITIVE",
        "5,   5, POSITIVE",
        "9,   9, POSITIVE",
    })
    void decode_returnsExpectedDigitAndSign(char input, int expectedDigit, Overpunch.Sign expectedSign) {
        Overpunch.Decoded d = Overpunch.decode(input);
        assertThat(d.digit).isEqualTo(expectedDigit);
        assertThat(d.sign).isEqualTo(expectedSign);
    }

    @Test
    void decode_rejectsNonOverpunchCharacter() {
        assertThatThrownBy(() -> Overpunch.decode(' '))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Overpunch.decode('Z'))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Overpunch.decode('!'))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "0, POSITIVE, '{'",
        "1, POSITIVE, A",
        "9, POSITIVE, I",
        "0, NEGATIVE, '}'",
        "1, NEGATIVE, J",
        "9, NEGATIVE, R",
    })
    void encode_returnsExpectedCharacter(int digit, Overpunch.Sign sign, char expected) {
        assertThat(Overpunch.encode(digit, sign)).isEqualTo(expected);
    }

    @Test
    void encode_rejectsDigitOutOfRange() {
        assertThatThrownBy(() -> Overpunch.encode(-1, Overpunch.Sign.POSITIVE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Overpunch.encode(10, Overpunch.Sign.NEGATIVE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "0, POSITIVE",
        "5, POSITIVE",
        "9, POSITIVE",
        "0, NEGATIVE",
        "5, NEGATIVE",
        "9, NEGATIVE",
    })
    void encodeThenDecode_isIdentity(int digit, Overpunch.Sign sign) {
        char encoded = Overpunch.encode(digit, sign);
        Overpunch.Decoded d = Overpunch.decode(encoded);
        assertThat(d.digit).isEqualTo(digit);
        assertThat(d.sign).isEqualTo(sign);
    }

    @Test
    void positiveZeroAndNegativeZero_haveDistinctEncodings() {
        // COBOL preserves the sign of zero in the byte layout, even though
        // the numeric value is the same. The codec must not silently collapse
        // them, because some COBOL programs check the byte directly.
        assertThat(Overpunch.encode(0, Overpunch.Sign.POSITIVE)).isEqualTo('{');
        assertThat(Overpunch.encode(0, Overpunch.Sign.NEGATIVE)).isEqualTo('}');
    }
}
