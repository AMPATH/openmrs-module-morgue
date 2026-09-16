package org.openmrs.module.morgue.web.resource;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.context.Context;
import org.openmrs.module.morgue.MorgueStorageUnit;
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
@Resource(name = RestConstants.VERSION_1 + MorgueResourceController.MORGUE_NAMESPACE + "/storageunit", supportedClass = MorgueStorageUnit.class, supportedOpenmrsVersions = {
        "2.0.*", "2.1.*", "2.2.*", "2.0 - 2.*" })
@Authorized
public class MorgueStorageUnitResource extends DelegatingCrudResource<MorgueStorageUnit> {
	
	@Override
	public MorgueStorageUnit newDelegate() {
		return new MorgueStorageUnit();
	}
	
	@Override
	public MorgueStorageUnit save(MorgueStorageUnit delegate) {
		return Context.getService(MorgueService.class).saveStorageUnit(delegate);
	}
	
	@Override
	public MorgueStorageUnit getByUniqueId(String uuid) {
		return Context.getService(MorgueService.class).getStorageUnitByUuid(uuid);
	}
	
	@Override
	public void delete(MorgueStorageUnit delegate, String reason, RequestContext context) throws ResponseException {
		delegate.setVoided(true);
		delegate.setVoidReason(reason);
		Context.getService(MorgueService.class).saveStorageUnit(delegate);
	}
	
	@Override
	public void purge(MorgueStorageUnit delegate, RequestContext context) throws ResponseException {
		Context.getService(MorgueService.class).deleteStorageUnit(delegate);
	}
	
	@Override
    protected PageableResult doGetAll(RequestContext context) throws ResponseException {
        return new NeedsPaging<>(
                Context.getService(MorgueService.class).getAllStorageUnits(false, null),
                context);
    }
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("uuid");
		description.addProperty("display");
		description.addProperty("location", Representation.REF);
		
		if (rep instanceof FullRepresentation) {
			description.addProperty("auditInfo");
		}
		
		return description;
	}
	
	@Override
	public String getUri(Object instance) {
		MorgueStorageUnit unit = (MorgueStorageUnit) instance;
		return RestConstants.URI_PREFIX + MorgueResourceController.MORGUE_NAMESPACE + "/storageunit/" + unit.getUuid();
	}
	
	@Override
	public String getResourceVersion() {
		return "1.0";
	}
}
