package com.supplychain.engine.controller;

import com.supplychain.engine.dto.BatchRequest;
import com.supplychain.engine.dto.InvoiceConfirmRequest;
import com.supplychain.engine.dto.InvoiceScanResponse;
import com.supplychain.engine.entity.Product;
import com.supplychain.engine.service.BatchService;
import com.supplychain.engine.service.InvoiceAiService;
import com.supplychain.engine.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceAiService invoiceAiService;
    private final ProductService productService;
    private final BatchService batchService;

    public InvoiceController(
            InvoiceAiService invoiceAiService,
            ProductService productService,
            BatchService batchService) {

        this.invoiceAiService = invoiceAiService;
        this.productService = productService;
        this.batchService = batchService;
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping(
            value = "/scan",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public InvoiceScanResponse scanInvoice(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invoice file cannot be empty"
            );
        }

        try {
            String contentType = file.getContentType();

            if (contentType == null) {
                throw new IllegalArgumentException(
                        "Could not determine invoice file type"
                );
            }

            if (!contentType.equals("application/pdf")
                    && !contentType.equals("image/jpeg")
                    && !contentType.equals("image/png")
                    && !contentType.equals("image/webp")) {

                throw new IllegalArgumentException(
                        "Unsupported invoice file type. " +
                                "Only PDF, JPEG, PNG and WEBP files are allowed."
                );
            }

            return invoiceAiService.extractInvoiceData(
                    file.getBytes(),
                    contentType
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not read the invoice file",
                    e
            );
        }
    }

    @PreAuthorize("hasRole('MANAGER')")
    @Transactional
    @PostMapping("/confirm")
    public String confirmInvoice(
            @Valid @RequestBody InvoiceConfirmRequest request) {

        LocalDate today = LocalDate.now();

        if (request.getInvoiceNumber() == null
                || request.getInvoiceNumber().isBlank()) {

            throw new IllegalArgumentException(
                    "Invoice number is required"
            );
        }

        if (request.getManufacturingDate() != null
                && request.getExpiryDate()
                .isBefore(request.getManufacturingDate())) {

            throw new IllegalArgumentException(
                    "Expiry date cannot be before manufacturing date"
            );
        }

        if (request.getExpiryDate().isBefore(today)) {
            throw new IllegalArgumentException(
                    "Cannot confirm an already expired batch"
            );
        }

        if (request.getReceivedDate() != null
                && request.getReceivedDate().isAfter(today)) {

            throw new IllegalArgumentException(
                    "Received date cannot be in the future"
            );
        }

        Product product = new Product();

        product.setName(request.getProductName());
        product.setSku(request.getSku());
        product.setCategory(request.getCategory());
        product.setUnit(request.getUnit());
        product.setBaseSellingPrice(
                request.getBaseSellingPrice()
        );
        product.setReorderThreshold(20);
        product.setActive(true);

        Product savedProduct =
                productService.createProduct(product);

        BatchRequest batchRequest = new BatchRequest();

        batchRequest.setProductId(savedProduct.getId());

        batchRequest.setBatchNumber(
                request.getBatchNumber()
        );

        batchRequest.setManufacturingDate(
                request.getManufacturingDate()
        );

        batchRequest.setExpiryDate(
                request.getExpiryDate()
        );

        batchRequest.setQuantity(
                request.getQuantity()
        );

        batchRequest.setPurchasePrice(
                request.getPurchasePrice()
        );

        batchRequest.setReceivedDate(
                request.getReceivedDate() != null
                        ? request.getReceivedDate()
                        : today
        );

        batchService.createBatch(batchRequest);

        return "Invoice " + request.getInvoiceNumber()
                + " confirmed successfully. "
                + "Product and batch created.";
    }
}