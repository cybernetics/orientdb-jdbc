/*
 * Copyright 1999-2010 Luca Garulli (l.garulli--at--orientechnologies.com)
 * Copyright 2011 TXT e-solutions SpA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.ORecordLazyList;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;

/**
 * @author Roberto Franchini (CELI Srl - franchini@celi.it)
 * @author Salvatore Piccione (TXT e-solutions SpA - salvo.picci@gmail.com)
 */
@SuppressWarnings("boxing")
public class OrientJdbcResultSetMetaData implements ResultSetMetaData {

	private final static Map<OType, Integer>	typesSqlTypes	= new HashMap<OType, Integer>();

	static {
		typesSqlTypes.put(OType.STRING, Types.VARCHAR);
		typesSqlTypes.put(OType.INTEGER, Types.INTEGER);
		typesSqlTypes.put(OType.FLOAT, Types.FLOAT);
		typesSqlTypes.put(OType.SHORT, Types.SMALLINT);
		typesSqlTypes.put(OType.BOOLEAN, Types.BOOLEAN);
		typesSqlTypes.put(OType.LONG, Types.BIGINT);
		typesSqlTypes.put(OType.DOUBLE, Types.DECIMAL);
		typesSqlTypes.put(OType.DATE, Types.DATE);
		typesSqlTypes.put(OType.DATETIME, Types.TIMESTAMP);
		typesSqlTypes.put(OType.BYTE, Types.TINYINT);
		typesSqlTypes.put(OType.SHORT, Types.SMALLINT);

		// NOT SURE ABOUT THE FOLLOWING MAPPINGS
		typesSqlTypes.put(OType.BINARY, Types.BINARY);
		typesSqlTypes.put(OType.EMBEDDED, Types.JAVA_OBJECT);
		typesSqlTypes.put(OType.EMBEDDEDLIST, Types.JAVA_OBJECT);
		typesSqlTypes.put(OType.EMBEDDEDMAP, Types.JAVA_OBJECT);
		typesSqlTypes.put(OType.EMBEDDEDSET, Types.JAVA_OBJECT);
		typesSqlTypes.put(OType.LINK, Types.JAVA_OBJECT);
		typesSqlTypes.put(OType.LINKLIST, Types.JAVA_OBJECT);
		typesSqlTypes.put(OType.LINKMAP, Types.JAVA_OBJECT);
		typesSqlTypes.put(OType.LINKSET, Types.JAVA_OBJECT);
		typesSqlTypes.put(OType.TRANSIENT, Types.NULL);
	}

	private OrientJdbcResultSet								resultSet;

	public OrientJdbcResultSetMetaData(OrientJdbcResultSet iResultSet) {
		resultSet = iResultSet;
	}

	public int getColumnCount() throws SQLException {
		ODocument currentRecord = this.resultSet.unwrap(ODocument.class);

		if (currentRecord == null)
			return 0;

		return currentRecord.fields();
	}

	public String getCatalogName(int column) throws SQLException {
		// return an empty String according to the method's documentation
		return "";
	}

	public String getColumnClassName(int column) throws SQLException {
		Object value = this.resultSet.getObject(column);
		if (value == null)
			return null;
		return value.getClass().getName();
	}

	public int getColumnDisplaySize(int column) throws SQLException {

		return 0;
	}

	public String getColumnLabel(int column) throws SQLException {
		// returns the name of the column as described in this method's
		// documentation
		return this.getColumnName(column);
	}

	public String getColumnName(int column) throws SQLException {
		ODocument currentRecord = this.resultSet.unwrap(ODocument.class);
		if (currentRecord == null)
			return null;
		else
			return currentRecord.fieldNames()[column - 1];
	}

	public int getColumnType(int column) throws SQLException {
		ODocument currentRecord = this.resultSet.unwrap(ODocument.class);
		final String[] fieldNames = currentRecord.fieldNames();

		if (column > fieldNames.length)
			return Types.NULL;

		String fieldName = fieldNames[column - 1];
		// The OClass is not available so attempting to retrieve the OType from
		// the schema class
		// results in a NullPointerException
		// OClass oclass = currentRecord.getSchemaClass();
		OType otype = currentRecord.fieldType(fieldName);

		if (otype == null) {
			Object value = currentRecord.field(fieldName);
			if (value == null)
				return Types.NULL;
			// Check if the type is a binary record or a collection of binary
			// records
			else if (value instanceof ORecordBytes)
				return Types.BINARY;
			else if (value instanceof ORecordLazyList) {
				ORecordLazyList list = (ORecordLazyList) value;
				// check if all the list items are instances of ORecordBytes
				ListIterator<OIdentifiable> iterator = list.listIterator();
				OIdentifiable listElement;
				boolean stop = false;
				while (iterator.hasNext() && !stop) {
					listElement = iterator.next();
					if (!(listElement instanceof ORecordBytes))
						stop = true;
				}
				if (!stop)
					return Types.BLOB;
			}
			return this.getSQLTypeFromJavaClass(value);
		} else if (otype == OType.EMBEDDED || otype == OType.LINK) {
			Object value = currentRecord.field(fieldName);
			if (value == null)
				return Types.NULL;
			// 1. Check if the type is another record or a collection of records
			if (value instanceof ORecordBytes)
				return Types.BINARY;
			else
				// the default type
				return typesSqlTypes.get(otype);
		} else if (otype == OType.EMBEDDEDLIST || otype == OType.LINKLIST) {
			Object value = currentRecord.field(fieldName);
			if (value == null)
				return Types.NULL;
			if (value instanceof ORecordLazyList) {
				ORecordLazyList list = (ORecordLazyList) value;
				// check if all the list items are instances of ORecordBytes
				ListIterator<OIdentifiable> iterator = list.listIterator();
				OIdentifiable listElement;
				boolean stop = false;
				while (iterator.hasNext() && !stop) {
					listElement = iterator.next();
					if (!(listElement instanceof ORecordBytes))
						stop = true;
				}
				if (stop)
					return typesSqlTypes.get(otype);
				else
					return Types.BLOB;
			} else
				return Types.JAVA_OBJECT;
		} else
			return typesSqlTypes.get(otype);
	}

	private int getSQLTypeFromJavaClass(Object value) {
		// START inferencing the OType from the Java class
		if (value instanceof Boolean)
			return typesSqlTypes.get(OType.BOOLEAN);
		else if (value instanceof Byte)
			return typesSqlTypes.get(OType.BYTE);
		else if (value instanceof Date)
			return typesSqlTypes.get(OType.DATETIME);
		else if (value instanceof Double)
			return typesSqlTypes.get(OType.DOUBLE);
		else if (value instanceof Float)
			return typesSqlTypes.get(OType.FLOAT);
		else if (value instanceof Integer)
			return typesSqlTypes.get(OType.INTEGER);
		else if (value instanceof Long)
			return typesSqlTypes.get(OType.LONG);
		else if (value instanceof Short)
			return typesSqlTypes.get(OType.SHORT);
		else if (value instanceof String)
			return typesSqlTypes.get(OType.STRING);
		else
			return Types.JAVA_OBJECT;
		// STOP inferencing the OType from the Java class
	}

	public String getColumnTypeName(int column) throws SQLException {
		ODocument currentRecord = this.resultSet.unwrap(ODocument.class);
		if (currentRecord == null)
			return null;
		else {
			OType columnType = currentRecord.fieldType(currentRecord.fieldNames()[column - 1]);
			if (columnType == null)
				return null;
			return columnType.toString();
		}
	}

	public int getPrecision(int column) throws SQLException {
		return 0;
	}

	public int getScale(int column) throws SQLException {
		return 0;
	}

	public String getSchemaName(int column) throws SQLException {
		ODocument currentRecord = this.resultSet.unwrap(ODocument.class);
		if (currentRecord == null)
			return null;
		else
			return currentRecord.getDatabase().getName();
	}

	public String getTableName(int column) throws SQLException {
		ODocument currentRecord = this.resultSet.unwrap(ODocument.class);
		if (currentRecord == null)
			return null;
		else
			// TODO TEST getStreamableName
			return currentRecord.getSchemaClass().getName();
	}

	public boolean isAutoIncrement(int column) throws SQLException {

		return false;
	}

	public boolean isCaseSensitive(int column) throws SQLException {

		return false;
	}

	public boolean isCurrency(int column) throws SQLException {

		return false;
	}

	public boolean isDefinitelyWritable(int column) throws SQLException {

		return false;
	}

	public int isNullable(int column) throws SQLException {
		return columnNullableUnknown;
	}

	public boolean isReadOnly(int column) throws SQLException {
		// at the moment the updateXXX() methods are not yet implemented
		return true;
	}

	public boolean isSearchable(int column) throws SQLException {

		return true;
	}

	public boolean isSigned(int column) throws SQLException {
		ODocument currentRecord = this.resultSet.unwrap(ODocument.class);
		if (currentRecord == null)
			return false;
		else
			return this.isANumericColumn(currentRecord.fieldType(currentRecord.fieldNames()[column - 1]));
	}

	public boolean isWritable(int column) throws SQLException {

		return false;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {

		return false;
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {

		return null;
	}

	private boolean isANumericColumn(OType type) {
		return type == OType.BYTE || type == OType.DOUBLE || type == OType.FLOAT || type == OType.INTEGER || type == OType.LONG
				|| type == OType.SHORT;
	}

	public static Integer getSqlType(final OType iType) {
		return typesSqlTypes.get(iType);

	}
}
