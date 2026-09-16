package org.openmrs.module.morgue.api.dao.hibernate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.Query;
import org.hibernate.SessionFactory;
import org.openmrs.Location;
import org.openmrs.module.morgue.MorgueStorageUnit;
import org.openmrs.module.morgue.api.dao.MorgueStorageUnitDao;

public class HibernateMorgueStorageUnitDao implements MorgueStorageUnitDao {
	
	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public MorgueStorageUnit getStorageUnitByUuid(String uuid) {
		Query<MorgueStorageUnit> query = sessionFactory.getCurrentSession().createQuery(
		    "from MorgueStorageUnit where uuid = :uuid", MorgueStorageUnit.class);
		query.setParameter("uuid", uuid);
		return query.uniqueResult();
	}
	
	@Override
    public List<MorgueStorageUnit> getAllStorageUnits(Boolean includeVoided, Location location) {
        StringBuilder hql = new StringBuilder("from MorgueStorageUnit");
        List<String> clauses = new ArrayList<>();

        if (includeVoided == null || !includeVoided) {
            clauses.add("voided = false");
        }

        if (location != null) {
            clauses.add("location = :location");
        }

        if (!clauses.isEmpty()) {
            hql.append(" where ").append(String.join(" and ", clauses));
        }

        Query<MorgueStorageUnit> query = sessionFactory.getCurrentSession()
                .createQuery(hql.toString(), MorgueStorageUnit.class);

        if (location != null) {
            query.setParameter("location", location);
        }

        return query.list();
    }
	
	@Override
	public MorgueStorageUnit saveStorageUnit(MorgueStorageUnit storageUnit) {
		sessionFactory.getCurrentSession().saveOrUpdate(storageUnit);
		return storageUnit;
	}
	
	@Override
	public void deleteStorageUnit(MorgueStorageUnit storageUnit) {
		sessionFactory.getCurrentSession().delete(storageUnit);
	}
	
}
