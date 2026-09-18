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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Location;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.morgue.MorgueStorageUnit;
import org.openmrs.module.morgue.api.dao.MorgueStorageUnitDao;

/**
 * JDBC backed implementation of {@link MorgueStorageUnitDao}.
 * <p>
 * {@link MorgueStorageUnit} is intentionally not a Hibernate entity - see {@link MorgueJdbcSupport}
 * for why - so every statement here is hand written SQL run through {@code doWork} /
 * {@code doReturningWork} on the current Hibernate session. That keeps the reads and writes on the
 * same connection and inside the same transaction as the rest of the request while adding nothing
 * to the reference application's shared {@code SessionFactory}.
 */
public class HibernateMorgueStorageUnitDao implements MorgueStorageUnitDao {
	
	static final String TABLE = "morgue_storage_unit";
	
	static final String ALIAS = "su";
	
	static final String PREFIX = "su_";
	
	private static final String[] OWN_COLUMNS = { "storage_unit_id", "uuid", "display", "location_id" };
	
	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/**
	 * @see MorgueStorageUnitDao#getStorageUnitByUuid(String)
	 */
	@Override
	public MorgueStorageUnit getStorageUnitByUuid(String uuid) {
		if (uuid == null) {
			return null;
		}

		final String sql = "select " + selectList() + " from " + TABLE + " " + ALIAS + " where " + ALIAS + ".uuid = ?";

		Session session = sessionFactory.getCurrentSession();
		List<Row> rows = session.doReturningWork(connection -> {
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, uuid);
				return readRows(ps);
			}
		});

		return rows.isEmpty() ? null : resolve(session, rows.get(0));
	}
	
	/**
	 * @see MorgueStorageUnitDao#getAllStorageUnits(Boolean, Location)
	 */
	@Override
	public List<MorgueStorageUnit> getAllStorageUnits(Boolean includeVoided, Location location) {
		StringBuilder sql = new StringBuilder("select ").append(selectList()).append(" from ").append(TABLE).append(' ')
				.append(ALIAS);
		List<String> clauses = new ArrayList<>();

		if (includeVoided == null || !includeVoided) {
			clauses.add(ALIAS + ".voided = false");
		}
		if (location != null) {
			clauses.add(ALIAS + ".location_id = ?");
		}
		if (!clauses.isEmpty()) {
			sql.append(" where ").append(String.join(" and ", clauses));
		}
		sql.append(" order by ").append(ALIAS).append(".storage_unit_id");

		final String statement = sql.toString();
		final Integer locationId = location == null ? null : location.getLocationId();

		Session session = sessionFactory.getCurrentSession();
		List<Row> rows = session.doReturningWork(connection -> {
			try (PreparedStatement ps = connection.prepareStatement(statement)) {
				if (locationId != null) {
					ps.setInt(1, locationId);
				}
				return readRows(ps);
			}
		});

		List<MorgueStorageUnit> storageUnits = new ArrayList<>(rows.size());
		for (Row row : rows) {
			storageUnits.add(resolve(session, row));
		}
		return storageUnits;
	}
	
	/**
	 * Inserts the storage unit when it has no id yet, otherwise updates the existing row. This
	 * mirrors the {@code saveOrUpdate} semantics the DAO had while the class was Hibernate mapped.
	 * 
	 * @see MorgueStorageUnitDao#saveStorageUnit(MorgueStorageUnit)
	 */
	@Override
	public MorgueStorageUnit saveStorageUnit(MorgueStorageUnit storageUnit) {
		if (storageUnit == null) {
			throw new DAOException("Storage unit is required");
		}
		
		if (storageUnit.getStorageUnitId() == null) {
			insert(storageUnit);
		} else {
			update(storageUnit);
		}
		return storageUnit;
	}
	
	/**
	 * @see MorgueStorageUnitDao#deleteStorageUnit(MorgueStorageUnit)
	 */
	@Override
	public void deleteStorageUnit(MorgueStorageUnit storageUnit) {
		if (storageUnit == null || storageUnit.getStorageUnitId() == null) {
			throw new DAOException("Cannot delete a morgue storage unit that has not been saved");
		}

		final String sql = "delete from " + TABLE + " where storage_unit_id = ?";
		final Integer id = storageUnit.getStorageUnitId();

		sessionFactory.getCurrentSession().doWork(connection -> {
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setInt(1, id);
				ps.executeUpdate();
			}
		});
	}
	
	private void insert(MorgueStorageUnit storageUnit) {
		MorgueJdbcSupport.stampForInsert(storageUnit);

		final String sql = "insert into " + TABLE + " (uuid, display, location_id, creator, date_created, changed_by, "
				+ "date_changed, voided, voided_by, date_voided, voided_reason) "
				+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		Integer generatedId = sessionFactory.getCurrentSession().doReturningWork(connection -> {
			try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				int i = 1;
				ps.setString(i++, storageUnit.getUuid());
				ps.setString(i++, storageUnit.getDisplay());
				MorgueJdbcSupport.setInteger(ps, i++, locationId(storageUnit));
				MorgueJdbcSupport.bindAudit(ps, i, storageUnit);
				ps.executeUpdate();
				return MorgueJdbcSupport.readGeneratedId(ps);
			}
		});

		storageUnit.setStorageUnitId(generatedId);
	}
	
	private void update(MorgueStorageUnit storageUnit) {
		MorgueJdbcSupport.stampForUpdate(storageUnit);

		final String sql = "update " + TABLE + " set uuid = ?, display = ?, location_id = ?, creator = ?, "
				+ "date_created = ?, changed_by = ?, date_changed = ?, voided = ?, voided_by = ?, date_voided = ?, "
				+ "voided_reason = ? where storage_unit_id = ?";

		sessionFactory.getCurrentSession().doWork(connection -> {
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				int i = 1;
				ps.setString(i++, storageUnit.getUuid());
				ps.setString(i++, storageUnit.getDisplay());
				MorgueJdbcSupport.setInteger(ps, i++, locationId(storageUnit));
				i = MorgueJdbcSupport.bindAudit(ps, i, storageUnit);
				ps.setInt(i, storageUnit.getStorageUnitId());
				ps.executeUpdate();
			}
		});
	}
	
	private static Integer locationId(MorgueStorageUnit storageUnit) {
		return storageUnit.getLocation() == null ? null : storageUnit.getLocation().getLocationId();
	}
	
	/**
	 * Builds the aliased select list for this table. Also used by the DAOs that join to it.
	 * 
	 * @return a comma separated select list labelled with {@link #PREFIX}
	 */
	static String selectList() {
		return MorgueJdbcSupport.selectList(ALIAS, PREFIX, OWN_COLUMNS, MorgueJdbcSupport.AUDIT_COLUMNS);
	}
	
	private static List<Row> readRows(PreparedStatement ps) throws SQLException {
		List<Row> rows = new ArrayList<>();
		try (ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				rows.add(readRow(rs));
			}
		}
		return rows;
	}
	
	/**
	 * Reads the storage unit columns of the current row, whether they came from a plain select on
	 * this table or from a join started by another DAO.
	 * 
	 * @param rs result set positioned on the row to read
	 * @return the raw values of that row
	 * @throws SQLException if a column cannot be read
	 */
	static Row readRow(ResultSet rs) throws SQLException {
		Row row = new Row();
		row.storageUnit.setStorageUnitId(MorgueJdbcSupport.getInteger(rs, PREFIX + "storage_unit_id"));
		row.storageUnit.setUuid(rs.getString(PREFIX + "uuid"));
		row.storageUnit.setDisplay(rs.getString(PREFIX + "display"));
		row.locationId = MorgueJdbcSupport.getInteger(rs, PREFIX + "location_id");
		row.audit = MorgueJdbcSupport.readAudit(rs, PREFIX);
		return row;
	}
	
	/**
	 * Turns a raw row into a storage unit, resolving its location and audit users off the session.
	 * Must be called after the originating result set has been closed.
	 * 
	 * @param session the session the row was read on
	 * @param row the raw row to resolve
	 * @return the populated storage unit
	 */
	static MorgueStorageUnit resolve(Session session, Row row) {
		if (row.locationId != null) {
			row.storageUnit.setLocation(session.get(Location.class, row.locationId));
		}
		MorgueJdbcSupport.applyAudit(session, row.storageUnit, row.audit);
		return row.storageUnit;
	}
	
	/**
	 * A row read straight off the {@link ResultSet}. Foreign keys stay as ids until the result set
	 * is closed, so that resolving them never issues a nested query on the shared connection.
	 */
	static final class Row {
		
		private final MorgueStorageUnit storageUnit = new MorgueStorageUnit();
		
		private Integer locationId;
		
		private MorgueJdbcSupport.AuditRow audit;
	}
}
