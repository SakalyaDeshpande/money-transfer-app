package com.revolut.moneytransfer.dto;

import com.revolut.moneytransfer.db.DbUtils;
import com.revolut.moneytransfer.db.H2DataSource;
import com.revolut.moneytransfer.exceptions.ImpossibleOperationExecution;
import com.revolut.moneytransfer.exceptions.ObjectModificationException;
import com.revolut.moneytransfer.service.MoneyExchangeService;
import com.revolut.moneytransfer.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Encapsulates all logic for Transaction entity which is related to the database. Implements the singleton pattern
 */
public class TransactionDto {
    private static final Logger log = LoggerFactory.getLogger(TransactionDto.class);

    private static final String TRANSACTION_TABLE_NAME = "transaction";
    private static final String TRANSACTION_ID_ROW = "id";
    private static final String TRANSACTION_FROM_ACCOUNT_ROW = "from_account_id";
    private static final String TRANSACTION_TO_ACCOUNT_ROW = "to_account_id";
    private static final String TRANSACTION_AMOUNT_ROW = "amount";
    private static final String TRANSACTION_CURRENCY_ROW = "currency_id";
    private static final String TRANSACTION_CREATION_DATE_ROW = "creation_date";
    private static final String TRANSACTION_UPDATE_DATE_ROW = "update_date";
    private static final String TRANSACTION_STATUS_ROW = "status_id";
    private static final String FAIL_MESSAGE_ROW = "failMessage";

    public static final String GET_ALL_TRANSACTIONS_SQL = "select * from " + TRANSACTION_TABLE_NAME;
    public static final String GET_TRANSACTIONS_BY_STATUS_SQL =
            "select id from " + TRANSACTION_TABLE_NAME + " trans " +
                    "where trans." + TRANSACTION_STATUS_ROW + " = ?";
    public static final String GET_TRANSACTIONS_BY_ID_SQL =
            "select * from " + TRANSACTION_TABLE_NAME + " trans " +
                    "where trans." + TRANSACTION_ID_ROW + " = ?";
    public static final String GET_TRANSACTIONS_FOR_UPDATE_BY_ID_SQL =
            GET_TRANSACTIONS_BY_ID_SQL + " for update";

    private static TransactionDto transactionDto;
    private BankAccountDto bankAccountDto = BankAccountDto.getInstance();
    private DbUtils dbUtils = DbUtils.getInstance();
    private MoneyExchangeService moneyExchangeService;

    private TransactionDto(MoneyExchangeService moneyExchangeService) {
        this.moneyExchangeService = moneyExchangeService;
    }

    //Just for testing purpose
    TransactionDto(DbUtils dbUtils) {
        this.dbUtils = dbUtils;
    }

    public static TransactionDto getInstance(MoneyExchangeService moneyExchangeService) {
        if(transactionDto == null){
            synchronized (TransactionDto.class) {
                if(transactionDto == null){
                    transactionDto = new TransactionDto(moneyExchangeService);
                }
            }
        }
        return transactionDto;
    }

    /**
     * @return All Transactions which is exists in the database at the moment
     *
     * TODO: add multipaging
     */
    public Collection<Transaction> getAllTransactions() {
        return dbUtils.executeQuery(GET_ALL_TRANSACTIONS_SQL, getAllTransactions -> {
            Collection<Transaction> transactions = new ArrayList<>();

            try (ResultSet transactionsRS = getAllTransactions.executeQuery()) {
                if (transactionsRS != null) {
                    while (transactionsRS.next()) {
                        transactions.add(extractTransactionFromResultSet(transactionsRS));
                    }
                }
            }

            return transactions;
        }).getResult();
    }

    /**
     * Returns the list of Transactions which has specified status
     *
     * @param transactionStatus transaction's status to be returned
     *
     * @return list of Transaction's ID which has the status provided
     */
    public Collection<Long> getAllTransactionIdsByStatus(TransactionStatus transactionStatus) {
        if (transactionStatus == null) {
            return null;
        }

        return dbUtils.executeQuery(GET_TRANSACTIONS_BY_STATUS_SQL, getTransactionsByStatus -> {
            Collection<Long> transactionIds = new ArrayList<>();

            getTransactionsByStatus.setLong(1, transactionStatus.getId());
            try (ResultSet transactionsRS = getTransactionsByStatus.executeQuery()) {
                if (transactionsRS != null) {
                    while (transactionsRS.next()) {
                        transactionIds.add(transactionsRS.getLong(TRANSACTION_ID_ROW));
                    }
                }
            }

            return transactionIds;
        }).getResult();
    }

    /**
     * Returns Transaction object by id specified
     *
     * @param id Transaction id
     *
     * @return Trnasaction object with id specified
     */
    public Transaction getTransactionById(Long id) {
        return dbUtils.executeQuery(GET_TRANSACTIONS_BY_ID_SQL, getTransactionById -> {
            getTransactionById.setLong(1, id);
            try (ResultSet transactionRS = getTransactionById.executeQuery()) {
                if (transactionRS != null && transactionRS.first()) {
                    return extractTransactionFromResultSet(transactionRS);
                }
            }

            return null;
        }).getResult();
    }

    /**
     * The method is creating the Transaction object provided. The main idea of the implementation is to make all the
     * operations related to transaction creation in one database's transaction. Operations are:
     * <ul>
     *     <li>Add the transferring amount to blockedAmount from source Bank Account</li>
     *     <li>Create transaction in the database</li>
     * </ul>
     * We are moving the amount into the blocking state but not subtracting it from the balance until transaction will
     * not be executed. We don't execute transaction at the time just to be more consistent in the situation when
     * one instance of the server will be broken.
     *
     * If something goes wrong all changes will be rolled back.
     * Another problem is multithreading. To resolve this problem we are using database locking mechanism
     * <code>SELECT ... FOR UPDATE</code>. All rows returned by this clause will be blocked until transaction will
     * be commited.
     *
     * @param transaction Transaction to be created.
     *
     * @return created transaction with ID generated. null or exception if object has not been created
     *
     * @throws ObjectModificationException will be thrown if balance of the customer will be not enough for the moment.
     */
    public Transaction createTransaction(Transaction transaction) throws ObjectModificationException {
        String INSERT_TRANSACTION_SQL =
                "insert into " + TRANSACTION_TABLE_NAME +
                        " (" +
                        TRANSACTION_FROM_ACCOUNT_ROW + ", " +
                        TRANSACTION_TO_ACCOUNT_ROW + ", " +
                        TRANSACTION_AMOUNT_ROW + ", " +
                        TRANSACTION_CURRENCY_ROW + ", " +
                        TRANSACTION_STATUS_ROW + ", " +
                        FAIL_MESSAGE_ROW + ", " +
                        TRANSACTION_CREATION_DATE_ROW + ", " +
                        TRANSACTION_UPDATE_DATE_ROW +
                        ") values (?, ?, ?, ?, ?, ?, ?, ?)";

        verify(transaction);

        Connection con = H2DataSource.getConnection();

        try {
            BankAccount fromBankAccount = bankAccountDto.
                    getForUpdateBankAccountById(con, transaction.getFromBankAccountId());

            BigDecimal amountToWithdraw = moneyExchangeService.exchange(
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    fromBankAccount.getCurrency()
            );

            //Check that from bank account has enough money
            if (fromBankAccount.getBalance()
                    .compareTo(amountToWithdraw) < 0) {
                throw new ObjectModificationException(ExceptionType.OBJECT_IS_MALFORMED,
                        "The specified bank account could not transfer this amount of money. " +
                                "His balance does not have enough money");
            }

            bankAccountDto.updateBankAccount(fromBankAccount, con);

            transaction = dbUtils.executeQueryInConnection(con, INSERT_TRANSACTION_SQL,
                    new DbUtils.CreationQueryExecutor<>(transaction, TransactionDto::fillInPreparedStatement)).getResult();

            if (transaction == null) {
                throw new ObjectModificationException(ExceptionType.COULD_NOT_OBTAIN_ID);
            }

            con.commit();
        } catch (RuntimeException | SQLException e) {
            DbUtils.safeRollback(con);
            log.error("Unexpected exception", e);
            throw new ImpossibleOperationExecution(e);
        } finally {
            DbUtils.quietlyClose(con);
        }

        return transaction;

    }

    /**
     * Transaction execution method. The logic is to get IN PROGRESS transaction and make all necessary changes in
     * linked bank accounts:
     * <ul>
     *     <li>Subtract the transferring amount from balance from source Bank Account</li>
     *     <li>Subtract the transferring amount from blockedAmount from source Bank Account</li>
     *     <li>Add the transferring amount to balance to target Bank Account</li>
     *     <li>Set SUCCESS status for the transaction</li>
     *     <li>Update all changed objects in the database</li>
     * </ul>
     * The same synchronization logic is used as in {@link #createTransaction(Transaction)}
     * Once transaction execution will be failed it will be marked with FAILED status and failMessage will be added
     * to the transaction
     *
     * @param id Transaction id to execute
     * @throws ObjectModificationException if provided ID will be null or transaction with that ID will be already
     * executed somehow
     */
    public void executeTransaction(Long id) throws ObjectModificationException {
        if (id == null) {
            throw new ObjectModificationException(ExceptionType.OBJECT_IS_MALFORMED,
                    "The specified transaction doesn't exists");
        }

        Connection con = H2DataSource.getConnection();

        Transaction transaction = null;
        try {
            transaction = getForUpdateTransactionById(id, con);

            if (transaction.getStatus() != TransactionStatus.PLANNED) {
                throw new ObjectModificationException(ExceptionType.OBJECT_IS_MALFORMED,
                        "Could not execute transaction which is not in PLANNED status");
            }

            BankAccount fromBankAccount = bankAccountDto.
                    getForUpdateBankAccountById(con, transaction.getFromBankAccountId());

            BankAccount toBankAccount = bankAccountDto.
                    getForUpdateBankAccountById(con, transaction.getToBankAccountId());

            BigDecimal amountToWithdraw = moneyExchangeService.exchange(
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    fromBankAccount.getCurrency()
            );
            BigDecimal newBalance = fromBankAccount.getBalance().subtract(amountToWithdraw);

            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailMessage(String.format("There is no enough money. Current balance is %f",
                        fromBankAccount.getBalance().doubleValue()));
            } else {
                fromBankAccount.setBalance(newBalance);

                bankAccountDto.updateBankAccount(fromBankAccount, con);

                BigDecimal amountToTransfer = moneyExchangeService.exchange(
                        transaction.getAmount(),
                        transaction.getCurrency(),
                        toBankAccount.getCurrency()
                );

                toBankAccount.setBalance(toBankAccount.getBalance().add(amountToTransfer));

                bankAccountDto.updateBankAccount(toBankAccount, con);

                transaction.setStatus(TransactionStatus.SUCCEED);
            }

            updateTransaction(transaction, con);

            con.commit();
        } catch (RuntimeException | SQLException e) {
            DbUtils.safeRollback(con);
            if (transaction != null) {
                transaction.setStatus(TransactionStatus.FAILED);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = String.format("Transaction has been rolled back as it was unexpected exception: %s",
                        sw.toString()).substring(0, 4000);
                transaction.setFailMessage(exceptionAsString);
                updateTransaction(transaction);
            }
            log.error("Unexpected exception", e);
            throw new ImpossibleOperationExecution(e);
        } finally {
            DbUtils.quietlyClose(con);
        }
    }

    /**
     * Returns the Transaction by the ID specified. Method which is not closing the connection once
     * result will be obtained.
     *
     * @param id id of Transaction to be returned
     * @param con the <code>Connection</code> to be used for this query
     *
     * @return the Transaction object with id specified
     */
    private Transaction getForUpdateTransactionById(Long id, Connection con) {
        return dbUtils.executeQueryInConnection(con, GET_TRANSACTIONS_FOR_UPDATE_BY_ID_SQL, getTransaction -> {
            getTransaction.setLong(1, id);
            try (ResultSet transactionRS = getTransaction.executeQuery()) {
                if (transactionRS != null && transactionRS.first()) {
                    return extractTransactionFromResultSet(transactionRS);
                }
            }

            return null;
        }).getResult();
    }

    /**
     * Updates the Transaction with changed parameters <code>status, failMessage and updateDate</code>. The method
     * is private as it should not be used by anyone except this class
     *
     * @param transaction - the object to be updated
     * @throws ObjectModificationException if transaction with the provided id will not be exists in the database at the
     * moment
     */
    private void updateTransaction(Transaction transaction) throws ObjectModificationException {
        updateTransaction(transaction, null);
    }

    /**
     * Special form of {@link #updateTransaction(Transaction)} method which is not closing the connection once result
     * will be obtained
     *
     * @param transaction Transaction object to be updated
     * @param con the <code>Connection</code> to be used for this query
     */
    private void updateTransaction(Transaction transaction, Connection con) throws ObjectModificationException {
        String UPDATE_TRANSACTION_SQL =
                "update " + TRANSACTION_TABLE_NAME +
                        " set " +
                        TRANSACTION_STATUS_ROW + " = ?, " +
                        FAIL_MESSAGE_ROW + " = ?, " +
                        TRANSACTION_UPDATE_DATE_ROW + " = ? " +
                        "where " + TRANSACTION_ID_ROW + " = ?";

        verify(transaction);

        DbUtils.QueryExecutor<Integer> queryExecutor = updateTransaction -> {
            updateTransaction.setInt(1, transaction.getStatus().getId());
            updateTransaction.setString(2, transaction.getFailMessage());
            updateTransaction.setDate(3, new Date(new java.util.Date().getTime()));
            updateTransaction.setLong(4, transaction.getId());

            return updateTransaction.executeUpdate();
        };

        int result;
        if (con == null) {
            result = dbUtils.executeQuery(UPDATE_TRANSACTION_SQL, queryExecutor).getResult();
        } else {
            result = dbUtils.executeQueryInConnection(con, UPDATE_TRANSACTION_SQL, queryExecutor).getResult();
        }

        if (result == 0) {
            throw new ObjectModificationException(ExceptionType.OBJECT_IS_NOT_FOUND);
        }
    }

    /**
     * Verifies the validity of the Transaction object to be saved into the database.
     *
     * @param transaction Transaction object to be validated
     *
     * @throws ObjectModificationException in case of any invalid parameter
     */
    private void verify(Transaction transaction) throws ObjectModificationException {
        if (transaction.getAmount() == null || transaction.getFromBankAccountId() == null ||
                transaction.getToBankAccountId() == null || transaction.getCurrency() == null
                || transaction.getStatus() == null || transaction.getCreationDate() == null
                || transaction.getUpdateDate() == null) {
            throw new ObjectModificationException(ExceptionType.OBJECT_IS_MALFORMED, "Fields could not be NULL");
        }
    }

    /**
     * Fills the provided prepared statement with the Transaction's parameters provided
     *
     * @param preparedStatement prepared statement to be filled in
     * @param transaction the Transaction which should be used to fill in
     */
    private static void fillInPreparedStatement(PreparedStatement preparedStatement, Transaction transaction) {
        try {
            preparedStatement.setLong(1, transaction.getFromBankAccountId());
            preparedStatement.setLong(2, transaction.getToBankAccountId());
            preparedStatement.setBigDecimal(3, transaction.getAmount());
            preparedStatement.setInt(4, transaction.getCurrency().getId());
            preparedStatement.setInt(5, transaction.getStatus().getId());
            preparedStatement.setString(6, transaction.getFailMessage());
            preparedStatement.setDate(7, new java.sql.Date(transaction.getCreationDate().getTime()));
            preparedStatement.setDate(8, new java.sql.Date(transaction.getUpdateDate().getTime()));
        } catch (SQLException e) {
            log.error("Transactions prepared statement could not be initialized by values", e);
        }

    }

    /**
     * The opposite method to {@link #fillInPreparedStatement(PreparedStatement, Transaction)} which is
     * extracts Transaction parameters from the result set
     *
     * @param transactionsRS result set with parameters of the Transaction
     *
     * @return extracted Transaction object
     *
     * @throws SQLException if some parameters in result set will not be found or will have another data type
     */
    private Transaction extractTransactionFromResultSet(ResultSet transactionsRS) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(transactionsRS.getLong(TRANSACTION_ID_ROW));
        transaction.setFromBankAccountId(transactionsRS.getLong(TRANSACTION_FROM_ACCOUNT_ROW));
        transaction.setToBankAccountId(transactionsRS.getLong(TRANSACTION_TO_ACCOUNT_ROW));
        transaction.setAmount(transactionsRS.getBigDecimal(TRANSACTION_AMOUNT_ROW));
        transaction.setCurrency(Currency.valueOf(transactionsRS.getInt(TRANSACTION_CURRENCY_ROW)));
        transaction.setStatus(TransactionStatus.valueOf(transactionsRS.getInt(TRANSACTION_STATUS_ROW)));
        transaction.setFailMessage(transactionsRS.getString(TRANSACTION_STATUS_ROW));
        transaction.setCreationDate(transactionsRS.getDate(TRANSACTION_CREATION_DATE_ROW));
        transaction.setUpdateDate(transactionsRS.getDate(TRANSACTION_UPDATE_DATE_ROW));
        return transaction;
    }
}
