package com.snovasys.ledgerline.dto;

import java.util.ArrayList;
import java.util.List;

public class AccountStatementResponse {
    private AccountDto account;
    private List<LedgerEntryDto> entries = new ArrayList<>();

    public AccountStatementResponse() {}

    public AccountStatementResponse(AccountDto account, List<LedgerEntryDto> entries) {
        this.account = account;
        this.entries = entries;
    }

    public AccountDto getAccount() { return account; }
    public void setAccount(AccountDto account) { this.account = account; }

    public List<LedgerEntryDto> getEntries() { return entries; }
    public void setEntries(List<LedgerEntryDto> entries) { this.entries = entries; }
}
