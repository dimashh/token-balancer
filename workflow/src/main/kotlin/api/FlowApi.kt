package api

import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.IdentityService
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

val SERVICE_NAMES = listOf("oracle", "Network Map Service")

@Path("agreement")
class FlowApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<FlowApi>()
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    //@Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
            .map { it.legalIdentities.first().name }
            //filter out myself, oracle and eventual network map started by driver
            .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

}