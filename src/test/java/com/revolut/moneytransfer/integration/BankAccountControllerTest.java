package com.revolut.moneytransfer.integration;

import com.revolut.moneytransfer.MoneyTransferApp;
import com.revolut.moneytransfer.constants.BankAccountConstants;
import com.revolut.moneytransfer.controller.BankAccountsController;
import com.revolut.moneytransfer.dto.BankAccountDto;
import com.revolut.moneytransfer.model.BankAccount;
import com.revolut.moneytransfer.model.Currency;
import com.revolut.moneytransfer.service.BankAccountService;
import org.glassfish.grizzly.http.server.HttpServer;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.Assert.assertNotEquals;

public class BankAccountControllerTest {
    private static HttpServer server;
    private static WebTarget target;

    @BeforeClass
    public static void beforeAll() {
        // start the server
        server = MoneyTransferApp.startServer();
        // create the client
        Client c = ClientBuilder.newClient();

        target = c.target(MoneyTransferApp.BASE_URI);
    }

    @AfterClass
    public static void afterAll() {
        server.shutdownNow();
    }

    /**
     * Tests that all bank accounts will be returned from the database
     */
    @Test
    public void testGetAllBankAccounts() {
        Response response = target.path(BankAccountsController.BASE_URL)
                .request().get();

        assertEquals(Response.Status.OK, response.getStatusInfo().toEnum());

        Collection<BankAccount> bankAccount = response.readEntity(new GenericType<Collection<BankAccount>>(){});

        assertEquals(bankAccount.size(), BankAccountDto.getInstance().getAllBankAccounts().size());
    }

    /**
     * Tests that particular bank account will be returned from the database
     */
    @Test
    public void testGetBankAccountById() {
        Response response = getById(BankAccountConstants.SAKALYA_DESHPANDE_BANK_ACCOUNT_ID);

        assertEquals(Response.Status.OK, response.getStatusInfo().toEnum());

        BankAccount bankAccount = response.readEntity(BankAccount.class);

        assertEquals(bankAccount.getId(), BankAccountConstants.SAKALYA_DESHPANDE_BANK_ACCOUNT_ID);
    }

    /**
     * Tests that method responds correctly if ID will be passed incorrectly (non int)
     */
    @Test
    public void testGetNullBankAccount() {
        Response response = getById(null);

        assertEquals(Response.Status.NOT_FOUND, response.getStatusInfo().toEnum());
    }

    /**
     * Tests that non existing bank account will be returned correctly
     */
    @Test
    public void testNonExistingBankAccountById() {
        Response response = getById(new Random().nextLong());

        assertEquals(Response.Status.NOT_FOUND, response.getStatusInfo().toEnum());
    }

    /**
     * Tests the successful update of the bank account. Even if it will be attempt to update balance it will not be
     * updated
     */
    @Test
    public void testUpdateBankAccount() {
        BankAccountService bankAccountService = BankAccountService.getInstance();
        String OWNER_NAME = "Owner Name";

        BankAccount secondAccount = bankAccountService.getBankAccountById(BankAccountConstants.JOHN_DOE_BANK_ACCOUNT_ID);
        secondAccount.setAccountHolderName(OWNER_NAME);
        BigDecimal accountBalance = secondAccount.getBalance();
        secondAccount.setBalance(accountBalance.add(BigDecimal.TEN));

        Response response = target.path(BankAccountsController.BASE_URL)
                .request()
                .put(from(secondAccount));

        assertEquals(Response.Status.OK, response.getStatusInfo().toEnum());

        BankAccount updatedAccount = bankAccountService.getBankAccountById(BankAccountConstants.JOHN_DOE_BANK_ACCOUNT_ID);

        assertEquals(OWNER_NAME, updatedAccount.getAccountHolderName());
        assertThat(accountBalance, Matchers.comparesEqualTo(updatedAccount.getBalance()));
    }

    /**
     * Tests the unsuccessful update of the bank account with non-existing id
     */
    @Test
    public void testUpdateNonExistingBankAccount() {
        BankAccount bankAccount = new BankAccount(new Random().nextLong(),
                "", BigDecimal.ZERO,  Currency.GBP);

        Response response = target.path(BankAccountsController.BASE_URL)
                .request()
                .put(from(bankAccount));

        assertEquals(Response.Status.NOT_FOUND, response.getStatusInfo().toEnum());
    }

    /**
     * Tests the unsuccessful update of the incorrect bank account
     */
    @Test
    public void testIncorrectUpdateBankAccount() {
        BankAccount bankAccount = new BankAccount();
        bankAccount.setId(new Random().nextLong());

        Response response = target.path(BankAccountsController.BASE_URL)
                .request()
                .put(from(bankAccount));

        assertEquals(Response.Status.BAD_REQUEST, response.getStatusInfo().toEnum());
    }

    /**
     * Tests the successful creation of the new bank account
     */
    @Test
    public void testCreateBankAccount() {
        BankAccountService bankAccountService = BankAccountService.getInstance();
        String ACCOUNT_HOLDER_NAME = "Account Holder Name";

        BankAccount bankAccount = new BankAccount(ACCOUNT_HOLDER_NAME, BigDecimal.ZERO,Currency.GBP);

        Response response = target.path(BankAccountsController.BASE_URL)
                .request()
                .post(from(bankAccount));

        assertEquals(Response.Status.OK, response.getStatusInfo().toEnum());

        BankAccount returnedAccount = response.readEntity(BankAccount.class);
        BankAccount createdAccount = bankAccountService.getBankAccountById(returnedAccount.getId());

        assertNotNull(returnedAccount);
        assertNotNull(createdAccount);

        assertNotEquals(returnedAccount.getId(), bankAccount.getId());
        assertEquals(returnedAccount.getId(), createdAccount.getId());
        assertEquals(ACCOUNT_HOLDER_NAME, createdAccount.getAccountHolderName());
    }

    private Response getById(Long id) {
        return target.path(BankAccountsController.BASE_URL + "/{" + BankAccountsController.GET_BANK_ACCOUNT_BY_ID_PATH + "}")
                .resolveTemplate("id", id == null ? "null" : id)
                .request().get();
    }

    private static Entity from(BankAccount bankAccount) {
        return Entity.entity(bankAccount, MediaType.valueOf(MediaType.APPLICATION_JSON));
    }
}
