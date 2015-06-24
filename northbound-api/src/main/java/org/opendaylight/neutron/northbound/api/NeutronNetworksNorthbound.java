/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.neutron.northbound.api;

import java.net.HttpURLConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.neutron.spi.INeutronNetworkAware;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.NeutronCRUDInterfaces;
import org.opendaylight.neutron.spi.NeutronNetwork;

/**
 * Neutron Northbound REST APIs for Network.<br>
 * This class provides REST APIs for managing neutron Networks
 *
 * <br>
 * <br>
 * Authentication scheme : <b>HTTP Basic</b><br>
 * Authentication realm : <b>opendaylight</b><br>
 * Transport : <b>HTTP and HTTPS</b><br>
 * <br>
 * HTTPS Authentication is disabled by default. Administrator can enable it in
 * tomcat-server.xml after adding a proper keystore / SSL certificate from a
 * trusted authority.<br>
 * More info :
 * http://tomcat.apache.org/tomcat-7.0-doc/ssl-howto.html#Configuration
 *
 */

@Path("/networks")
public class NeutronNetworksNorthbound {

    @Context
    UriInfo uriInfo;

    private static final int HTTP_OK_BOTTOM = 200;
    private static final int HTTP_OK_TOP = 299;
    private static final String INTERFACE_NAME = "Network CRUD Interface";
    private static final String UUID_NO_EXIST = "Network UUID does not exist.";
    private static final String UUID_EXISTS = "Network UUID already exists.";
    private static final String NO_PROVIDERS = "No providers registered.  Please try again later";
    private static final String NO_PROVIDER_LIST = "Couldn't get providers list.  Please try again later";

    private NeutronNetwork extractFields(NeutronNetwork o, List<String> fields) {
        return o.extractFields(fields);
    }

    /**
     * Returns a list of all Networks */

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackNetworks.class)
    @StatusCodes({
        @ResponseCode(code = HttpURLConnection.HTTP_OK, condition = "Operation successful"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAUTHORIZED, condition = "Unauthorized"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_IMPLEMENTED, condition = "Not Implemented"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAVAILABLE, condition = "No providers available") })
    public Response listNetworks(
            // return fields
            @QueryParam("fields") List<String> fields,
            // note: openstack isn't clear about filtering on lists, so we aren't handling them
            @QueryParam("id") String queryID,
            @QueryParam("name") String queryName,
            @QueryParam("admin_state_up") String queryAdminStateUp,
            @QueryParam("status") String queryStatus,
            @QueryParam("shared") String queryShared,
            @QueryParam("tenant_id") String queryTenantID,
            @QueryParam("router_external") String queryRouterExternal,
            @QueryParam("provider_network_type") String queryProviderNetworkType,
            @QueryParam("provider_physical_network") String queryProviderPhysicalNetwork,
            @QueryParam("provider_segmentation_id") String queryProviderSegmentationID,
            // linkTitle
            @QueryParam("limit") Integer limit,
            @QueryParam("marker") String marker,
            @DefaultValue("false") @QueryParam("page_reverse") Boolean pageReverse
            // sorting not supported
            ) {
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException(INTERFACE_NAME
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        List<NeutronNetwork> allNetworks = networkInterface.getAllNetworks();
        List<NeutronNetwork> ans = new ArrayList<NeutronNetwork>();
        Iterator<NeutronNetwork> i = allNetworks.iterator();
        while (i.hasNext()) {
            NeutronNetwork oSN = i.next();
            //match filters: TODO provider extension
            Boolean bAdminStateUp = null;
            Boolean bShared = null;
            Boolean bRouterExternal = null;
            if (queryAdminStateUp != null) {
                bAdminStateUp = Boolean.valueOf(queryAdminStateUp);
            }
            if (queryShared != null) {
                bShared = Boolean.valueOf(queryShared);
            }
            if (queryRouterExternal != null) {
                bRouterExternal = Boolean.valueOf(queryRouterExternal);
            }
            if ((queryID == null || queryID.equals(oSN.getID())) &&
                    (queryName == null || queryName.equals(oSN.getNetworkName())) &&
                    (bAdminStateUp == null || bAdminStateUp.booleanValue() == oSN.isAdminStateUp()) &&
                    (queryStatus == null || queryStatus.equals(oSN.getStatus())) &&
                    (bShared == null || bShared.booleanValue() == oSN.isShared()) &&
                    (bRouterExternal == null || bRouterExternal.booleanValue() == oSN.isRouterExternal()) &&
                    (queryTenantID == null || queryTenantID.equals(oSN.getTenantID()))) {
                if (fields.size() > 0) {
                    ans.add(extractFields(oSN,fields));
                } else {
                    ans.add(oSN);
                }
            }
        }

        if (limit != null && ans.size() > 1) {
            // Return a paginated request
            NeutronNetworkRequest request = (NeutronNetworkRequest) PaginatedRequestFactory.createRequest(limit,
                    marker, pageReverse, uriInfo, ans, NeutronNetwork.class);
            return Response.status(HttpURLConnection.HTTP_OK).entity(request).build();
        }

    return Response.status(HttpURLConnection.HTTP_OK).entity(new NeutronNetworkRequest(ans)).build();

    }

    /**
     * Returns a specific Network */

    @Path("{netUUID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackNetworks.class)
    @StatusCodes({
        @ResponseCode(code = HttpURLConnection.HTTP_OK, condition = "Operation successful"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAUTHORIZED, condition = "Unauthorized"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_FOUND, condition = "Not Found"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_IMPLEMENTED, condition = "Not Implemented"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAVAILABLE, condition = "No providers available") })
    public Response showNetwork(
            @PathParam("netUUID") String netUUID,
            // return fields
            @QueryParam("fields") List<String> fields
            ) {
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD( this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException(INTERFACE_NAME
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!networkInterface.networkExists(netUUID)) {
            throw new ResourceNotFoundException(UUID_NO_EXIST);
        }
        if (fields.size() > 0) {
            NeutronNetwork ans = networkInterface.getNetwork(netUUID);
            return Response.status(HttpURLConnection.HTTP_OK).entity(
                    new NeutronNetworkRequest(extractFields(ans, fields))).build();
        } else {
            return Response.status(HttpURLConnection.HTTP_OK).entity(
                    new NeutronNetworkRequest(networkInterface.getNetwork(netUUID))).build();
        }
    }

    /**
     * Creates new Networks */
    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    @TypeHint(NeutronNetwork.class)
    @StatusCodes({
        @ResponseCode(code = HttpURLConnection.HTTP_CREATED, condition = "Created"),
        @ResponseCode(code = HttpURLConnection.HTTP_BAD_REQUEST, condition = "Bad Request"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAUTHORIZED, condition = "Unauthorized"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_IMPLEMENTED, condition = "Not Implemented"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAVAILABLE, condition = "No providers available") })
    public Response createNetworks(final NeutronNetworkRequest input) {
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD( this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException(INTERFACE_NAME
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (input.isSingleton()) {
            NeutronNetwork singleton = input.getSingleton();

            /*
             * network ID can't already exist
             */
            if (networkInterface.networkExists(singleton.getID())) {
                throw new BadRequestException(UUID_EXISTS);
            }

            Object[] instances = NeutronUtil.getInstances(INeutronNetworkAware.class, this);
            if (instances != null) {
                if (instances.length > 0) {
                    for (Object instance : instances) {
                        INeutronNetworkAware service = (INeutronNetworkAware) instance;
                        int status = service.canCreateNetwork(singleton);
                        if (status < HTTP_OK_BOTTOM || status > HTTP_OK_TOP) {
                            return Response.status(status).build();
                        }
                    }
                } else {
                    throw new ServiceUnavailableException(NO_PROVIDERS);
                }
            } else {
                throw new ServiceUnavailableException(NO_PROVIDER_LIST);
            }

            // add network to cache
            singleton.initDefaults();
            networkInterface.addNetwork(singleton);
            if (instances != null) {
                for (Object instance : instances) {
                    INeutronNetworkAware service = (INeutronNetworkAware) instance;
                    service.neutronNetworkCreated(singleton);
                }
            }

        } else {
            List<NeutronNetwork> bulk = input.getBulk();
            Iterator<NeutronNetwork> i = bulk.iterator();
            HashMap<String, NeutronNetwork> testMap = new HashMap<String, NeutronNetwork>();
            Object[] instances = NeutronUtil.getInstances(INeutronNetworkAware.class, this);
            while (i.hasNext()) {
                NeutronNetwork test = i.next();

                /*
                 * network ID can't already exist, nor can there be an entry for this UUID
                 * already in this bulk request
                 */
                if (networkInterface.networkExists(test.getID())) {
                    throw new BadRequestException(UUID_EXISTS);
                }
                if (testMap.containsKey(test.getID())) {
                    throw new BadRequestException(UUID_EXISTS);
                }
                if (instances != null) {
                    if (instances.length > 0) {
                        for (Object instance: instances) {
                            INeutronNetworkAware service = (INeutronNetworkAware) instance;
                            int status = service.canCreateNetwork(test);
                            if (status < HTTP_OK_BOTTOM || status > HTTP_OK_TOP) {
                                return Response.status(status).build();
                            }
                        }
                    } else {
                        throw new ServiceUnavailableException(NO_PROVIDERS);
                    }
                } else {
                    throw new ServiceUnavailableException(NO_PROVIDER_LIST);
                }
                testMap.put(test.getID(),test);
            }

            // now that everything passed, add items to the cache
            i = bulk.iterator();
            while (i.hasNext()) {
                NeutronNetwork test = i.next();
                test.initDefaults();
                networkInterface.addNetwork(test);
                if (instances != null) {
                    for (Object instance: instances) {
                        INeutronNetworkAware service = (INeutronNetworkAware) instance;
                        service.neutronNetworkCreated(test);
                    }
                }
            }
        }
        return Response.status(HttpURLConnection.HTTP_CREATED).entity(input).build();
    }

    /**
     * Updates a Network */
    @Path("{netUUID}")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackNetworks.class)
    @StatusCodes({
        @ResponseCode(code = HttpURLConnection.HTTP_OK, condition = "Operation successful"),
        @ResponseCode(code = HttpURLConnection.HTTP_BAD_REQUEST, condition = "Bad Request"),
        @ResponseCode(code = HttpURLConnection.HTTP_FORBIDDEN, condition = "Forbidden"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_FOUND, condition = "Not Found"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_IMPLEMENTED, condition = "Not Implemented"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAVAILABLE, condition = "No providers available") })
    public Response updateNetwork(
            @PathParam("netUUID") String netUUID, final NeutronNetworkRequest input
            ) {
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD( this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException(INTERFACE_NAME
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * network has to exist and only a single delta is supported
         */
        if (!networkInterface.networkExists(netUUID)) {
            throw new ResourceNotFoundException(UUID_NO_EXIST);
        }
        if (!input.isSingleton()) {
            throw new BadRequestException("only singleton edits supported");
        }
        NeutronNetwork delta = input.getSingleton();

        /*
         * transitions forbidden by Neutron
         */
        if (delta.getID() != null || delta.getTenantID() != null ||
                delta.getStatus() != null) {
            throw new BadRequestException("attribute edit blocked by Neutron");
        }

        Object[] instances = NeutronUtil.getInstances(INeutronNetworkAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronNetworkAware service = (INeutronNetworkAware) instance;
                    NeutronNetwork original = networkInterface.getNetwork(netUUID);
                    int status = service.canUpdateNetwork(delta, original);
                    if (status < HTTP_OK_BOTTOM || status > HTTP_OK_TOP) {
                        return Response.status(status).build();
                    }
                }
            } else {
                throw new ServiceUnavailableException(NO_PROVIDERS);
            }
        } else {
            throw new ServiceUnavailableException(NO_PROVIDER_LIST);
        }

        // update network object and return the modified object
                networkInterface.updateNetwork(netUUID, delta);
                NeutronNetwork updatedSingleton = networkInterface.getNetwork(netUUID);
                if (instances != null) {
                    for (Object instance : instances) {
                        INeutronNetworkAware service = (INeutronNetworkAware) instance;
                        service.neutronNetworkUpdated(updatedSingleton);
                    }
                }
                return Response.status(HttpURLConnection.HTTP_OK).entity(
                        new NeutronNetworkRequest(networkInterface.getNetwork(netUUID))).build();
    }

    /**
     * Deletes a Network */

    @Path("{netUUID}")
    @DELETE
    @StatusCodes({
        @ResponseCode(code = HttpURLConnection.HTTP_NO_CONTENT, condition = "No Content"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAUTHORIZED, condition = "Unauthorized"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_FOUND, condition = "Not Found"),
        @ResponseCode(code = HttpURLConnection.HTTP_CONFLICT, condition = "Network In Use"),
        @ResponseCode(code = HttpURLConnection.HTTP_NOT_IMPLEMENTED, condition = "Not Implemented"),
        @ResponseCode(code = HttpURLConnection.HTTP_UNAVAILABLE, condition = "No providers available") })
    public Response deleteNetwork(
            @PathParam("netUUID") String netUUID) {
        INeutronNetworkCRUD networkInterface = NeutronCRUDInterfaces.getINeutronNetworkCRUD( this);
        if (networkInterface == null) {
            throw new ServiceUnavailableException(INTERFACE_NAME
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        /*
         * network has to exist and not be in use before it can be removed
         */
        if (!networkInterface.networkExists(netUUID)) {
            throw new ResourceNotFoundException(UUID_NO_EXIST);
        }
        if (networkInterface.networkInUse(netUUID)) {
            throw new ResourceConflictException("Network ID in use");
        }

        NeutronNetwork singleton = networkInterface.getNetwork(netUUID);
        Object[] instances = NeutronUtil.getInstances(INeutronNetworkAware.class, this);
        if (instances != null) {
            if (instances.length > 0) {
                for (Object instance : instances) {
                    INeutronNetworkAware service = (INeutronNetworkAware) instance;
                    int status = service.canDeleteNetwork(singleton);
                    if (status < HTTP_OK_BOTTOM || status > HTTP_OK_TOP) {
                        return Response.status(status).build();
                    }
                }
            } else {
                throw new ServiceUnavailableException(NO_PROVIDERS);
            }
        } else {
            throw new ServiceUnavailableException(NO_PROVIDER_LIST);
        }

        networkInterface.removeNetwork(netUUID);
        if (instances != null) {
            for (Object instance : instances) {
                INeutronNetworkAware service = (INeutronNetworkAware) instance;
                service.neutronNetworkDeleted(singleton);
            }
        }
        return Response.status(HttpURLConnection.HTTP_NO_CONTENT).build();
    }
}
