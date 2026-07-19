package com.company.portal.identity.mfa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Base32Test {

    @Test
    void roundTripsRandomBytes() {
        byte[] input = new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };
        String encoded = Base32.encode(input);
        assertThat(encoded).matches("[A-Z2-7]+");
        assertThat(Base32.decode(encoded)).containsExactly(input);
    }

    @Test
    void toleratesPaddingAndCaseOnDecode() {
        byte[] input = "hello world".getBytes();
        String encoded = Base32.encode(input);
        assertThat(Base32.decode(encoded.toLowerCase() + "===")).containsExactly(input);
    }

    @Test
    void emptyInputProducesEmptyOutput() {
        assertThat(Base32.encode(new byte[0])).isEmpty();
        assertThat(Base32.decode("")).isEmpty();
    }
}
