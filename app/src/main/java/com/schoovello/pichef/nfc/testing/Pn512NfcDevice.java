/*
	Copyright 2018 Jonathan O. Schooler
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
		http://www.apache.org/licenses/LICENSE-2.0
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
 */

package com.schoovello.pichef.nfc.testing;

import android.support.annotation.NonNull;

import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Interface to a PN512 device configured for SPI. Intended to be used with the Explore-NFC board
 * attached to a Raspberry Pi running Android Things.<p/>
 * Most method calls are blocking, so they should be called from a background thread.
 */
public class Pn512NfcDevice {

	private final SpiDevice mSpi;

	private final byte[] mTxBuffer = new byte[256];
	private final byte[] mRxBuffer = new byte[256];

	public Pn512NfcDevice(@NonNull SpiDevice spiDevice) throws IOException {
		mSpi = spiDevice;
		mSpi.setFrequency(250_000);
		mSpi.setBitsPerWord(8);
		mSpi.setMode(SpiDevice.MODE0);
		mSpi.setCsChange(false);
	}

	public void close() {
		try {
			mSpi.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void softReset() throws IOException {
		writeCommand(Command.SOFT_RESET);
	}

	/**
	 * Performs a self test according to the instructions in the manual.
	 * @return {@code true} if the test was successful.
	 */
	public boolean selfTest() throws IOException {
		// 1. Perform a soft reset.
		softReset();

		// 2. Clear the internal buffer by writing 25 bytes of 00h and perform the Config Command.
		writeFifo(newData(25, (byte) 0x00));
		writeCommand(Command.CONFIGURE);
		waitForIdle();

		// 3. Enable the Selftest by writing the value 09h to the register AutoTestReg.
		writeRegister(RegisterAddress.AUTO_TEST_REG, Values.AUTO_TEST_ENABLE_SELF_TEST);

		// 4. Write 00h to the FIFO.
		writeFifo(new byte[] { 0x00 });

		// 5. Start the Selftest with the CalcCRC Command.
		writeCommand(Command.CALC_CRC);

		// wait for self test to finish
		waitForIdle();

		// validate fifo
		byte[] result = readFifo(ConstantData.SELF_TEST_EXPECTED_RESULT.length);
		boolean valid = true;
		for (int i = 0; i < ConstantData.SELF_TEST_EXPECTED_RESULT.length; i++) {
			if (result[i] != ConstantData.SELF_TEST_EXPECTED_RESULT[i]) {
				valid = false;
				break;
			}
		}

		return valid;
	}

	public boolean tryActivateMiFare() throws IOException {
		// enable Rx (writing NO_CMD_CHANGE clears RcvOff and PowerDown bits)
		writeRegister(RegisterAddress.COMMAND_REG, Command.NO_CMD_CHANGE);

		// enable initiator mode
		byte controlRegValue = readRegister(RegisterAddress.CONTROL_REG);
		controlRegValue = BitUtils.setBits(controlRegValue, (byte) 0b0001_0000);
		writeRegister(RegisterAddress.CONTROL_REG, controlRegValue);

		// enable RF
		writeRegister(RegisterAddress.TX_CONTROL_REG, (byte) 0b1000_0010);

		// Send WUPA
		//  Clear FIFO
		flushFifo();

		// WUPA is a 7-bit command, so set the framing params
		byte bitFramingRegValue = readRegister(RegisterAddress.BIT_FRAMING_REG);
		bitFramingRegValue = BitUtils.setBits(bitFramingRegValue, (byte) 0b0000_0111);
		writeRegister(RegisterAddress.BIT_FRAMING_REG, bitFramingRegValue);

		//  Write command sequence to FIFO
		writeFifo(new byte[] {MiFareDevice.Command.WUPA});

		// clear interrupt flags
		clearAllInterruptFlags();

		//  Write Transceive command
		writeCommand(Command.TRANSCEIVE);

		//  Set BitFramingReg register’s StartSend bit to logic 1.
		bitFramingRegValue = readRegister(RegisterAddress.BIT_FRAMING_REG);
		bitFramingRegValue = BitUtils.setBits(bitFramingRegValue, (byte) 0b1000_0000);
		writeRegister(RegisterAddress.BIT_FRAMING_REG, bitFramingRegValue);

		// wait for finished signals
		byte irqRegValue;
		do {
			smallDelayBlocking();
			irqRegValue = readRegister(RegisterAddress.COMM_IRQ_REG);
		} while ((irqRegValue & (byte) 0b0011_0000) != (byte) 0b0011_0000);


		// cancel transceive command
		writeCommand(Command.IDLE);

		//  Read FIFO
		byte[] result = readFifo(2);

		//  Check expected response
		byte[] expected = { (byte) 0x44, (byte) 0x00 };
		return Arrays.equals(expected, result);
	}

	private void clearAllInterruptFlags() throws IOException {
		writeRegister(RegisterAddress.COMM_IRQ_REG, (byte) 0b0111_1111);
	}

	private void flushFifo() throws IOException {
		writeRegister(RegisterAddress.FIFO_LEVEL_REG, (byte) 0b1000_0000);
	}

	private void writeCommand(byte command) throws IOException {
		writeRegister(RegisterAddress.COMMAND_REG, command);
	}

	private void writeRegister(byte regAddress, byte data) throws IOException {
		mTxBuffer[0] = getSpiWriteAddress(regAddress);
		mTxBuffer[1] = data;

		mSpi.write(mTxBuffer, 2);
	}

	private byte readRegister(byte regAddress) throws IOException {
		mTxBuffer[0] = getSpiReadAddress(regAddress);
		mTxBuffer[1] = 0;

		mSpi.transfer(mTxBuffer, mRxBuffer, 2);

		return mRxBuffer[1];
	}

	private void writeData(byte regAddress, byte[] data) throws IOException {
		if (data.length >= mTxBuffer.length) {
			throw new IllegalArgumentException("data.length must be less than " + mTxBuffer.length);
		}

		mTxBuffer[0] = getSpiWriteAddress(regAddress);
		System.arraycopy(data, 0, mTxBuffer, 1, data.length);

		mSpi.write(mTxBuffer, data.length + 1);
	}

	private byte[] readData(byte regAddress, int length) throws IOException {
		final int spiTransferLength = length + 1;

		Arrays.fill(mTxBuffer, 0, length, getSpiReadAddress(regAddress));
		mTxBuffer[length] = (byte) 0x00;

		mSpi.transfer(mTxBuffer, mRxBuffer, spiTransferLength);

		byte[] result = new byte[length];
		System.arraycopy(mRxBuffer, 1, result, 0, length);

		return result;
	}

	public boolean testCrc() throws IOException {
		softReset();

		byte[] data = "Hello, world!".getBytes(Charset.forName("US-ASCII"));

		clearAndStartCrc();
		writeFifo(data);

		byte[] crcResult = readCrcReg();

		sendIdleCommand();

		// CRC-16 (KERMIT)
		byte[] expectedCrcResult = { (byte) 0xD1, (byte) 0x5E };

		return Arrays.equals(crcResult, expectedCrcResult);
	}

	private void clearAndStartCrc() throws IOException {
		// set the starting CRC value to 0
		// read ModeReg
		byte modeValue = readRegister(RegisterAddress.MODE_REG);
		// set bits
		modeValue = BitUtils.clearBits(modeValue, (byte) 0b0000_0011);
		// write ModeReg
		writeRegister(RegisterAddress.MODE_REG, modeValue);

		// send start CRC command
		writeCommand(Command.CALC_CRC);
	}

	private byte[] readCrcReg() throws IOException {
		byte[] result = new byte[2];
		result[0] = readRegister(RegisterAddress.CRC_RESULT_MSB_REG);
		result[1] = readRegister(RegisterAddress.CRC_RESULT_LSB_REG);
		return result;
	}

	private void sendIdleCommand() throws IOException {
		writeCommand(Command.IDLE);
	}

	private void writeFifo(byte[] data) throws IOException {
		writeData(RegisterAddress.FIFO_DATA_REG, data);
	}

	private byte[] readFifo(int length) throws IOException {
		return readData(RegisterAddress.FIFO_DATA_REG, length);
	}

	private void waitForIdle() throws IOException {
		byte command;
		do {
			smallDelayBlocking();
			byte commandRegValue = readRegister(RegisterAddress.COMMAND_REG);
			command = (byte) (commandRegValue & Command._COMMAND_MASK);
		} while (command != Command.IDLE);
	}

	private void waitForModemIdle() throws IOException {
		byte modemState;
		do {
			smallDelayBlocking();
			byte status2RegValue = readRegister(RegisterAddress.STATUS_2_REG);
			modemState = (byte) (status2RegValue & 0b0000_0111);
		} while (modemState != 0);
	}

	private static byte[] newData(int length, byte fillValue) {
		byte[] result = new byte[length];
		Arrays.fill(result, fillValue);
		return result;
	}

	private static void smallDelayBlocking() {
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static byte getSpiReadAddress(byte address) {
		return (byte) (0b1000_0000 | ((address & 0b0011_1111) << 1));
	}

	private static byte getSpiWriteAddress(byte address) {
		//noinspection PointlessBitwiseExpression
		return (byte) (0b0000_0000 | ((address & 0b0011_1111) << 1));
	}

	public interface RegisterAddress {
		byte COMMAND_REG = (byte) 0x01;
		byte COMM_IRQ_REG = (byte) 0x04;
		byte STATUS_2_REG = (byte) 0x08;
		byte FIFO_DATA_REG = (byte) 0x09;
		byte FIFO_LEVEL_REG = (byte) 0x0a;
		byte CONTROL_REG = (byte) 0x0c;
		byte BIT_FRAMING_REG = (byte) 0x0d;
		byte MODE_REG = (byte) 0x11;
		byte TX_CONTROL_REG = (byte) 0x14;
		byte TX_AUTO_REG = (byte) 0x15;
		byte CRC_RESULT_MSB_REG = (byte) 0x21;
		byte CRC_RESULT_LSB_REG = (byte) 0x22;
		byte AUTO_TEST_REG = (byte) 0x36;
	}

	public interface Command {
		byte _COMMAND_MASK = 0b0000_1111;

		byte IDLE = (byte) 0b0000; // no action, cancels current command execution
		byte CONFIGURE = (byte) 0b0001; // Configures the PN512 for FeliCa, MIFARE and NFCIP-1 communication
		byte GENERATE_RANDOM_ID = (byte) 0b0010; // generates a 10-byte random ID number
		byte CALC_CRC = (byte) 0b0011; // activates the CRC coprocessor or performs a self test
		byte TRANSMIT = (byte) 0b0100; // transmits data from the FIFO buffer
		byte NO_CMD_CHANGE = (byte) 0b0111; // no command change, can be used to modify the CommandReg register bits without affecting the command, for example, the PowerDown bit
		byte RECEIVE = (byte) 0b1000; // activates the receiver circuits
		byte TRANSCEIVE = (byte) 0b1100; // transmits data from FIFO buffer to antenna and automatically activates the receiver after transmission
		byte AUTOCOLL = (byte) 0b1101; // Handles FeliCa polling (Card Operation mode only) and MIFARE anticollision (Card Operation mode only)
		byte MF_AUTHENT = (byte) 0b1110; // performs the MIFARE standard authentication as a reader
		byte SOFT_RESET = (byte) 0b1111; // resets the PN512
	}

	public interface Values {
		byte AUTO_TEST_ENABLE_SELF_TEST = (byte) 0x09;
	}

	public interface ConstantData {
		byte[] SELF_TEST_EXPECTED_RESULT = {
				(byte) 0x00, (byte) 0xEB, (byte) 0x66, (byte) 0xBA, (byte) 0x57, (byte) 0xBF,
				(byte) 0x23, (byte) 0x95, (byte) 0xD0, (byte) 0xE3, (byte) 0x0D, (byte) 0x3D,
				(byte) 0x27, (byte) 0x89, (byte) 0x5C, (byte) 0xDE, (byte) 0x9D, (byte) 0x3B,
				(byte) 0xA7, (byte) 0x00, (byte) 0x21, (byte) 0x5B, (byte) 0x89, (byte) 0x82,
				(byte) 0x51, (byte) 0x3A, (byte) 0xEB, (byte) 0x02, (byte) 0x0C, (byte) 0xA5,
				(byte) 0x00, (byte) 0x49, (byte) 0x7C, (byte) 0x84, (byte) 0x4D, (byte) 0xB3,
				(byte) 0xCC, (byte) 0xD2, (byte) 0x1B, (byte) 0x81, (byte) 0x5D, (byte) 0x48,
				(byte) 0x76, (byte) 0xD5, (byte) 0x71, (byte) 0x61, (byte) 0x21, (byte) 0xA9,
				(byte) 0x86, (byte) 0x96, (byte) 0x83, (byte) 0x38, (byte) 0xCF, (byte) 0x9D,
				(byte) 0x5B, (byte) 0x6D, (byte) 0xDC, (byte) 0x15, (byte) 0xBA, (byte) 0x3E,
				(byte) 0x7D, (byte) 0x95, (byte) 0x3B, (byte) 0x2F
		};
	}

}
