package com.schoovello.pichef.nfc.testing;

public class BitUtils {

	public static byte clearBits(byte value, byte bitsToClear) {
		return (byte) (value & ~(bitsToClear));
	}

	public static byte setBits(byte value, byte bitsToSet) {
		return (byte) (value | bitsToSet);
	}

}
