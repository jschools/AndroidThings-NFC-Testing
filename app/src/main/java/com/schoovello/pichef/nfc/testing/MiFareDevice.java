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

public class MiFareDevice {

	public interface Command {
		byte REQA = (byte) (0x26 << 1); // 7-bit
		byte WUPA = (byte) (0x52 << 1); // 7-bit
		byte READ = (byte) 0x30;
	}

}
