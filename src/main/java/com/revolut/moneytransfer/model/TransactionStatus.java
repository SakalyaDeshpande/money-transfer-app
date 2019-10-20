package com.revolut.moneytransfer.model;

/**
 * The dictionary wrapper related to the database table <code>transaction_status</code>
 */
public enum TransactionStatus {
    PLANNED(1), PROCESSING(2), FAILED(3), SUCCEED(4);

    private int id;

    TransactionStatus(int id) {
        this.id = id;
    }

    public static TransactionStatus valueOf(int id) {
        for(TransactionStatus e : values()) {
            if(e.id == id) return e;
        }

        return null;
    }

    public int getId() {
        return id;
    }
}
