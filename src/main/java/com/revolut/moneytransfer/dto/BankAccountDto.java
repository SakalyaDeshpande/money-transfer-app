package com.revolut.moneytransfer.dto;

import com.revolut.moneytransfer.db.DbUtils;
import com.revolut.moneytransfer.exceptions.ObjectModificationException;
import com.revolut.moneytransfer.model.BankAccount;
import com.revolut.moneytransfer.model.Currency;
import com.revolut.moneytransfer.model.ExceptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Encapsulates all logic for Bank Account entity which is related to the database. Implements the singleton pattern.
 */
public class BankAccountDto {
    private static final String BANK_ACCOUNT_TABLE_NAME = "bank_account";
    private static final String BANK_ACCOUNT_ID_ROW = "id";
    private static final String BANK_ACCOUNT_HOLDER_NAME_ROW = "account_holder_name";
    private static final String BANK_ACCOUNT_BALANCE_ROW = "balance";
    private static final String BANK_ACCOUNT_CURRENCY_ID_ROW = "currency_id";

    private static final Logger log = LoggerFactory.getLogger(BankAccountDto.class);

    private static final BankAccountDto bas = new BankAccountDto();
    private DbUtils dbUtils = DbUtils.getInstance();

    private BankAccountDto() {
    }

    public static BankAccountDto getInstance() {
        return bas;
    }

    /**
     * @return All Bank Accounts which is exists in the database at the moment
     * <p>
     * TODO: add multipaging
     */
    public Collection<BankAccount> getAllBankAccounts() {
        return dbUtils.executeQuery("select * from " + BANK_ACCOUNT_TABLE_NAME, getBankAccounts -> {
            Collection<BankAccount> bankAccounts = new ArrayList<>();

            try (ResultSet bankAccountsRS = getBankAccounts.executeQuery()) {
                if (bankAccountsRS != null) {
                    while (bankAccountsRS.next()) {
                        bankAccounts.add(extractBankAccountFromResultSet(bankAccountsRS));
                    }
                }
            }

            return bankAccounts;
        }).getResult();
    }

    /**
     * Returns Bank Account object by id specified
     *
     * @param id Bank Account object id
     * @return Bank Account object with id specified
     */
    public BankAccount getBankAccountById(Long id) {
        String GET_BANK_ACCOUNT_BY_ID_SQL =
                "select * from " + BANK_ACCOUNT_TABLE_NAME + " ba " +
                        "where ba." + BANK_ACCOUNT_ID_ROW + " = ?";

        return dbUtils.executeQuery(GET_BANK_ACCOUNT_BY_ID_SQL, getBankAccount -> {
            getBankAccount.setLong(1, id);
            try (ResultSet bankAccountRS = getBankAccount.executeQuery()) {
                if (bankAccountRS != null && bankAccountRS.first()) {
                    return extractBankAccountFromResultSet(bankAccountRS);
                }
            }

            return null;
        }).getResult();
    }

    /**
     * Special form of {@link #getBankAccountById(Long)} method which is not closing the connection once result
     * will be obtained. We are using it only inside the related <code>TransactionDto</code>
     *
     * @param id  Bank Account object id
     * @param con the <code>Connection</code> to be used for this query
     */
    BankAccount getForUpdateBankAccountById(Connection con, Long id) {
        String GET_BANK_ACCOUNT_BY_ID_SQL =
                "select * from " + BANK_ACCOUNT_TABLE_NAME + " ba " +
                        "where ba." + BANK_ACCOUNT_ID_ROW + " = ? " +
                        "for update";

        return dbUtils.executeQueryInConnection(con, GET_BANK_ACCOUNT_BY_ID_SQL, getBankAccount -> {
            getBankAccount.setLong(1, id);
            try (ResultSet bankAccountRS = getBankAccount.executeQuery()) {
                if (bankAccountRS != null && bankAccountRS.first()) {
                    return extractBankAccountFromResultSet(bankAccountRS);
                }
            }

            return null;
        }).getResult();
    }

    /**
     * Updates the Bank Account with changed parameters using the id provided by the object passed. Only ownerName
     * parameter will be updated.
     *
     * @param bankAccount - the object to be updated
     * @throws ObjectModificationException if Bank Account with the provided id will not be exists in the database at
     *                                     the moment or object provided is malformed
     */
    public void updateBankAccountSafe(BankAccount bankAccount) throws ObjectModificationException {
        String UPDATE_BANK_ACCOUNT_SQL =
                "update " + BANK_ACCOUNT_TABLE_NAME +
                        " set " +
                        BANK_ACCOUNT_HOLDER_NAME_ROW + " = ? " +
                        "where " + BANK_ACCOUNT_ID_ROW + " = ?";

        if (bankAccount.getId() == null || bankAccount.getAccountHolderName() == null) {
            throw new ObjectModificationException(ExceptionType.OBJECT_IS_MALFORMED, "Id and OwnerName fields could not be NULL");
        }

        DbUtils.QueryExecutor<Integer> queryExecutor = updateBankAccount -> {
            updateBankAccount.setString(1, bankAccount.getAccountHolderName());
            updateBankAccount.setLong(2, bankAccount.getId());

            return updateBankAccount.executeUpdate();
        };

        int result = dbUtils.executeQuery(UPDATE_BANK_ACCOUNT_SQL, queryExecutor).getResult();

        if (result == 0) {
            throw new ObjectModificationException(ExceptionType.OBJECT_IS_NOT_FOUND);
        }
    }

    /**
     * Updates the Bank Account with changed parameters using the id provided by the object passed.
     * We are using it only inside the related <code>TransactionDto</code>
     *
     * @param bankAccount Bank Account object which will be updated
     * @param con         the <code>Connection</code> to be used for this query
     * @throws ObjectModificationException if Bank Account with the provided id will not be exists in the database at the
     *                                     moment or object provided is malformed
     */
    void updateBankAccount(BankAccount bankAccount, Connection con) throws ObjectModificationException {
        String UPDATE_BANK_ACCOUNT_SQL =
                "update " + BANK_ACCOUNT_TABLE_NAME +
                        " set " +
                        BANK_ACCOUNT_HOLDER_NAME_ROW + " = ?, " +
                        BANK_ACCOUNT_BALANCE_ROW + " = ?, " +
                        BANK_ACCOUNT_CURRENCY_ID_ROW + " = ? " +
                        "where " + BANK_ACCOUNT_ID_ROW + " = ?";

        verify(bankAccount);

        DbUtils.QueryExecutor<Integer> queryExecutor = updateBankAccount -> {
            fillInPreparedStatement(updateBankAccount, bankAccount);
            updateBankAccount.setLong(4, bankAccount.getId());

            return updateBankAccount.executeUpdate();
        };

        int result;
        if (con == null) {
            result = dbUtils.executeQuery(UPDATE_BANK_ACCOUNT_SQL, queryExecutor).getResult();
        } else {
            result = dbUtils.executeQueryInConnection(con, UPDATE_BANK_ACCOUNT_SQL, queryExecutor).getResult();
        }

        if (result == 0) {
            throw new ObjectModificationException(ExceptionType.OBJECT_IS_NOT_FOUND);
        }
    }

    /**
     * Creates the Bank Account object provided in the database. Id of this objects will not be used. It will be
     * generated and returned in the result of the method.
     *
     * @param bankAccount Bank Account object which should be created
     * @return created Bank Account object with ID specified'
     * @throws ObjectModificationException if Bank Account with the provided id will not be exists in the database at the
     *                                     moment or object provided is malformed
     */
    public BankAccount createBankAccount(BankAccount bankAccount) throws ObjectModificationException {
        String INSERT_BANK_ACCOUNT_SQL =
                "insert into " + BANK_ACCOUNT_TABLE_NAME +
                        " (" +
                        BANK_ACCOUNT_HOLDER_NAME_ROW + ", " +
                        BANK_ACCOUNT_BALANCE_ROW + ", " +
                        BANK_ACCOUNT_CURRENCY_ID_ROW +
                        ") values (?, ?, ?)";

        verify(bankAccount);

        bankAccount = dbUtils.executeQuery(INSERT_BANK_ACCOUNT_SQL,
                new DbUtils.CreationQueryExecutor<>(bankAccount, BankAccountDto::fillInPreparedStatement)).getResult();

        if (bankAccount == null) {
            throw new ObjectModificationException(ExceptionType.COULD_NOT_OBTAIN_ID);
        }

        return bankAccount;
    }

    /**
     * The opposite method to {@link #fillInPreparedStatement(PreparedStatement, BankAccount)} which is
     * extracts Bank Account parameters from the result set
     *
     * @param bankAccountsRS result set with parameters of the Bank Account
     * @return extracted Bank Account object
     * @throws SQLException if some parameters in result set will not be found or will have non compatible
     *                      data type
     */
    private BankAccount extractBankAccountFromResultSet(ResultSet bankAccountsRS) throws SQLException {
        BankAccount bankAccount = new BankAccount();
        bankAccount.setId(bankAccountsRS.getLong(BANK_ACCOUNT_ID_ROW));
        bankAccount.setAccountHolderName(bankAccountsRS.getString(BANK_ACCOUNT_HOLDER_NAME_ROW));
        bankAccount.setBalance(bankAccountsRS.getBigDecimal(BANK_ACCOUNT_BALANCE_ROW));
        bankAccount.setCurrency(Currency.valueOf(bankAccountsRS.getInt(BANK_ACCOUNT_CURRENCY_ID_ROW)));

        return bankAccount;
    }

    /**
     * Verifies the validity of the Bank Account object to be saved into the database.
     *
     * @param bankAccount Bank Account object to be validated
     * @throws ObjectModificationException in case of any invalid parameter
     */
    private void verify(BankAccount bankAccount) throws ObjectModificationException {
        /*if (bankAccount.getId() == null) {
            throw new ObjectModificationException(ExceptionType.OBJECT_IS_MALFORMED,
                    "ID value is invalid");
        }
*/
        if (bankAccount.getAccountHolderName() == null || bankAccount.getBalance() == null ||
                 bankAccount.getCurrency() == null) {
            throw new ObjectModificationException(ExceptionType.OBJECT_IS_MALFORMED, "Fields could not be NULL");
        }
    }

    /**
     * Fills the provided prepared statement with the Bank Account's parameters provided
     *
     * @param preparedStatement prepared statement to be filled in
     * @param bankAccount       the Bank Account object which should be used to fill in
     */
    private static void fillInPreparedStatement(PreparedStatement preparedStatement, BankAccount bankAccount) {
        try {
            preparedStatement.setString(1, bankAccount.getAccountHolderName());
            preparedStatement.setBigDecimal(2, bankAccount.getBalance());
            preparedStatement.setLong(3, bankAccount.getCurrency().getId());
        } catch (SQLException e) {
            log.error("BankAccount prepared statement could not be initialized by values", e);
        }
    }
}
