package org.openmrs.module.morgue.web.resource;

import java.util.Date;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.context.Context;
import org.openmrs.module.morgue.MorgueCompartment;
import org.openmrs.module.morgue.MorgueStorageAssignment;
import org.openmrs.module.morgue.api.MorgueService;
import org.openmrs.module.morgue.rest.controller.base.MorgueResourceController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMethod;

@CrossOrigin(origins = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE,
        RequestMethod.OPTIONS })
@Resource(name = RestConstants.VERSION_1 + MorgueResourceController.MORGUE_NAMESPACE + "/storage-assignment", supportedClass = MorgueStorageAssignment.class, supportedOpenmrsVersions = {
        "2.0.*", "2.1.*", "2.2.*", "2.0 - 2.*" })
@Authorized
public class MorgueStorageAssignmentResource extends DelegatingCrudResource<MorgueStorageAssignment> {
	
	@Override
	public MorgueStorageAssignment newDelegate() {
		return new MorgueStorageAssignment();
	}
	
	/**
	 * POST with patient + compartment uuids in the body routes through the service's
	 * assignPatientToCompartment business logic, rather than a raw save, so the double-booking
	 * checks are enforced.
	 */
	@Override
	public MorgueStorageAssignment save(MorgueStorageAssignment delegate) {
		if (delegate.getId() == null) {
			return Context.getService(MorgueService.class).assignPatientToCompartment(delegate.getPatient(),
			    delegate.getCompartment());
		}
		// Existing assignment being updated (e.g. discharge) - use plain save via DAO through service
		return Context.getService(MorgueService.class).dischargeAssignment(delegate);
	}
	
	@Override
	public MorgueStorageAssignment getByUniqueId(String uuid) {
		MorgueCompartment compartment = Context.getService(MorgueService.class).getCompartmentByUuid(uuid);
		if (compartment == null) {
			return null;
		}
		return Context.getService(MorgueService.class).getAssignmentsForCompartment(compartment).stream()
		        .filter(assignment -> !assignment.getVoided() && assignment.getDateDischarged() == null)
		        .findFirst().orElse(null);
	}
	
	@Override
	public void delete(MorgueStorageAssignment delegate, String reason, RequestContext context) throws ResponseException {
		delegate.setVoided(true);
		delegate.setVoidReason(reason);
		Context.getService(MorgueService.class).dischargeAssignment(delegate);
	}
	
	@Override
	public void purge(MorgueStorageAssignment delegate, RequestContext context) throws ResponseException {
		Context.getService(MorgueService.class).deleteAssignment(delegate);
	}
	
	@Override
    protected PageableResult doSearch(RequestContext context) {
        String patientUuid = context.getRequest().getParameter("patient");
        if (patientUuid != null) {
            Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
            return new NeedsPaging<>(
                    Context.getService(MorgueService.class).getAssignmentsForPatient(patient),
                    context);
        }

		String locationUuid = context.getRequest().getParameter("location");
		if (locationUuid != null) {
			Location location = Context.getLocationService().getLocationByUuid(locationUuid);
			if (location == null) {
				return new org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult();
			}
			String status = context.getRequest().getParameter("status");
			String createdOnOrAfter = context.getRequest().getParameter("createdOnOrAfter");
			String admittedOnOrAfter = context.getRequest().getParameter("admittedOnOrAfter");
			String admittedOnOrBefore = context.getRequest().getParameter("admittedOnOrBefore");
			return new NeedsPaging<>(Context.getService(MorgueService.class).getAssignmentsForLocation(location, false, status,
			        convertDate(createdOnOrAfter), convertDate(admittedOnOrAfter), convertDate(admittedOnOrBefore)), context);
		}
        return new org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult();
    }
	
	@Override
	protected PageableResult doGetAll(RequestContext context) throws ResponseException {
		return doSearch(context);
	}
	
	private Date convertDate(String value) {
		return value == null || value.trim().isEmpty() ? null : (Date) ConversionUtil.convert(value, Date.class);
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		if (rep instanceof CustomRepresentation) {
			return null;
		}
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("uuid");
		description.addProperty("patient", Representation.REF);
		description.addProperty("compartment", Representation.REF);
		description.addProperty("dateAdmitted");
		description.addProperty("dateDischarged");
		description.addProperty("status");
		
		if (rep instanceof FullRepresentation) {
			description.addProperty("auditInfo");
		}
		
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("patient", Representation.REF);
		description.addProperty("compartment", Representation.REF);
		return description;
	}
	
	@Override
	public String getUri(Object instance) {
		MorgueStorageAssignment assignment = (MorgueStorageAssignment) instance;
		String compartmentUuid = assignment.getCompartment() == null ? assignment.getUuid() : assignment.getCompartment()
		        .getUuid();
		return RestConstants.URI_PREFIX + MorgueResourceController.MORGUE_NAMESPACE + "/storage-assignment/"
		        + compartmentUuid;
	}
	
	@Override
	public String getResourceVersion() {
		return "1.0";
	}
}
