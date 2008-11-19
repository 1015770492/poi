/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hssf.record;

import java.util.Arrays;

import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.StringUtil;

/**
 * Title: Write Access Record (0x005C)<p/>
 * 
 * Description: Stores the username of that who owns the spreadsheet generator (on unix the user's 
 * login, on Windoze its the name you typed when you installed the thing)
 * <p/>
 * REFERENCE: PG 424 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)
 * <p/>
 * 
 * @author Andrew C. Oliver (acoliver at apache dot org)
 */
public final class WriteAccessRecord extends StandardRecord {
	public final static short sid = 0x005C;

	private static final byte PAD_CHAR = (byte) ' ';
	private static final int DATA_SIZE = 112;
	private String field_1_username;
	/** this record is always padded to a constant length */
	private byte[] padding;

	public WriteAccessRecord() {
		setUsername("");
		padding = new byte[DATA_SIZE - 3];
	}

	public WriteAccessRecord(RecordInputStream in) {
		if (in.remaining() > DATA_SIZE) {
			throw new RecordFormatException("Expected data size (" + DATA_SIZE + ") but got ("
					+ in.remaining() + ")");
		}
		// The string is always 112 characters (padded with spaces), therefore
		// this record can not be continued.

		int nChars = in.readUShort();
		int is16BitFlag = in.readUByte();
		int expectedPadSize = DATA_SIZE - 3;
		if ((is16BitFlag & 0x01) == 0x00) {
			field_1_username = StringUtil.readCompressedUnicode(in, nChars);
			expectedPadSize -= nChars;
		} else {
			field_1_username = StringUtil.readUnicodeLE(in, nChars);
			expectedPadSize -= nChars * 2;
		}
		padding = new byte[expectedPadSize];
		int padSize = in.remaining();
		in.readFully(padding, 0, padSize);
		if (padSize < expectedPadSize) {
			// this occurs in a couple of test examples: "42564.xls",
			// "bug_42794.xls"
			Arrays.fill(padding, padSize, expectedPadSize, PAD_CHAR);
		}
	}

	/**
	 * set the username for the user that created the report. HSSF uses the
	 * logged in user.
	 * 
	 * @param username of the user who is logged in (probably "tomcat" or "apache")
	 */
	public void setUsername(String username) {
		boolean is16bit = StringUtil.hasMultibyte(username);
		int encodedByteCount = 3 + username.length() * (is16bit ? 2 : 1);
		int paddingSize = DATA_SIZE - encodedByteCount;
		if (paddingSize < 0) {
			throw new IllegalArgumentException("Name is too long: " + username);
		}
		padding = new byte[paddingSize];
		Arrays.fill(padding, PAD_CHAR);

		field_1_username = username;
	}

	/**
	 * get the username for the user that created the report. HSSF uses the
	 * logged in user. On natively created M$ Excel sheet this would be the name
	 * you typed in when you installed it in most cases.
	 * 
	 * @return username of the user who is logged in (probably "tomcat" or "apache")
	 */
	public String getUsername() {
		return field_1_username;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("[WRITEACCESS]\n");
		buffer.append("    .name            = ").append(field_1_username.toString()).append("\n");
		buffer.append("[/WRITEACCESS]\n");
		return buffer.toString();
	}

	public void serialize(LittleEndianOutput out) {
		String username = getUsername();
		boolean is16bit = StringUtil.hasMultibyte(username);

		out.writeShort(username.length());
		out.writeByte(is16bit ? 0x01 : 0x00);
		if (is16bit) {
			StringUtil.putUnicodeLE(username, out);
		} else {
			StringUtil.putCompressedUnicode(username, out);
		}
		out.write(padding);
	}

	protected int getDataSize() {
		return DATA_SIZE;
	}

	public short getSid() {
		return sid;
	}
}
