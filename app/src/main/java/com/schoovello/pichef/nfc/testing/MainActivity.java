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

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

public class MainActivity extends Activity {

	private HandlerThread mThread;
	private Handler mHandler;

	private Pn512NfcDevice mNfcDevice;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mThread = new HandlerThread("SPIThread");
		mThread.start();
		mHandler = new Handler(mThread.getLooper());

		try {
			PeripheralManager peripheralManager = PeripheralManager.getInstance();
			SpiDevice spi = peripheralManager.openSpiDevice("SPI0.0");
			mNfcDevice = new Pn512NfcDevice(spi);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		mThread.quit();

		mNfcDevice.close();
	}

	@Override
	protected void onStart() {
		super.onStart();

		mHandler.post(mSelfTestRunnable);
	}

	@Override
	protected void onStop() {
		super.onStop();

		mHandler.removeCallbacksAndMessages(null);
	}

	private Runnable mSelfTestRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				boolean valid = mNfcDevice.selfTest();

				String message = valid ? "SUCCESS" : "FAILURE";
				Log.d("SELF_TEST", "Self Test result: " + message);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	};


}
