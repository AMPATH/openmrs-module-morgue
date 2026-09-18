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
import org.openmrs.api.db.DAOException;
import org.openmrs.module.morgue.MorgueCompartment;
import org.openmrs.module.morgue.MorgueStorageUnit;
import org.openmrs.module.morgue.api.dao.MorgueCompartmentDao;

/**
 * JDBC backed implementation of {@link MorgueCompartmentDao}.
 * <p>
 * {@link MorgueCompartment} is intentionally not a Hibernate entity - see {@link MorgueJdbcSupport}
 * for why - so every statement here is hand written SQL run through {@code doWork} /
 * {@code doReturningWork} on the current Hibernate session, keeping the work on the same connection
 * and transaction as the rest of the request.
 * <p>
 * The owning storage unit used to be a Hibernate many-to-one; it is now fetched in the same
 * statement by joining {@code morgue_storage_unit}, so callers still get a populated
 * {@link MorgueStorageUnit} back.
 */
public class HibernateMorgueCompartmentDao implements MorgueCompartmentDao {
	
	static final String TABLE = "morgue_compartment";
	
	static final String ALIAS = "c";
	
	static final String PREFIX = "c_";
	
	private static final String[] OWN_COLUMNS = { "compartment_id", "uuid", "display", "storage_unit_id" };
	
	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/**
	 * @see MorgueCompartmentDao#getCompartmentByUuid(String)
	 */
	@Override
	public MorgueCompartment getCompartmentByUuid(String uuid) {
		if (uuid == null) {
			return null;
		}
		
		final String sql = baseSelect() + " where " + ALIAS + ".uuid = ?";
		
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
	 * @see MorgueCompartmentDao#getCompartmentsByStorageUnit(MorgueStorageUnit)
	 */
	@Override
	public List<MorgueCompartment> getCompartmentsByStorageUnit(MorgueStorageUnit storageUnit) {
		if (storageUnit == null || storageUnit.getStorageUnitId() == null) {
			return new ArrayList<>();
		}
		
		final String sql = baseSelect() + " where " + ALIAS + ".storage_unit_id = ? and " + ALIAS
		        + ".voided = false order by " + ALIAS + ".compartment_id";
		final Integer storageUnitId = storageUnit.getStorageUnitId();
		
		Session session = sessionFactory.getCurrentSession();
		List<Row> rows = session.doReturningWork(connection -> {
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setInt(1, storageUnitId);
				return readRows(ps);
			}
		});
		
		List<MorgueCompartment> compartments = new ArrayList<>(rows.size());
		for (Row row : rows) {
			compartments.add(resolve(session, row));
		}
		return compartments;
	}
	
	/**
	 * Inserts the compartment when it has no id yet, otherwise updates the existing row. This
	 * mirrors the {@code saveOrUpdate} semantics the DAO had while the class was Hibernate mapped.
	 * 
	 * @see MorgueCompartmentDao#saveCompartment(MorgueCompartment)
	 */
	@Override
	public MorgueCompartment saveCompartment(MorgueCompartment compartment) {
		if (compartment == null) {
			throw new DAOException("Compartment is required");
		}
		
		if (compartment.getCompartmentId() == null) {
			insert(compartment);
		} else {
			update(compartment);
		}
		return compartment;
	}
	
	/**
	 * @see MorgueCompartmentDao#deleteCompartment(MorgueCompartment)
	 */
	@Override
	public void deleteCompartment(MorgueCompartment compartment) {
		if (compartment == null || compartment.getCompartmentId() == null) {
			throw new DAOException("Cannot delete a morgue compartment that has not been saved");
		}
		
		final String sql = "delete from " + TABLE + " where compartment_id = ?";
		final Integer id = compartment.getCompartmentId();
		
		sessionFactory.getCurrentSession().doWork(connection -> {
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setInt(1, id);
				ps.executeUpdate();
			}
		});
	}
	
	private void insert(MorgueCompartment compartment) {
		MorgueJdbcSupport.stampForInsert(compartment);
		
		final String sql = "insert into " + TABLE + " (uuid, display, storage_unit_id, creator, date_created, "
		        + "changed_by, date_changed, voided, voided_by, date_voided, voided_reason) "
		        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		Integer generatedId = sessionFactory.getCurrentSession().doReturningWork(connection -> {
			try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				int i = 1;
				ps.setString(i++, compartment.getUuid());
				ps.setString(i++, compartment.getDisplay());
				MorgueJdbcSupport.setInteger(ps, i++, storageUnitId(compartment));
				MorgueJdbcSupport.bindAudit(ps, i, compartment);
				ps.executeUpdate();
				return MorgueJdbcSupport.readGeneratedId(ps);
			}
		});
		
		compartment.setCompartmentId(generatedId);
	}
	
	private void update(MorgueCompartment compartment) {
		MorgueJdbcSupport.stampForUpdate(compartment);
		
		final String sql = "update " + TABLE + " set uuid = ?, display = ?, storage_unit_id = ?, creator = ?, "
		        + "date_created = ?, changed_by = ?, date_changed = ?, voided = ?, voided_by = ?, date_voided = ?, "
		        + "voided_reason = ? where compartment_id = ?";
		
		sessionFactory.getCurrentSession().doWork(connection -> {
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				int i = 1;
				ps.setString(i++, compartment.getUuid());
				ps.setString(i++, compartment.getDisplay());
				MorgueJdbcSupport.setInteger(ps, i++, storageUnitId(compartment));
				i = MorgueJdbcSupport.bindAudit(ps, i, compartment);
				ps.setInt(i, compartment.getCompartmentId());
				ps.executeUpdate();
			}
		});
	}
	
	private static Integer storageUnitId(MorgueCompartment compartment) {
		return compartment.getStorageUnit() == null ? null : compartment.getStorageUnit().getStorageUnitId();
	}
	
	/**
	 * Builds the aliased select list for this table. Also used by the DAOs that join to it.
	 * 
	 * @return a comma separated select list labelled with {@link #PREFIX}
	 */
	static String selectList() {
		return MorgueJdbcSupport.selectList(ALIAS, PREFIX, OWN_COLUMNS, MorgueJdbcSupport.AUDIT_COLUMNS);
	}
	
	/**
	 * The select and from clauses every read here shares: the compartment's own columns plus the
	 * joined storage unit's.
	 * 
	 * @return a select statement up to and including the joins
	 */
	private static String baseSelect() {
		return "select " + selectList() + ", " + HibernateMorgueStorageUnitDao.selectList() + " from " + TABLE + " " + ALIAS
		        + " " + storageUnitJoin();
	}
	
	/**
	 * The join that brings in the compartment's owning storage unit.
	 * 
	 * @return an inner join clause onto {@code morgue_storage_unit}
	 */
	static String storageUnitJoin() {
		return "inner join " + HibernateMorgueStorageUnitDao.TABLE + " " + HibernateMorgueStorageUnitDao.ALIAS + " on "
		        + HibernateMorgueStorageUnitDao.ALIAS + ".storage_unit_id = " + ALIAS + ".storage_unit_id";
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
	 * Reads the compartment columns, and the joined storage unit columns, of the current row.
	 * 
	 * @param rs result set positioned on the row to read
	 * @return the raw values of that row
	 * @throws SQLException if a column cannot be read
	 */
	static Row readRow(ResultSet rs) throws SQLException {
		Row row = new Row();
		row.compartment.setCompartmentId(MorgueJdbcSupport.getInteger(rs, PREFIX + "compartment_id"));
		row.compartment.setUuid(rs.getString(PREFIX + "uuid"));
		row.compartment.setDisplay(rs.getString(PREFIX + "display"));
		row.audit = MorgueJdbcSupport.readAudit(rs, PREFIX);
		row.storageUnitRow = HibernateMorgueStorageUnitDao.readRow(rs);
		return row;
	}
	
	/**
	 * Turns a raw row into a compartment, resolving its storage unit and audit users off the
	 * session. Must be called after the originating result set has been closed.
	 * 
	 * @param session the session the row was read on
	 * @param row the raw row to resolve
	 * @return the populated compartment
	 */
	static MorgueCompartment resolve(Session session, Row row) {
		row.compartment.setStorageUnit(HibernateMorgueStorageUnitDao.resolve(session, row.storageUnitRow));
		MorgueJdbcSupport.applyAudit(session, row.compartment, row.audit);
		return row.compartment;
	}
	
	/**
	 * A row read straight off the {@link ResultSet}. Foreign keys stay as ids until the result set
	 * is closed, so that resolving them never issues a nested query on the shared connection.
	 */
	static final class Row {
		
		private final MorgueCompartment compartment = new MorgueCompartment();
		
		private MorgueJdbcSupport.AuditRow audit;
		
		private HibernateMorgueStorageUnitDao.Row storageUnitRow;
	}
}
