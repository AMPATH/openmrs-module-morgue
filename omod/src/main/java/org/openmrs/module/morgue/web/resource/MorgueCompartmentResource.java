package org.openmrs.module.morgue.web.resource;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.context.Context;
import org.openmrs.module.morgue.MorgueCompartment;
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
@Resource(name = RestConstants.VERSION_1 + MorgueResourceController.MORGUE_NAMESPACE + "/compartment", supportedClass = MorgueCompartment.class, supportedOpenmrsVersions = {
        "2.0.*", "2.1.*", "2.2.*", "2.0 - 2.*" })
@Authorized
public class MorgueCompartmentResource extends DelegatingCrudResource<MorgueCompartment> {
	
	@Override
	public MorgueCompartment newDelegate() {
		return new MorgueCompartment();
	}
	
	@Override
	public MorgueCompartment save(MorgueCompartment delegate) {
		return Context.getService(MorgueService.class).saveCompartment(delegate);
	}
	
	@Override
	public MorgueCompartment getByUniqueId(String uuid) {
		return Context.getService(MorgueService.class).getCompartmentByUuid(uuid);
	}
	
	@Override
	public void delete(MorgueCompartment delegate, String reason, RequestContext context) throws ResponseException {
		delegate.setVoided(true);
		delegate.setVoidReason(reason);
		Context.getService(MorgueService.class).saveCompartment(delegate);
	}
	
	@Override
	public void purge(MorgueCompartment delegate, RequestContext context) throws ResponseException {
		Context.getService(MorgueService.class).deleteCompartment(delegate);
	}
	
	@Override
    protected PageableResult doSearch(RequestContext context) {
        String storageUnitUuid = context.getRequest().getParameter("storageUnit");
        if (storageUnitUuid != null) {
            org.openmrs.module.morgue.MorgueStorageUnit unit =
                    Context.getService(MorgueService.class).getStorageUnitByUuid(storageUnitUuid);
            return new NeedsPaging<>(
                    Context.getService(MorgueService.class).getCompartmentsByStorageUnit(unit),
                    context);
        }
        return new org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult();
    }
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("uuid");
		description.addProperty("display");
		description.addProperty("storageUnit", Representation.REF);
		
		if (rep instanceof FullRepresentation) {
			description.addProperty("auditInfo");
		}
		
		return description;
	}
	
	@Override
	public String getUri(Object instance) {
		MorgueCompartment compartment = (MorgueCompartment) instance;
		return RestConstants.URI_PREFIX + MorgueResourceController.MORGUE_NAMESPACE + "/compartment/"
		        + compartment.getUuid();
	}
	
	@Override
	public String getResourceVersion() {
		return "1.0";
	}
}
