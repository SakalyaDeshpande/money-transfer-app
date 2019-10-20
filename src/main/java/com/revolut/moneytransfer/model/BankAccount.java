package com.revolut.moneytransfer.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Random;

/**
 * Bank Account entity model. Relates to the database table <code>bank_account</code>. Defines the bank account of
 * individual with <code>ownerName</code>. It has <code>balance</code> in specific money <code>currency</code>. Once
 * there is any PLANNED transferring transaction in the system relates to this Bank Account, the transaction amount is
 * reserved in <code>blockedAmount</code> field
 */
public class BankAccount implements ModelHasId{
    private Long id;

    @NotNull
    @Size(min=1)
    private String accountHolderName;

    private BigDecimal balance;

    @NotNull
    private Currency currency;

    public BankAccount() {
    }

    public BankAccount(String accountHolderName, BigDecimal balance, Currency currency) {
        this(new Random().nextLong(), accountHolderName, balance, currency);
    }

    public BankAccount(Long id, String accountHolderName, BigDecimal balance, Currency currency) {
        this.id = id;
        this.accountHolderName = accountHolderName;
        this.balance = balance;
        this.currency = currency;
    }

    public BankAccount(Long id, String accountHolderName) {
        this.id = id;
        this.accountHolderName = accountHolderName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankAccount that = (BankAccount) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
