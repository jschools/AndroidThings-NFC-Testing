package com.schoovello.pichef.nfc.testing;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class BitUtilsTest {

	@Test
	public void testClearBits() {
		byte value;
		byte bitsToClear;
		byte expected;
		byte actual;

		value = (byte) 0b1111_1111;
		bitsToClear = (byte) 0b0000_0000;
		expected = (byte) 0b1111_1111;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b1111_1111;
		bitsToClear = (byte) 0b1111_1111;
		expected = (byte) 0b0000_0000;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b1111_1111;
		bitsToClear = (byte) 0b0000_1111;
		expected = (byte) 0b1111_0000;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b1111_1111;
		bitsToClear = (byte) 0b1111_0000;
		expected = (byte) 0b0000_1111;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b1111_1111;
		bitsToClear = (byte) 0b1010_1010;
		expected = (byte) 0b0101_0101;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b0000_0000;
		bitsToClear = (byte) 0b0000_0000;
		expected = (byte) 0b0000_0000;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b0000_0000;
		bitsToClear = (byte) 0b1111_1111;
		expected = (byte) 0b0000_0000;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b0000_0000;
		bitsToClear = (byte) 0b1111_0000;
		expected = (byte) 0b0000_0000;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b0000_0000;
		bitsToClear = (byte) 0b0000_1111;
		expected = (byte) 0b0000_0000;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b1111_0000;
		bitsToClear = (byte) 0b0000_0000;
		expected = (byte) 0b1111_0000;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b1111_0000;
		bitsToClear = (byte) 0b1111_0000;
		expected = (byte) 0b0000_0000;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b1111_0000;
		bitsToClear = (byte) 0b0000_0000;
		expected = (byte) 0b1111_0000;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b1100_0011;
		bitsToClear = (byte) 0b0000_0000;
		expected = (byte) 0b1100_0011;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b1100_0011;
		bitsToClear = (byte) 0b0011_1100;
		expected = (byte) 0b1100_0011;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b1100_0011;
		bitsToClear = (byte) 0b1111_1100;
		expected = (byte) 0b0000_0011;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b1100_0011;
		bitsToClear = (byte) 0b1111_1111;
		expected = (byte) 0b0000_0000;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);

		value = (byte) 0b1100_0011;
		bitsToClear = (byte) 0b1010_0101;
		expected = (byte) 0b0100_0010;
		actual = BitUtils.clearBits(value, bitsToClear);
		assertEquals(expected, actual);
	}

}
