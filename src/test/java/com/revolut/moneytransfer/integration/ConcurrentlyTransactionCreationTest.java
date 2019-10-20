package com.revolut.moneytransfer.integration;

import com.revolut.moneytransfer.exceptions.ObjectModificationException;
import com.revolut.moneytransfer.model.BankAccount;
import com.revolut.moneytransfer.model.Currency;
import com.revolut.moneytransfer.model.Transaction;
import com.revolut.moneytransfer.service.BankAccountService;
import com.revolut.moneytransfer.service.ConstantMoneyExchangeService;
import com.revolut.moneytransfer.service.TransactionsService;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;

public class ConcurrentlyTransactionCreationTest {
    private TransactionsService transactionsService = TransactionsService.getInstance(new ConstantMoneyExchangeService());
    private BankAccountService bankAccountService = BankAccountService.getInstance();

    private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(1000L);
    private static final BigDecimal TRANSACTION_AMOUNT = BigDecimal.ONE;
    private static final int INVOCATION_COUNT = 1000;

    private Long fromBankAccountId;
    private Long toBankAccountId;

    @BeforeClass
    public void initData() throws ObjectModificationException {
        BankAccount fromBankAccount = new BankAccount(
                "New Bank Account",
                INITIAL_BALANCE,
                Currency.EUR
        );

        BankAccount toBankAccount = new BankAccount(
                "New Bank Account 2",
                BigDecimal.ZERO,
                Currency.USD
        );

        fromBankAccountId = bankAccountService.createBankAccount(fromBankAccount).getId();
        toBankAccountId = bankAccountService.createBankAccount(toBankAccount).getId();
    }

    @Test(threadPoolSize = 100, invocationCount = INVOCATION_COUNT)
    public void testConcurrentTransactionCreation() throws ObjectModificationException {
        Transaction transaction = new Transaction(
                fromBankAccountId,
                toBankAccountId,
                TRANSACTION_AMOUNT,
                Currency.EUR
        );

        transactionsService.createTransaction(transaction);
    }

    @AfterClass
    public void checkResults() {
        BankAccount bankAccount = bankAccountService.getBankAccountById(fromBankAccountId);

        assertThat(bankAccount.getBalance(), Matchers.comparesEqualTo(INITIAL_BALANCE));
        /*assertThat(bankAccount.getBlockedAmount(),
                Matchers.comparesEqualTo(
                        BigDecimal.ZERO.add(
                            TRANSACTION_AMOUNT.multiply(BigDecimal.valueOf(INVOCATION_COUNT)))
                )
        );
*/    }
}
