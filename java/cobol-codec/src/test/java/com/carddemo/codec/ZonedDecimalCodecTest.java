package com.carddemo.codec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ZonedDecimalCodec}.
 *
 * <p>The empirical anchor is {@code dailytran.txt} row 2's {@code TRAN-AMT}
 * field: bytes {@code "0000009190}"} decode to {@code -919.00}. If that case
 * works, the rest of the table follows from {@link OverpunchTest}.
 */
class ZonedDecimalCodecTest {

    private static final PicClause TRAN_AMT = PicClause.numeric(9, 2, true);  // S9(09)V99

    @Test
    void decode_ascii_negativeFromDailyTranRow2() {
        // The exact 11-byte field from app/data/ASCII/dailytran.txt row 2.
        byte[] field = "0000009190}".getBytes(StandardCharsets.US_ASCII);
        BigDecimal v = ZonedDecimalCodec.decode(field, TRAN_AMT, Encoding.ASCII);
        assertThat(v).isEqualByComparingTo(new BigDecimal("-919.00"));
        assertThat(v.scale()).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({
        // bytes          expected     -- 11 digits, S9(8)V999 (8 integer + 3 fractional)
        "'00000050478',   50.478",
        "'0000005047G',   50.477",   // G = +7 (trailing digit replaced by overpunch)
        "'0000005047P',  -50.477",   // P = -7
        "'0000005047{',   50.470",   // '{' = +0
        "'0000005047}',  -50.470",   // '}' = -0 (still parses as 0 sign-negative)
    })
    void decode_ascii_variousSignedTrailings(String literal, String expected) {
        PicClause pic = PicClause.numeric(8, 3, true);  // S9(8)V999, 11 bytes
        byte[] field = literal.getBytes(StandardCharsets.US_ASCII);
        BigDecimal v = ZonedDecimalCodec.decode(field, pic, Encoding.ASCII);
        assertThat(v).isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    void decode_ebcdic_overpunchUsesEbcdicByte() {
        // EBCDIC: '0' = 0xF0, 'G' = 0xC7.
        byte[] field = new byte[] {
            (byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0,
            (byte)0xF0, (byte)0xF0, (byte)0xF0, (byte)0xF0,
            (byte)0xF0, (byte)0xF0, (byte)0xC7,                  // ...000G  (+7)
        };
        PicClause pic = PicClause.numeric(9, 2, true);
        BigDecimal v = ZonedDecimalCodec.decode(field, pic, Encoding.EBCDIC);
        assertThat(v).isEqualByComparingTo(new BigDecimal("0.07"));
    }

    @Test
    void decode_unsigned() {
        byte[] field = "0000000019".getBytes(StandardCharsets.US_ASCII);
        PicClause pic = PicClause.numeric(10, 0, false);
        BigDecimal v = ZonedDecimalCodec.decode(field, pic, Encoding.ASCII);
        assertThat(v).isEqualByComparingTo(new BigDecimal("19"));
    }

    @Test
    void encode_negative_roundTrip() {
        BigDecimal v = new BigDecimal("-919.00");
        byte[] encoded = ZonedDecimalCodec.encode(v, TRAN_AMT, Encoding.ASCII);
        assertThat(new String(encoded, StandardCharsets.US_ASCII)).isEqualTo("0000009190}");
        BigDecimal back = ZonedDecimalCodec.decode(encoded, TRAN_AMT, Encoding.ASCII);
        assertThat(back).isEqualByComparingTo(v);
    }

    @Test
    void encode_positive_roundTrip() {
        BigDecimal v = new BigDecimal("194.00");
        PicClause pic = PicClause.numeric(10, 2, true);  // S9(10)V99
        byte[] encoded = ZonedDecimalCodec.encode(v, pic, Encoding.ASCII);
        assertThat(new String(encoded, StandardCharsets.US_ASCII)).isEqualTo("00000019400{");
        BigDecimal back = ZonedDecimalCodec.decode(encoded, pic, Encoding.ASCII);
        assertThat(back).isEqualByComparingTo(v);
    }

    @Test
    void encode_positiveZero_distinctFromNegativeZero() {
        PicClause pic = PicClause.numeric(2, 2, true);  // S9(2)V99
        byte[] posZero = ZonedDecimalCodec.encode(new BigDecimal("0.00"), pic, Encoding.ASCII);
        assertThat(new String(posZero, StandardCharsets.US_ASCII)).isEqualTo("000{");
    }

    @Test
    void encode_rejectsScaleMismatch() {
        BigDecimal v = new BigDecimal("1.5");  // scale 1
        PicClause pic = PicClause.numeric(5, 2, true);  // expects scale 2
        assertThatThrownBy(() -> ZonedDecimalCodec.encode(v, pic, Encoding.ASCII))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scale");
    }

    @Test
    void encode_rejectsOverflow() {
        BigDecimal v = new BigDecimal("99999.99");
        PicClause pic = PicClause.numeric(3, 2, true);
        assertThatThrownBy(() -> ZonedDecimalCodec.encode(v, pic, Encoding.ASCII))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digits");
    }

    @Test
    void encode_rejectsNegativeForUnsignedPic() {
        BigDecimal v = new BigDecimal("-1");
        PicClause pic = PicClause.numeric(5, 0, false);
        assertThatThrownBy(() -> ZonedDecimalCodec.encode(v, pic, Encoding.ASCII))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsigned");
    }

    @Test
    void negativeZero_roundTrip_preservesBytesViaFullApi() {
        // decode collapses +0 and -0 to BigDecimal.ZERO, but decodeFull/encodeFull
        // preserves the distinction so round-trip is byte-exact.
        PicClause pic = PicClause.numeric(10, 2, true);
        byte[] negZeroBytes = "00000000000}".getBytes(StandardCharsets.US_ASCII);
        ZonedValue zv = ZonedDecimalCodec.decodeFull(negZeroBytes, pic, Encoding.ASCII);
        assertThat(zv.isNegativeZero()).isTrue();
        assertThat(zv.value()).isEqualByComparingTo(BigDecimal.ZERO);
        byte[] back = ZonedDecimalCodec.encodeFull(zv, pic, Encoding.ASCII);
        assertThat(back).isEqualTo(negZeroBytes);
    }

    @Test
    void positiveZero_decodeFull_hasNegativeZeroFlagFalse() {
        PicClause pic = PicClause.numeric(10, 2, true);
        byte[] posZeroBytes = "00000000000{".getBytes(StandardCharsets.US_ASCII);
        ZonedValue zv = ZonedDecimalCodec.decodeFull(posZeroBytes, pic, Encoding.ASCII);
        assertThat(zv.isNegativeZero()).isFalse();
        assertThat(zv.value()).isEqualByComparingTo(BigDecimal.ZERO);
        byte[] back = ZonedDecimalCodec.encodeFull(zv, pic, Encoding.ASCII);
        assertThat(back).isEqualTo(posZeroBytes);
    }

    @Test
    void decode_rejectsNonDigitInBody() {
        byte[] field = "00X000050{".getBytes(StandardCharsets.US_ASCII);
        PicClause pic = PicClause.numeric(8, 2, true);
        assertThatThrownBy(() -> ZonedDecimalCodec.decode(field, pic, Encoding.ASCII))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
