package com.revolut.moneytransfer.service;

import com.revolut.moneytransfer.dto.BankAccountDto;
import com.revolut.moneytransfer.exceptions.ObjectModificationException;
import com.revolut.moneytransfer.model.BankAccount;

import java.util.Collection;

/**
 * Right now the proxy service under the {@link BankAccountDto}. Should be used to abstract the presentation layer
 * from the persistence layer
 */
public class BankAccountService {
    private static final BankAccountService bas = new BankAccountService();

    public static BankAccountService getInstance() {
        return bas;
    }

    public Collection<BankAccount> getAllBankAccounts() {
        return BankAccountDto.getInstance().getAllBankAccounts();
    }

    public BankAccount getBankAccountById(Long id) {
        return BankAccountDto.getInstance().getBankAccountById(id);
    }

    public void updateBankAccount(BankAccount bankAccount) throws ObjectModificationException {
        BankAccountDto.getInstance().updateBankAccountSafe(bankAccount);
    }

    public BankAccount createBankAccount(BankAccount bankAccount) throws ObjectModificationException {
        return BankAccountDto.getInstance().createBankAccount(bankAccount);
    }
}
