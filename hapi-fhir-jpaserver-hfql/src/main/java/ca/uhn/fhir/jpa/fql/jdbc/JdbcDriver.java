/*-
 * #%L
 * HAPI FHIR JPA Server - HFQL Driver
 * %%
 * Copyright (C) 2014 - 2023 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.jpa.fql.jdbc;

import java.io.PrintStream;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class JdbcDriver implements Driver {
	private static final JdbcDriver INSTANCE = new JdbcDriver();
	public static final String URL_PREFIX = "jdbc:hapifhir:";
	private static boolean ourRegistered;

	static {
		load();
	}

	@Override
	public Connection connect(String theUrl, Properties theProperties) throws SQLException {
		String serverUrl = theUrl.substring(URL_PREFIX.length());

		JdbcConnection connection = new JdbcConnection(serverUrl);
		connection.setUsername(theProperties.getProperty("user", null));
		connection.setPassword(theProperties.getProperty("password", null));
		return connection;
	}

	@Override
	public boolean acceptsURL(String theUrl) {
		return theUrl.startsWith(URL_PREFIX);
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return new DriverPropertyInfo[0];
	}

	@Override
	public int getMajorVersion() {
		return 1;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public boolean jdbcCompliant() {
		return false;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}

	public static synchronized Driver load() {
		try {
			if (!ourRegistered) {
				ourRegistered = true;
				DriverManager.registerDriver(INSTANCE);
			}
		} catch (SQLException e) {
			logException(e);
		}

		return INSTANCE;
	}

	private static void logException(SQLException e) {
		PrintStream out = System.out;
		e.printStackTrace(out);
	}

	public static synchronized void unload() {
		try {
			if (ourRegistered) {
				ourRegistered = false;
				DriverManager.deregisterDriver(INSTANCE);
			}
		} catch (SQLException e) {
			logException(e);
		}
	}
}