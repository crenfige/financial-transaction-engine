package com.engine.financial_transaction_engine;

import org.springframework.boot.SpringApplication;

public class TestFinancialTransactionEngineApplication {

	public static void main(String[] args) {
		SpringApplication.from(FinancialTransactionEngineApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
