package com.template;

import javassist.tools.web.BadHttpRequest;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.transactions.SignedTransaction;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

// This API is accessible from /api/template. The endpoint paths specified below are relative to it.
@Path("template")
public class TemplateApi {
    private final CordaRPCOps rpcOps;
    private final Party myIdentity;

    public TemplateApi(CordaRPCOps services) {
        this.rpcOps = services;
        this.myIdentity = rpcOps.nodeInfo().getLegalIdentities().get(0);
    }

    /**
     * Accessible at /api/template/templateGetEndpoint.
     */
    @GET
    @Path("templateGetEndpoint")
    @Produces(MediaType.APPLICATION_JSON)
    public Response templateGetEndpoint() {
        return Response.ok("Template GET endpoint.").build();
    }

    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Party me(){
        return myIdentity;
    }

    @GET
    @Path("issue")
    @Produces(MediaType.APPLICATION_JSON)
    public Response issue(@QueryParam(value = "name") String name){
        try {
            final FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(
                    TemplateFlow.Initiator.class,
                    name
            );

            final SignedTransaction result = flowHandle.getReturnValue().get();

            return Response.status(Response.Status.CREATED).entity(result).build();
        } catch (Exception e){
            return Response.status(Response.Status.BAD_REQUEST).entity(e).build();
        }
    }
}