package com.revolut.moneytransfer.controller;

import com.revolut.moneytransfer.exceptions.ObjectModificationException;
import com.revolut.moneytransfer.model.Transaction;
import com.revolut.moneytransfer.service.ConstantMoneyExchangeService;
import com.revolut.moneytransfer.service.TransactionsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The resource is responsible for the Transaction entity. Make it possible to create
 * and provide transactions. There is no ability to update an existing transaction as it is controversial operation
 * for this type of object. This object could be modified only by the system itself
 */
@Path(TransactionsController.BASE_URL)
@Produces(MediaType.APPLICATION_JSON)
public class TransactionsController {
    private final Logger log = LoggerFactory.getLogger(TransactionsController.class);

    public static final String BASE_URL = "/transactions";
    public static final String GET_TRANSACTION_BY_ID_PATH = "id";

    private TransactionsService transactionsService = TransactionsService.getInstance(new ConstantMoneyExchangeService());

    /**
     * Returns all transactions in the system with there statuses
     */
    @GET
    public Response getAllTransactions() {
        return Response.ok().entity(transactionsService.getAllTransactions()).build();
    }

    /**
     * Returns transaction by specified ID
     *
     * @param id transaction ID
     *
     * @return Transaction with the ID provided
     */
    @GET()
    @Path("{" + GET_TRANSACTION_BY_ID_PATH + "}")
    public Response getTransactionById(@PathParam(GET_TRANSACTION_BY_ID_PATH) Long id) {
        return Response.ok().entity(transactionsService.getTransactionById(id)).build();
    }

    /**
     * Make it possible to create money transfer from one account to another.
     * The result of execution is created transaction with actual status. Usually it is "IN PROGRESS".
     * The transaction execution process is asynchronous and controlled by the system itself
     *
     * @param transaction The transaction object which should be created. The only required fields are:
     *                    <code>fromBankAccountId, toBankAccountId, amount, currency</code>. All other parameters
     *                    will be ignored and created by the system
     *
     * @return created and updated transaction object provided
     */
    @POST()
    public Response createTransaction(Transaction transaction) throws ObjectModificationException {
        transaction = transactionsService.createTransaction(transaction);

        return Response.ok().entity(transaction).build();
    }
}
