import { useRef, useState } from "react";
import api from "../services/api";
import "./Invoices.css";

const initialInvoice = {
    invoiceNumber: "",
    supplierName: "",
    invoiceDate: "",
    productName: "",
    sku: "",
    category: "General",
    unit: "piece",
    baseSellingPrice: "",
    batchNumber: "",
    manufacturingDate: "",
    expiryDate: "",
    quantity: "",
    purchasePrice: "",
    receivedDate: new Date().toISOString().slice(0, 10),
};

function Invoices() {
    const [file, setFile] = useState(null);
    const [invoice, setInvoice] = useState(initialInvoice);
    const [scanning, setScanning] = useState(false);
    const [saving, setSaving] = useState(false);
    const [scanned, setScanned] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const fileInputRef = useRef(null);

    function handleChange(event) {
        const { name, value } = event.target;

        setInvoice((previous) => ({
            ...previous,
            [name]: value,
        }));
    }

    function handleFileChange(event) {
        const selectedFile = event.target.files?.[0] ?? null;

        setFile(selectedFile);
        setScanned(false);
        setMessage("");
        setError("");
    }

    async function handleScan(event) {
        event.preventDefault();

        if (scanning || saving) return;

        if (!file) {
            setError("Please select an invoice file first.");
            return;
        }

        const allowedTypes = [
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp",
        ];

        const allowedExtensions = /\.(pdf|jpe?g|png|webp)$/i;

        if (
            !allowedTypes.includes(file.type) &&
            !allowedExtensions.test(file.name)
        ) {
            setError("Please upload a PDF, JPG, PNG, or WEBP invoice.");
            return;
        }

        const maxFileSize = 10 * 1024 * 1024;

        if (file.size > maxFileSize) {
            setError("The invoice file must be smaller than 10 MB.");
            return;
        }

        setScanning(true);
        setError("");
        setMessage("");

        try {
            const formData = new FormData();
            formData.append("file", file);

            const response = await api.post(
                "/invoices/scan",
                formData
            );

            const result = response.data;

            setInvoice((previous) => ({
                ...previous,
                invoiceNumber: result.invoiceNumber ?? previous.invoiceNumber,
                supplierName: result.supplierName ?? previous.supplierName,
                invoiceDate: result.invoiceDate ?? previous.invoiceDate,
                productName: result.productName ?? previous.productName,
                sku: result.sku ?? previous.sku,
                category: result.category ?? previous.category,
                unit: result.unit ?? previous.unit,
                baseSellingPrice:
                    result.baseSellingPrice ?? previous.baseSellingPrice,
                batchNumber: result.batchNumber ?? previous.batchNumber,
                manufacturingDate:
                    result.manufacturingDate ?? previous.manufacturingDate,
                expiryDate: result.expiryDate ?? previous.expiryDate,
                quantity: result.quantity ?? previous.quantity,
                purchasePrice: result.purchasePrice ?? previous.purchasePrice,
                receivedDate:
                    result.receivedDate ??
                    result.invoiceDate ??
                    previous.receivedDate,
            }));

            setScanned(true);
            setMessage(
                "Invoice scanned successfully. Review every field before confirming."
            );
        } catch (err) {
            setScanned(false);
            setError(
                err.response?.data?.message ||
                "Invoice scanning failed. You can enter the invoice details manually."
            );
        } finally {
            setScanning(false);
        }
    }

    async function handleConfirm(event) {
        event.preventDefault();

        if (saving || scanning) return;

        setError("");
        setMessage("");

        const payload = {
            invoiceNumber: invoice.invoiceNumber.trim(),
            supplierName: invoice.supplierName.trim(),
            invoiceDate: invoice.invoiceDate || null,
            productName: invoice.productName.trim(),
            sku: invoice.sku.trim(),
            category: invoice.category.trim(),
            unit: invoice.unit.trim(),
            baseSellingPrice: Number(invoice.baseSellingPrice),
            batchNumber: invoice.batchNumber.trim(),
            manufacturingDate: invoice.manufacturingDate || null,
            expiryDate: invoice.expiryDate,
            quantity: Number(invoice.quantity),
            purchasePrice: Number(invoice.purchasePrice),
            receivedDate: invoice.receivedDate,
        };

        if (
            !payload.productName ||
            !payload.sku ||
            !payload.category ||
            !payload.unit ||
            !payload.batchNumber ||
            !payload.expiryDate ||
            !payload.receivedDate
        ) {
            setError("Please complete all required product and batch fields.");
            return;
        }

        if (
            !Number.isFinite(payload.baseSellingPrice) ||
            payload.baseSellingPrice <= 0
        ) {
            setError("Base selling price must be greater than zero.");
            return;
        }

        if (
            !Number.isInteger(payload.quantity) ||
            payload.quantity <= 0
        ) {
            setError("Quantity must be a positive whole number.");
            return;
        }

        if (
            !Number.isFinite(payload.purchasePrice) ||
            payload.purchasePrice <= 0
        ) {
            setError("Purchase price must be greater than zero.");
            return;
        }

        if (
            payload.manufacturingDate &&
            payload.manufacturingDate > payload.expiryDate
        ) {
            setError(
                "Manufacturing date cannot be later than the expiry date."
            );
            return;
        }

        if (payload.receivedDate > payload.expiryDate) {
            setError(
                "Received date cannot be later than the expiry date."
            );
            return;
        }

        if (
            payload.manufacturingDate &&
            payload.receivedDate < payload.manufacturingDate
        ) {
            setError(
                "Received date cannot be earlier than the manufacturing date."
            );
            return;
        }

        setSaving(true);

        try {
            await api.post("/invoices/confirm", payload);

            setMessage(
                "Invoice confirmed successfully. Product and batch records have been created."
            );

            setInvoice({
                ...initialInvoice,
                receivedDate: new Date().toISOString().slice(0, 10),
            });

            setFile(null);
            setScanned(false);

            if (fileInputRef.current) {
                fileInputRef.current.value = "";
            }
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to confirm the invoice. Check the details and try again."
            );
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="invoices-page">
            <header className="invoices-header">
                <div>
                    <span className="invoices-eyebrow">
                        INTELLIGENT DOCUMENT PROCESSING
                    </span>

                    <h1>AI Invoice Scanner</h1>

                    <p>
                        Extract supplier, product and batch details from
                        invoices and review them before adding inventory.
                    </p>
                </div>

                <div className="invoice-ai-badge">
                    <span className="invoice-ai-dot"></span>
                    AI Powered
                </div>
            </header>

            <section className="invoice-workflow">
                <div className="workflow-step active">
                    <span>1</span>
                    <div>
                        <strong>Upload</strong>
                        <small>Select invoice</small>
                    </div>
                </div>

                <div className="workflow-line"></div>

                <div className={`workflow-step ${scanned ? "active" : ""}`}>
                    <span>2</span>
                    <div>
                        <strong>Review</strong>
                        <small>Verify extracted data</small>
                    </div>
                </div>

                <div className="workflow-line"></div>

                <div className="workflow-step">
                    <span>3</span>
                    <div>
                        <strong>Confirm</strong>
                        <small>Create inventory</small>
                    </div>
                </div>
            </section>

            <section className="invoice-upload-card">
                <div className="invoice-section-heading">
                    <div>
                        <h2>Upload supplier invoice</h2>
                        <p>Supported formats: PDF, JPG, PNG and WEBP. Maximum 10 MB.</p>
                    </div>

                    <span className="invoice-step-label">STEP 01</span>
                </div>

                <form onSubmit={handleScan}>
                    <label
                        className="invoice-dropzone"
                        htmlFor="invoice-file"
                    >
                        <div className="invoice-upload-icon">↑</div>

                        <strong>
                            {file ? file.name : "Choose an invoice file"}
                        </strong>

                        <span>
                            {file
                                ? `${(file.size / 1024).toFixed(1)} KB selected`
                                : "Click here to browse your computer"}
                        </span>

                        <input
                            ref={fileInputRef}
                            id="invoice-file"
                            type="file"
                            accept=".pdf,.jpg,.jpeg,.png,.webp,application/pdf,image/jpeg,image/png,image/webp"
                            onChange={handleFileChange}
                        />
                    </label>

                    <button
                        type="submit"
                        className="invoice-scan-button"
                        disabled={scanning || saving || !file}
                    >
                        {scanning ? "Scanning invoice..." : "✦ Scan with AI"}
                    </button>
                </form>

                <div className="invoice-manual-note">
                    <strong>AI unavailable?</strong>
                    <span>
                        You can still enter the invoice details manually in
                        the review form below.
                    </span>
                </div>
            </section>

            {error && (
                <div
                    className="invoice-alert invoice-alert-error"
                    role="alert"
                >
                    <strong>Attention</strong>
                    <span>{error}</span>
                </div>
            )}

            {message && (
                <div
                    className="invoice-alert invoice-alert-success"
                    role="status"
                >
                    <strong>{scanned ? "Scan completed" : "Success"}</strong>
                    <span>{message}</span>
                </div>
            )}

            <section className="invoice-review-card">
                <div className="invoice-section-heading">
                    <div>
                        <h2>Review invoice details</h2>
                        <p>
                            Verify AI-extracted values or fill in the fields
                            manually before confirming.
                        </p>
                    </div>

                    <span className="invoice-step-label">STEP 02</span>
                </div>

                <form onSubmit={handleConfirm}>
                    <div className="invoice-form-section">
                        <h3>Invoice information</h3>

                        <div className="invoice-form-grid">
                            <label>
                                Invoice Number
                                <input
                                    name="invoiceNumber"
                                    value={invoice.invoiceNumber}
                                    onChange={handleChange}
                                    placeholder="e.g. INV-2026-1001"
                                    maxLength={100}
                                />
                            </label>

                            <label>
                                Supplier Name
                                <input
                                    name="supplierName"
                                    value={invoice.supplierName}
                                    onChange={handleChange}
                                    placeholder="Supplier name"
                                    maxLength={150}
                                />
                            </label>

                            <label>
                                Invoice Date
                                <input
                                    type="date"
                                    name="invoiceDate"
                                    value={invoice.invoiceDate}
                                    onChange={handleChange}
                                />
                            </label>

                            <label>
                                Stock Received Date *
                                <input
                                    type="date"
                                    name="receivedDate"
                                    value={invoice.receivedDate}
                                    onChange={handleChange}
                                    required
                                />
                            </label>
                        </div>
                    </div>

                    <div className="invoice-form-section">
                        <h3>Product information</h3>

                        <div className="invoice-form-grid">
                            <label>
                                Product Name *
                                <input
                                    name="productName"
                                    value={invoice.productName}
                                    onChange={handleChange}
                                    maxLength={150}
                                    required
                                />
                            </label>

                            <label>
                                SKU *
                                <input
                                    name="sku"
                                    value={invoice.sku}
                                    onChange={handleChange}
                                    maxLength={100}
                                    required
                                />
                            </label>

                            <label>
                                Category *
                                <input
                                    name="category"
                                    value={invoice.category}
                                    onChange={handleChange}
                                    maxLength={100}
                                    required
                                />
                            </label>

                            <label>
                                Unit *
                                <select
                                    name="unit"
                                    value={invoice.unit}
                                    onChange={handleChange}
                                    required
                                >
                                    <option value="piece">Piece</option>
                                    <option value="box">Box</option>
                                    <option value="pack">Pack</option>
                                    <option value="bottle">Bottle</option>
                                    <option value="kg">Kilogram</option>
                                    <option value="litre">Litre</option>
                                </select>
                            </label>

                            <label>
                                Base Selling Price (₹) *
                                <input
                                    type="number"
                                    name="baseSellingPrice"
                                    min="0.01"
                                    step="0.01"
                                    value={invoice.baseSellingPrice}
                                    onChange={handleChange}
                                    required
                                />
                            </label>
                        </div>
                    </div>

                    <div className="invoice-form-section">
                        <h3>Batch and expiry information</h3>

                        <div className="invoice-form-grid">
                            <label>
                                Batch Number *
                                <input
                                    name="batchNumber"
                                    value={invoice.batchNumber}
                                    onChange={handleChange}
                                    maxLength={100}
                                    required
                                />
                            </label>

                            <label>
                                Quantity Received *
                                <input
                                    type="number"
                                    name="quantity"
                                    min="1"
                                    step="1"
                                    value={invoice.quantity}
                                    onChange={handleChange}
                                    required
                                />
                            </label>

                            <label>
                                Purchase Price per Unit (₹) *
                                <input
                                    type="number"
                                    name="purchasePrice"
                                    min="0.01"
                                    step="0.01"
                                    value={invoice.purchasePrice}
                                    onChange={handleChange}
                                    required
                                />
                            </label>

                            <label>
                                Manufacturing Date
                                <input
                                    type="date"
                                    name="manufacturingDate"
                                    value={invoice.manufacturingDate}
                                    onChange={handleChange}
                                    max={invoice.expiryDate || undefined}
                                />
                            </label>

                            <label>
                                Expiry Date *
                                <input
                                    type="date"
                                    name="expiryDate"
                                    value={invoice.expiryDate}
                                    onChange={handleChange}
                                    min={invoice.manufacturingDate || undefined}
                                    required
                                />
                            </label>
                        </div>
                    </div>

                    <div className="invoice-review-footer">
                        <div>
                            <strong>Final verification</strong>
                            <span>
                                Confirming creates the product and received
                                batch in your inventory.
                            </span>
                        </div>

                        <button
                            type="submit"
                            className="invoice-confirm-button"
                            disabled={saving || scanning}
                        >
                            {saving
                                ? "Confirming..."
                                : "✓ Confirm & Add Inventory"}
                        </button>
                    </div>
                </form>
            </section>
        </div>
    );
}

export default Invoices;