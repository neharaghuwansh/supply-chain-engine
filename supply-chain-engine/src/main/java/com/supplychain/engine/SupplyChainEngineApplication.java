package com.supplychain.engine;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@EnableScheduling
@SpringBootApplication
public class SupplyChainEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(SupplyChainEngineApplication.class, args);
	}

}
