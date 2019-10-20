package com.revolut.moneytransfer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * Transaction entity model. Relates to the database table <code>transaction</code>. Defines the transferring transaction
 * which is initialized by <code>fromBankAccount</code> who wants to transfer money to the <code>toBankAccount</code>
 * of <code>amount</code> in <code>currency</code> currency. Additionally this class controls the creation and last
 * update dates alongside with the actual {@link TransactionStatus} <code>status</code>  and <code>failMessage</code> in case of FAIL status.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction implements ModelHasId{
    private Long id;
    private Long fromBankAccountId;
    private Long toBankAccountId;
    private BigDecimal amount;
    private Currency currency;
    private Date creationDate;
    private Date updateDate;
    private TransactionStatus status;
    private String failMessage;

    public Transaction() {
        this.creationDate = new Date();
        this.updateDate = new Date();
        this.status = TransactionStatus.PLANNED;
        this.failMessage = "";
    }

    public Transaction(Long fromBankAccountId, Long toBankAccountId, BigDecimal amount, Currency currency) {
        this();
        this.fromBankAccountId = fromBankAccountId;
        this.toBankAccountId = toBankAccountId;
        this.amount = amount;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFromBankAccountId() {
        return fromBankAccountId;
    }

    public void setFromBankAccountId(Long fromBankAccountId) {
        this.fromBankAccountId = fromBankAccountId;
    }

    public Long getToBankAccountId() {
        return toBankAccountId;
    }

    public void setToBankAccountId(Long toBankAccountId) {
        this.toBankAccountId = toBankAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(String failMessage) {
        this.failMessage = failMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
