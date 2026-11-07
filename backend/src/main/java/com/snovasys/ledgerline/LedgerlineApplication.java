package com.snovasys.ledgerline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class LedgerlineApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerlineApplication.class, args);
    }
}
