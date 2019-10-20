package com.revolut.moneytransfer.dto;

import com.revolut.moneytransfer.constants.BankAccountConstants;
import com.revolut.moneytransfer.db.DbUtils;
import com.revolut.moneytransfer.exceptions.ObjectModificationException;
import com.revolut.moneytransfer.model.BankAccount;
import com.revolut.moneytransfer.model.Currency;
import com.revolut.moneytransfer.model.Transaction;
import com.revolut.moneytransfer.model.TransactionStatus;
import com.revolut.moneytransfer.service.ConstantMoneyExchangeService;
import com.revolut.moneytransfer.service.MoneyExchangeService;
import org.hamcrest.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class TransactionDtoTest {
    private TransactionDto transactionDto;
    private Collection<Transaction> testList;
    private MoneyExchangeService moneyExchangeService = new ConstantMoneyExchangeService();

    private static final Long TRANSACTION_1_ID = 1L;
    private static final Long TRANSACTION_2_ID = 2L;

    private Transaction transaction1;
    private Transaction transaction2;

    @BeforeClass
    public void initTestData() {
        DbUtils dbUtils = mock(DbUtils.class);
        transactionDto = new TransactionDto(dbUtils);

        transaction1 = new Transaction(
                BankAccountConstants.SAKALYA_DESHPANDE_BANK_ACCOUNT_ID,
                BankAccountConstants.JOHN_DOE_BANK_ACCOUNT_ID,
                BigDecimal.ONE,
                Currency.EUR);
        transaction1.setId(TRANSACTION_1_ID);

        transaction2 = new Transaction(
                BankAccountConstants.JOHN_DOE_BANK_ACCOUNT_ID,
                BankAccountConstants.JANE_DOE_BANK_ACCOUNT_ID,
                BigDecimal.TEN,
                Currency.EUR);
        transaction2.setId(TRANSACTION_2_ID);

        testList = Arrays.asList(transaction1, transaction2);

        when(dbUtils.executeQuery(eq(TransactionDto.GET_ALL_TRANSACTIONS_SQL), any())).thenReturn(
                new DbUtils.QueryResult<>(testList)
        );

        when(dbUtils.executeQuery(eq(TransactionDto.GET_TRANSACTIONS_BY_STATUS_SQL), any())).thenReturn(
                new DbUtils.QueryResult<>(testList.stream().map(Transaction::getId).collect(Collectors.toList()))
        );

        when(dbUtils.executeQueryInConnection(any(), eq(TransactionDto.GET_TRANSACTIONS_FOR_UPDATE_BY_ID_SQL), any()))
                .thenReturn(new DbUtils.QueryResult<>(testList));
    }

    /**
     * Tests that all transactions from DB will be returned
     */
    @Test
    public void testGetAllTransactions() {
        Collection<Transaction> resultList = transactionDto.getAllTransactions();

        assertNotNull(resultList);
        assertEquals(testList, resultList);
    }

    /**
     * Tests that all transaction's id with particular status will be returned
     */
    @Test
    public void testGetAllTransactionIdsByStatus() {
        Collection<Long> resultTransactionIds = transactionDto.getAllTransactionIdsByStatus(TransactionStatus.PLANNED);

        assertNotNull(resultTransactionIds);
        assertEquals(resultTransactionIds.size(), 2);
        assertTrue(resultTransactionIds.contains(BankAccountConstants.SAKALYA_DESHPANDE_BANK_ACCOUNT_ID));
        assertTrue(resultTransactionIds.contains(BankAccountConstants.JOHN_DOE_BANK_ACCOUNT_ID));
    }

    @Test
    public void testTransactionCreation() throws ObjectModificationException {
        TransactionDto transactionDto = TransactionDto.getInstance(moneyExchangeService);
        BankAccountDto bankAccountDto = BankAccountDto.getInstance();

        BankAccount sakalya = bankAccountDto.getBankAccountById(BankAccountConstants.SAKALYA_DESHPANDE_BANK_ACCOUNT_ID);
        BankAccount john = bankAccountDto.getBankAccountById(BankAccountConstants.JOHN_DOE_BANK_ACCOUNT_ID);

        BigDecimal sakalyaInitialBalance = sakalya.getBalance();
        BigDecimal johnInitialBalance = john.getBalance();

        Transaction resultTransaction = transactionDto.createTransaction(transaction1);

        assertEquals(resultTransaction.getStatus(), TransactionStatus.PLANNED);

        sakalya = bankAccountDto.getBankAccountById(BankAccountConstants.SAKALYA_DESHPANDE_BANK_ACCOUNT_ID);
        john = bankAccountDto.getBankAccountById(BankAccountConstants.JOHN_DOE_BANK_ACCOUNT_ID);

        assertThat(sakalyaInitialBalance, Matchers.comparesEqualTo(sakalya.getBalance()));

        assertThat(johnInitialBalance, Matchers.comparesEqualTo(john.getBalance()));
    }

    @Test
    public void testTransactionExecution() throws ObjectModificationException {
        TransactionDto transactionDto = TransactionDto.getInstance(moneyExchangeService);
        BankAccountDto bankAccountDto = BankAccountDto.getInstance();

        BankAccount john = bankAccountDto.getBankAccountById(BankAccountConstants.JOHN_DOE_BANK_ACCOUNT_ID);
        BankAccount jane = bankAccountDto.getBankAccountById(BankAccountConstants.JANE_DOE_BANK_ACCOUNT_ID);

        BigDecimal nikolayInitialBalance = john.getBalance();
        BigDecimal vladInitialBalance = jane.getBalance();

        Transaction resultTransaction = transactionDto.createTransaction(transaction2);
        transactionDto.executeTransaction(resultTransaction.getId());

        resultTransaction = transactionDto.getTransactionById(resultTransaction.getId());
        john = bankAccountDto.getBankAccountById(transaction2.getFromBankAccountId());
        jane = bankAccountDto.getBankAccountById(transaction2.getToBankAccountId());
        BigDecimal needToWithdraw = moneyExchangeService.exchange(
                transaction2.getAmount(),
                transaction2.getCurrency(),
                john.getCurrency()
        );
        BigDecimal needToTransfer = moneyExchangeService.exchange(
                transaction2.getAmount(),
                transaction2.getCurrency(),
                jane.getCurrency()
        );

        assertEquals(resultTransaction.getStatus(), TransactionStatus.SUCCEED);
        assertThat(nikolayInitialBalance.subtract(needToWithdraw), Matchers.comparesEqualTo(john.getBalance()));

        assertThat(vladInitialBalance.add(needToTransfer), Matchers.comparesEqualTo(jane.getBalance()));

    }

    @Test(expectedExceptions = ObjectModificationException.class)
    public void testWrongTransactionCreation() throws ObjectModificationException {
        TransactionDto transactionDto = TransactionDto.getInstance(moneyExchangeService);
        BankAccountDto bankAccountDto = BankAccountDto.getInstance();

        Transaction transaction = new Transaction(
                BankAccountConstants.JOHN_DOE_BANK_ACCOUNT_ID,
                BankAccountConstants.JANE_DOE_BANK_ACCOUNT_ID,
                BigDecimal.valueOf(10000),
                Currency.EUR
        );

        transactionDto.createTransaction(transaction);
    }
}
