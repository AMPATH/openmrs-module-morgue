package org.openmrs.module.morgue.web.resource;

import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.context.Context;
import org.openmrs.module.morgue.MorgueCompartment;
import org.openmrs.module.morgue.MorgueStorageAssignment;
import org.openmrs.module.morgue.api.MorgueService;
import org.openmrs.module.morgue.rest.controller.base.MorgueResourceController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
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
@Resource(name = RestConstants.VERSION_1 + MorgueResourceController.MORGUE_NAMESPACE + "/storageassignment", supportedClass = MorgueStorageAssignment.class, supportedOpenmrsVersions = {
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
		// No direct getByUuid on the service yet - falls back to searching patient assignments
		// Consider adding MorgueService.getAssignmentByUuid(uuid) if this is used often.
		throw new UnsupportedOperationException(
		        "getByUniqueId not yet implemented - add getAssignmentByUuid to MorgueService/DAO");
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
        return new org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult();
    }
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
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
	public String getUri(Object instance) {
		MorgueStorageAssignment assignment = (MorgueStorageAssignment) instance;
		return RestConstants.URI_PREFIX + MorgueResourceController.MORGUE_NAMESPACE + "/storageassignment/"
		        + assignment.getUuid();
	}
	
	@Override
	public String getResourceVersion() {
		return "1.0";
	}
}
