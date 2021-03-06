/*
 * This file is part of the OdinMS Maple Story Server Copyright (C) 2008 ~ 2010
 * Patrick Huy <patrick.huy@frz.cc> Matthias Butz <matze@odinms.de> Jan
 * Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation. You may not use, modify or distribute this
 * program under any other version of the GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.tools;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class LogUtil {

	// Logging output file
	public static final String Acc_Stuck = "Log_AccountStuck.rtf", Login_Error = "Log_Login_Error.rtf", Timer_Log = "Log_Timer_Except.rtf",
		MapTimer_Log = "Log_MapTimer_Except.rtf", GMCommand_Log = "Log_GMCommand.rtf", IP_Log = "Log_AccountIP.rtf", Horntail_Log = "Log_Horntail.rtf",
		Pinkbean_Log = "Log_Pinkbean.rtf";
	// End
	
	public static void log(final String file, final String msg) {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file, true);
			out.write(msg.getBytes());
			out.write("\n------------------------\n".getBytes());
		} catch (final IOException ess) {
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (final IOException ignore) {
			}
		}
	}

	public static void outputFileError(final String file, final Throwable t) {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file, true);
			out.write(getString(t).getBytes());
			out.write("\n------------------------\n".getBytes());
		} catch (final IOException ess) {
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (final IOException ignore) {
			}
		}
	}

	public static String CurrentReadable_Time() {
		// Sorry, DateFormat is not thread-safe, so we can't extract it as a static field :(
		final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		final Date currentTime = Calendar.getInstance().getTime();
		return sdf.format(currentTime);
	}

	public static String getString(final Throwable e) {
		String retValue = null;
		StringWriter sw = null;
		PrintWriter pw = null;
		try {
			sw = new StringWriter();
			pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			retValue = sw.toString();
		} finally {
			try {
				if (pw != null) {
					pw.close();
				}
				if (sw != null) {
					sw.close();
				}
			} catch (final IOException ignore) {
			}
		}
		return retValue;
	}
}
