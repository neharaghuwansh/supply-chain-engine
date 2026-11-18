package com.supplychain.engine.service;

import com.supplychain.engine.dto.InvoiceScanResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

@Service
public class InvoiceAiService {

    private final ChatClient chatClient;

    public InvoiceAiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public InvoiceScanResponse extractInvoiceData(
            byte[] fileData,
            String contentType) {

        String prompt = """
                You are an invoice data extraction assistant for a
                supply-chain and inventory management system.

                Extract ONLY information actually present in the invoice.

                Extract these fields:

                invoiceNumber
                supplierName
                invoiceDate
                productName
                sku
                batchNumber
                manufacturingDate
                expiryDate
                quantity
                purchasePrice

                Rules:
                1. Never invent or guess information.
                2. Return null when a field is not present.
                3. Dates must use yyyy-MM-dd.
                4. quantity must be a whole number.
                5. purchasePrice must contain only the numeric price.
                6. Return ONLY valid JSON matching the requested object.
                """;

        Media media = new Media(
                MimeType.valueOf(contentType),
                new ByteArrayResource(fileData)
        );

        return chatClient
                .prompt()
                .user(user -> user
                        .text(prompt)
                        .media(media))
                .call()
                .entity(
                        InvoiceScanResponse.class,
                        spec -> spec.validateSchema()
                );
    }
}