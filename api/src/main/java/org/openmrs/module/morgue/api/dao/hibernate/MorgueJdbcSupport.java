/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.morgue.api.dao.hibernate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

import org.hibernate.Session;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.User;
import org.openmrs.api.context.Context;

/**
 * Shared JDBC plumbing for the morgue storage DAOs.
 * <p>
 * The storage unit / compartment / storage assignment tables are deliberately <strong>not</strong>
 * registered as Hibernate entities: adding mapped classes to the reference application's single
 * shared {@code SessionFactory} makes Spring's AOP auto-proxy creator build the whole
 * {@code SessionFactory} eagerly while it is still resolving the {@code MessageSource} bean, which
 * pushed module install / server boot from ~90 seconds to 5-40+ minutes. The DAOs therefore issue
 * hand-written SQL over the Hibernate-managed connection and map rows back onto plain POJOs, the
 * same approach {@code MorgueDao#getPatients} already uses.
 * <p>
 * Because the rows never pass through Hibernate, OpenMRS' {@code AuditableInterceptor} never sees
 * them either, so the audit and void columns have to be stamped here before a write and read back
 * explicitly afterwards.
 */
final class MorgueJdbcSupport {
	
	/** Audit and void columns shared by all three morgue storage tables. */
	static final String[] AUDIT_COLUMNS = { "creator", "date_created", "changed_by", "date_changed", "voided", "voided_by",
	        "date_voided", "voided_reason" };
	
	private MorgueJdbcSupport() {
	}
	
	/**
	 * Builds an aliased select list, so that the columns of several morgue tables can be read from
	 * one joined result set without their names colliding.
	 * 
	 * @param alias the table alias the columns belong to
	 * @param prefix the prefix to give each result set label, may be empty
	 * @param columnGroups the column names to select
	 * @return a comma separated select list
	 */
	static String selectList(String alias, String prefix, String[]... columnGroups) {
		StringBuilder selectList = new StringBuilder();
		for (String[] columns : columnGroups) {
			for (String column : columns) {
				if (selectList.length() > 0) {
					selectList.append(", ");
				}
				selectList.append(alias).append('.').append(column).append(" as ").append(prefix).append(column);
			}
		}
		return selectList.toString();
	}
	
	/**
	 * Holds the raw audit column values of a single row. Foreign keys are kept as ids rather than
	 * resolved immediately so that no further Hibernate query is issued while the {@link ResultSet}
	 * that produced them is still open on the shared connection.
	 */
	static final class AuditRow {
		
		private Integer creatorId;
		
		private Date dateCreated;
		
		private Integer changedById;
		
		private Date dateChanged;
		
		private boolean voided;
		
		private Integer voidedById;
		
		private Date dateVoided;
		
		private String voidReason;
	}
	
	/**
	 * Reads the eight audit and void columns of the current row.
	 * 
	 * @param rs result set positioned on the row to read
	 * @param prefix the label prefix used for this table in the select list
	 * @return the raw audit values of that row
	 * @throws SQLException if a column cannot be read
	 */
	static AuditRow readAudit(ResultSet rs, String prefix) throws SQLException {
		AuditRow audit = new AuditRow();
		audit.creatorId = getInteger(rs, prefix + "creator");
		audit.dateCreated = getDate(rs, prefix + "date_created");
		audit.changedById = getInteger(rs, prefix + "changed_by");
		audit.dateChanged = getDate(rs, prefix + "date_changed");
		audit.voided = rs.getBoolean(prefix + "voided");
		audit.voidedById = getInteger(rs, prefix + "voided_by");
		audit.dateVoided = getDate(rs, prefix + "date_voided");
		audit.voidReason = rs.getString(prefix + "voided_reason");
		return audit;
	}
	
	/**
	 * Copies previously read audit values onto an object, resolving the user foreign keys. Must be
	 * called after the originating {@link ResultSet} has been closed, so that loading the users
	 * does not issue a nested query on the shared connection. The users are loaded straight off the
	 * session rather than through {@code UserService}, so that reading a morgue row still needs no
	 * privilege that the Hibernate many-to-one did not need either.
	 * 
	 * @param session the session the row was read on
	 * @param target the object to populate
	 * @param audit the audit values read by {@link #readAudit(ResultSet, String)}
	 */
	static void applyAudit(Session session, BaseOpenmrsData target, AuditRow audit) {
		target.setCreator(getUser(session, audit.creatorId));
		target.setDateCreated(audit.dateCreated);
		target.setChangedBy(getUser(session, audit.changedById));
		target.setDateChanged(audit.dateChanged);
		target.setVoided(audit.voided);
		target.setVoidedBy(getUser(session, audit.voidedById));
		target.setDateVoided(audit.dateVoided);
		target.setVoidReason(audit.voidReason);
	}
	
	/**
	 * Fills in the creator / dateCreated / voided defaults that the Hibernate interceptors used to
	 * supply, ahead of an insert.
	 * 
	 * @param data the object about to be inserted
	 */
	static void stampForInsert(BaseOpenmrsData data) {
		if (data.getCreator() == null) {
			data.setCreator(Context.getAuthenticatedUser());
		}
		if (data.getDateCreated() == null) {
			data.setDateCreated(new Date());
		}
		if (data.getVoided() == null) {
			data.setVoided(Boolean.FALSE);
		}
		stampVoidInfo(data);
	}
	
	/**
	 * Refreshes changedBy / dateChanged ahead of an update, as the Hibernate interceptors used to.
	 * 
	 * @param data the object about to be updated
	 */
	static void stampForUpdate(BaseOpenmrsData data) {
		data.setChangedBy(Context.getAuthenticatedUser());
		data.setDateChanged(new Date());
		if (data.getVoided() == null) {
			data.setVoided(Boolean.FALSE);
		}
		stampVoidInfo(data);
	}
	
	/**
	 * Binds the eight audit and void values of an object to consecutive statement parameters.
	 * 
	 * @param ps the statement to bind to
	 * @param index the one-based index of the first audit parameter
	 * @param data the object whose audit values should be bound
	 * @return the index immediately after the last bound parameter
	 * @throws SQLException if a parameter cannot be bound
	 */
	static int bindAudit(PreparedStatement ps, int index, BaseOpenmrsData data) throws SQLException {
		int i = index;
		setInteger(ps, i++, getUserId(data.getCreator()));
		ps.setTimestamp(i++, toTimestamp(data.getDateCreated()));
		setInteger(ps, i++, getUserId(data.getChangedBy()));
		ps.setTimestamp(i++, toTimestamp(data.getDateChanged()));
		ps.setBoolean(i++, Boolean.TRUE.equals(data.getVoided()));
		setInteger(ps, i++, getUserId(data.getVoidedBy()));
		ps.setTimestamp(i++, toTimestamp(data.getDateVoided()));
		ps.setString(i++, data.getVoidReason());
		return i;
	}
	
	/**
	 * Reads a nullable integer column, returning {@code null} rather than zero for SQL NULL.
	 * 
	 * @param rs result set positioned on the row to read
	 * @param column the column name
	 * @return the column value, or {@code null}
	 * @throws SQLException if the column cannot be read
	 */
	static Integer getInteger(ResultSet rs, String column) throws SQLException {
		int value = rs.getInt(column);
		return rs.wasNull() ? null : value;
	}
	
	/**
	 * Reads a timestamp column as a plain {@link Date}.
	 * 
	 * @param rs result set positioned on the row to read
	 * @param column the column name
	 * @return the column value, or {@code null}
	 * @throws SQLException if the column cannot be read
	 */
	static Date getDate(ResultSet rs, String column) throws SQLException {
		Timestamp timestamp = rs.getTimestamp(column);
		return timestamp == null ? null : new Date(timestamp.getTime());
	}
	
	/**
	 * Binds a nullable integer parameter.
	 * 
	 * @param ps the statement to bind to
	 * @param index the one-based parameter index
	 * @param value the value to bind, may be {@code null}
	 * @throws SQLException if the parameter cannot be bound
	 */
	static void setInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
		if (value == null) {
			ps.setNull(index, Types.INTEGER);
		} else {
			ps.setInt(index, value);
		}
	}
	
	/**
	 * Converts a {@link Date} to the {@link Timestamp} the JDBC driver expects.
	 * 
	 * @param date the date to convert, may be {@code null}
	 * @return the equivalent timestamp, or {@code null}
	 */
	static Timestamp toTimestamp(Date date) {
		return date == null ? null : new Timestamp(date.getTime());
	}
	
	/**
	 * Reads the generated primary key of an insert.
	 * 
	 * @param ps a statement executed with {@code RETURN_GENERATED_KEYS}
	 * @return the generated key
	 * @throws SQLException if no key was returned
	 */
	static Integer readGeneratedId(PreparedStatement ps) throws SQLException {
		try (ResultSet keys = ps.getGeneratedKeys()) {
			if (!keys.next()) {
				throw new SQLException("No generated key returned for insert");
			}
			return keys.getInt(1);
		}
	}
	
	private static void stampVoidInfo(BaseOpenmrsData data) {
		if (Boolean.TRUE.equals(data.getVoided())) {
			if (data.getVoidedBy() == null) {
				data.setVoidedBy(Context.getAuthenticatedUser());
			}
			if (data.getDateVoided() == null) {
				data.setDateVoided(new Date());
			}
		}
	}
	
	private static Integer getUserId(User user) {
		return user == null ? null : user.getUserId();
	}
	
	private static User getUser(Session session, Integer userId) {
		return userId == null ? null : session.get(User.class, userId);
	}
}
