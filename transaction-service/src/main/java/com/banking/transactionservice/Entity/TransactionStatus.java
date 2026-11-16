package com.banking.transactionservice.Entity;

/**
 * Transaction Lifecylce flow
 * Pending -> Processing -> Completed (Clean transaction -without fraud)
 *                       -> Pending_Verification(Suspicious Detected)
 *                                 -> Completed(Verified)
 *                                 -> Flagged(SAGA Refund)
 *                       -> Failed
 *                       ->Flagged
 */
public enum TransactionStatus {
    PENDING,
    PROCESSING,
    PENDING_VERIFICATION,
    COMPLETED,
    FAILED,
    FLAGGED

}
