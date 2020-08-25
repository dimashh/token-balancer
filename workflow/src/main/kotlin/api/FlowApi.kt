package api

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.node.services.IdentityService
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import workflow.CreateWalletFlow.Initiator
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

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

    @POST
    @Path("/wallet")
    fun createWallet(
        @QueryParam("quantity") quantity: Long?,
        @QueryParam("currency") currencyCode: String?,
        @QueryParam("token") token: FungibleToken?,
        @QueryParam("owner") owner: CordaX500Name,
        @QueryParam("issuer") issuer: CordaX500Name
    ): Response {
        val currency = try {
            Currency.getInstance(currencyCode)
        } catch (e: IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Currency $currencyCode cannot be found.\n")
                .build()
        } catch (e: Exception) {
            logger.error("Failed getting Currency $currencyCode", e)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error getting $currencyCode.\n")
                .build()
        }

        val owner = rpcOps.wellKnownPartyFromX500Name(owner)
            ?: return Response.status(Response.Status.BAD_REQUEST).entity("Party named $owner cannot be found.\n")
                .build()

        val issuer = rpcOps.wellKnownPartyFromX500Name(issuer)
            ?: return Response.status(Response.Status.BAD_REQUEST).entity("Party named $issuer cannot be found.\n")
                .build()

        return try {
            val signedTx = rpcOps.startTrackedFlow(::Initiator,currency, null, owner, listOf(owner))
                .returnValue.getOrThrow()
            Response.status(Response.Status.CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(Response.Status.BAD_REQUEST).entity(ex.message!!).build()
        }
    }

}