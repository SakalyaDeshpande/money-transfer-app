package com.revolut.moneytransfer.controller;

import com.revolut.moneytransfer.exceptions.ObjectModificationException;
import com.revolut.moneytransfer.model.BankAccount;
import com.revolut.moneytransfer.service.BankAccountService;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;

/**
 * This class is responsible for CRUD operations of Bank Account object
 */
@Path(BankAccountsController.BASE_URL)
@Produces(MediaType.APPLICATION_JSON)
public class BankAccountsController {
    public static final String BASE_URL = "/accounts";
    public static final String GET_BANK_ACCOUNT_BY_ID_PATH = "id";

    private final static BankAccountService bankAccountService = BankAccountService.getInstance();

    /**
     * Creates the Bank Account object with the provided parameters. It doesn't mean if provided object will have
     * an ID specified. This ID will be regenerated and returned in the response object
     *
     * @param bankAccount the Bank Account object to create with parameters specified
     *
     * @return Bank Account object with the ID parameter specified.
     */
    @POST
    public Response createBankAccount(@Valid BankAccount bankAccount) throws ObjectModificationException {
        BankAccount createdBankAccount;

        createdBankAccount = bankAccountService.createBankAccount(bankAccount);

        return Response.ok(createdBankAccount).build();
    }

    @GET
    public Response getAllBankAccounts() {
        Collection<BankAccount> bankAccounts;

        bankAccounts = bankAccountService.getAllBankAccounts();

        if (bankAccounts == null) {
            Response.noContent().build();
        }

        return Response.ok(bankAccounts).build();
    }

    /**
     * @param id The ID of Bank Account
     *
     * @return The Bank Account object which has particular ID. This ID has been generated and returned
     * during the Bank Account creation by the <code>POST: /bankAccount</code> endpoint
     */
    @GET
    @Path("{" + GET_BANK_ACCOUNT_BY_ID_PATH + "}")
    public Response getBankAccountById(@PathParam(GET_BANK_ACCOUNT_BY_ID_PATH) Long id) {
        BankAccount bankAccount;


        bankAccount = bankAccountService.getBankAccountById(id);

        if (bankAccount == null) {
            throw new WebApplicationException("The bank account does not exist", Response.Status.NOT_FOUND);
        }

        return Response.ok(bankAccount).build();
    }

    /**
     * Updates the particular Bank Account with the parameters provided. The Bank Account which should be
     * updated is searching by the ID which has provided object. You can not update <code>balance</code> and/or
     * <code>blockedAmount</code> fields of the object as it is information maintained only by the system.
     *
     * @param bankAccount the Bank Account object (id should be specified) which will update the data
     *
     * @return updated Bank Account object. In general it should be object with the same parameters as provided had
     */
    @PUT
    public Response updateBankAccount(BankAccount bankAccount) throws ObjectModificationException {
        BankAccountService.getInstance().updateBankAccount(bankAccount);

        return Response.ok(bankAccount).build();
    }


}
