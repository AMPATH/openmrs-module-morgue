package org.openmrs.module.morgue.api.dao.hibernate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.module.morgue.MorgueCompartment;
import org.openmrs.module.morgue.MorgueStorageAssignment;
import org.openmrs.module.morgue.api.dao.MorgueStorageAssignmentDao;

public class HibernateMorgueStorageAssignmentDao implements MorgueStorageAssignmentDao {
	
	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@Override
	public MorgueStorageAssignment getActiveAssignmentForPatient(Patient patient) {
		Query<MorgueStorageAssignment> query = sessionFactory.getCurrentSession().createQuery(
		    "from MorgueStorageAssignment where patient = :patient " + "and dateDischarged is null and voided = false",
		    MorgueStorageAssignment.class);
		query.setParameter("patient", patient);
		return query.uniqueResult();
	}
	
	@Override
	public List<MorgueStorageAssignment> getAssignmentsForPatient(Patient patient) {
		Query<MorgueStorageAssignment> query = sessionFactory.getCurrentSession().createQuery(
		    "from MorgueStorageAssignment where patient = :patient and voided = false", MorgueStorageAssignment.class);
		query.setParameter("patient", patient);
		return query.list();
	}
	
	@Override
	public MorgueStorageAssignment saveAssignment(MorgueStorageAssignment assignment) {
		sessionFactory.getCurrentSession().saveOrUpdate(assignment);
		return assignment;
	}
	
	@Override
	public void deleteAssignment(MorgueStorageAssignment assignment) {
		sessionFactory.getCurrentSession().delete(assignment);
	}
	
	@Override
    public List<MorgueStorageAssignment> getAssignmentsForLocation(Location location, Boolean includeVoided,
            Date createdOnOrAfter, Date admittedOnOrAfter, Date admittedOnOrBefore) {

        StringBuilder hql = new StringBuilder("from MorgueStorageAssignment a");
        List<String> clauses = new ArrayList<>();

        if (location != null) {
            clauses.add("a.compartment.storageUnit.location = :location");
        }

        if (includeVoided == null || !includeVoided) {
            clauses.add("a.voided = false");
        }

        if (createdOnOrAfter != null) {
            clauses.add("a.dateCreated >= :createdOnOrAfter");
        }

        if (admittedOnOrAfter != null) {
            clauses.add("a.dateAdmitted >= :admittedOnOrAfter");
        }

        if (admittedOnOrBefore != null) {
            clauses.add("a.dateAdmitted <= :admittedOnOrBefore");
        }

        if (!clauses.isEmpty()) {
            hql.append(" where ").append(String.join(" and ", clauses));
        }

        Query<MorgueStorageAssignment> query = sessionFactory.getCurrentSession()
                .createQuery(hql.toString(), MorgueStorageAssignment.class);

        if (location != null) {
            query.setParameter("location", location);
        }
        if (createdOnOrAfter != null) {
            query.setParameter("createdOnOrAfter", createdOnOrAfter);
        }
        if (admittedOnOrAfter != null) {
            query.setParameter("admittedOnOrAfter", admittedOnOrAfter);
        }
        if (admittedOnOrBefore != null) {
            query.setParameter("admittedOnOrBefore", admittedOnOrBefore);
        }

        return query.list();
    }
	
	@Override
	public List<MorgueStorageAssignment> getAssignmentsForCompartment(MorgueCompartment compartment) {
		Query<MorgueStorageAssignment> query = sessionFactory.getCurrentSession().createQuery(
		    "from MorgueStorageAssignment where compartment = :compartment and voided = false",
		    MorgueStorageAssignment.class);
		query.setParameter("compartment", compartment);
		return query.list();
	}
	
}
