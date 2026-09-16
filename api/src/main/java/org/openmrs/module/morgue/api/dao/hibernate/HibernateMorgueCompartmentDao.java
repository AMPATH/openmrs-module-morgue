package org.openmrs.module.morgue.api.dao.hibernate;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.openmrs.module.morgue.MorgueCompartment;
import org.openmrs.module.morgue.MorgueStorageUnit;
import org.openmrs.module.morgue.api.dao.MorgueCompartmentDao;

public class HibernateMorgueCompartmentDao implements MorgueCompartmentDao {
	
	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public MorgueCompartment getCompartmentByUuid(String uuid) {
		Query<MorgueCompartment> query = sessionFactory.getCurrentSession().createQuery(
		    "from MorgueCompartment where uuid = :uuid", MorgueCompartment.class);
		query.setParameter("uuid", uuid);
		return query.uniqueResult();
	}
	
	@Override
	public List<MorgueCompartment> getCompartmentsByStorageUnit(MorgueStorageUnit storageUnit) {
		Query<MorgueCompartment> query = sessionFactory.getCurrentSession().createQuery(
		    "from MorgueCompartment where storageUnit = :storageUnit and voided = false", MorgueCompartment.class);
		query.setParameter("storageUnit", storageUnit);
		return query.list();
	}
	
	@Override
	public MorgueCompartment saveCompartment(MorgueCompartment compartment) {
		sessionFactory.getCurrentSession().saveOrUpdate(compartment);
		return compartment;
	}
	
	@Override
	public void deleteCompartment(MorgueCompartment compartment) {
		sessionFactory.getCurrentSession().delete(compartment);
	}
	
}
