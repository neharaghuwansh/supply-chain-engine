package com.supplychain.engine.config;

import com.supplychain.engine.entity.ExpiryRisk;
import com.supplychain.engine.entity.PricingRule;
import com.supplychain.engine.entity.Role;
import com.supplychain.engine.entity.User;
import com.supplychain.engine.repository.PricingRuleRepository;
import com.supplychain.engine.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeData(
            UserRepository userRepository,
            PricingRuleRepository pricingRuleRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            // Create default Manager
            if (userRepository.findByUsername("manager1").isEmpty()) {

                User manager = new User();

                manager.setUsername("manager1");

                manager.setPassword(
                        passwordEncoder.encode("Manager@123")
                );

                manager.setRole(Role.MANAGER);

                manager.setActive(true);

                userRepository.save(manager);
            }

            // Create default Cashier
            if (userRepository.findByUsername("cashier1").isEmpty()) {

                User cashier = new User();

                cashier.setUsername("cashier1");

                cashier.setPassword(
                        passwordEncoder.encode("Cashier@123")
                );

                cashier.setRole(Role.CASHIER);

                cashier.setActive(true);

                userRepository.save(cashier);
            }

            // Create default pricing rules
            createPricingRule(
                    pricingRuleRepository,
                    ExpiryRisk.LOW,
                    BigDecimal.ZERO
            );

            createPricingRule(
                    pricingRuleRepository,
                    ExpiryRisk.MEDIUM,
                    BigDecimal.TEN
            );

            createPricingRule(
                    pricingRuleRepository,
                    ExpiryRisk.HIGH,
                    BigDecimal.valueOf(20)
            );

            createPricingRule(
                    pricingRuleRepository,
                    ExpiryRisk.CRITICAL,
                    BigDecimal.valueOf(40)
            );

            createPricingRule(
                    pricingRuleRepository,
                    ExpiryRisk.EXPIRED,
                    BigDecimal.valueOf(100)
            );
        };
    }

    private void createPricingRule(
            PricingRuleRepository pricingRuleRepository,
            ExpiryRisk riskLevel,
            BigDecimal discountPercent) {

        if (pricingRuleRepository
                .findByRiskLevel(riskLevel)
                .isEmpty()) {

            PricingRule rule = new PricingRule();

            rule.setRiskLevel(riskLevel);
            rule.setDiscountPercent(discountPercent);
            rule.setActive(true);

            pricingRuleRepository.save(rule);
        }
    }
}