import { useEffect, useState } from "react";
import api from "../services/api";
import "./Batches.css";

function Batches() {
    const [batches, setBatches] = useState([]);
    const [products, setProducts] = useState([]);
    const [risks, setRisks] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [showForm, setShowForm] = useState(false);
    const [saving, setSaving] = useState(false);
    const [editingBatchId, setEditingBatchId] = useState(null);
    const [archivingBatchId, setArchivingBatchId] = useState(null);
    const [restoringBatchId, setRestoringBatchId] = useState(null);

    const initialForm = {
        productId: "",
        batchNumber: "",
        manufacturingDate: "",
        expiryDate: "",
        quantity: "",
        purchasePrice: "",
        receivedDate: new Date().toISOString().slice(0, 10),
    };

    const [form, setForm] = useState(initialForm);

    async function loadData() {
        setLoading(true);
        setError("");

        try {
            const [batchResponse, productResponse] = await Promise.all([
                api.get("/batches"),
                api.get("/products"),
            ]);

            const loadedBatches = Array.isArray(batchResponse.data)
                ? batchResponse.data
                : [];

            const loadedProducts = Array.isArray(productResponse.data)
                ? productResponse.data
                : [];

            setBatches(loadedBatches);
            setProducts(loadedProducts);

            const riskResults = await Promise.all(
                loadedBatches.map(async (batch) => {
                    try {
                        const response = await api.get(
                            `/batches/${batch.id}/expiry-risk`
                        );

                        return [batch.id, response.data];
                    } catch {
                        return [batch.id, null];
                    }
                })
            );

            setRisks(Object.fromEntries(riskResults));
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load batches. Please try again."
            );
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadData();
    }, []);

    function handleChange(event) {
        const { name, value } = event.target;

        setForm((previous) => ({
            ...previous,
            [name]: value,
        }));
    }

    function resetForm() {
        setForm({
            ...initialForm,
            receivedDate: new Date().toISOString().slice(0, 10),
        });
    }

    function closeForm() {
        setShowForm(false);
        setEditingBatchId(null);
        resetForm();
    }

    function getBatchProductId(batch) {
        return batch.product?.id ?? batch.productId;
    }

    function getBatchProductName(batch) {
        return batch.product?.name ?? "—";
    }

    function isArchived(batch) {
        return batch.archived === true;
    }

    function getBatchDate(value) {
        if (!value) return "";

        return String(value).slice(0, 10);
    }

    async function handleSubmit(event) {
        event.preventDefault();

        if (saving) return;

        setError("");
        setSuccess("");

        const purchasePrice = Number(form.purchasePrice);

        if (!form.batchNumber.trim()) {
            setError("Please enter a batch number.");
            return;
        }

        if (
            form.purchasePrice.trim() === "" ||
            !Number.isFinite(purchasePrice) ||
            purchasePrice <= 0
        ) {
            setError("Purchase price must be greater than zero.");
            return;
        }

        if (!form.expiryDate || !form.receivedDate) {
            setError("Expiry date and received date are required.");
            return;
        }

        if (
            form.manufacturingDate &&
            form.manufacturingDate > form.expiryDate
        ) {
            setError(
                "Manufacturing date cannot be later than the expiry date."
            );
            return;
        }

        if (
            form.manufacturingDate &&
            form.manufacturingDate > form.receivedDate
        ) {
            setError(
                "Received date cannot be earlier than the manufacturing date."
            );
            return;
        }

        if (form.receivedDate > form.expiryDate) {
            setError(
                "Received date cannot be later than the expiry date."
            );
            return;
        }

        const duplicateBatch = batches.some(
            (batch) =>
                batch.id !== editingBatchId &&
                batch.batchNumber?.trim().toLowerCase() ===
                form.batchNumber.trim().toLowerCase() &&
                String(getBatchProductId(batch)) ===
                String(form.productId || getBatchProductId(batch)) &&
                (
                    editingBatchId !== null ||
                    String(getBatchProductId(batch)) ===
                    String(form.productId)
                )
        );

        if (duplicateBatch) {
            setError(
                "This batch number already exists for the selected product."
            );
            return;
        }

        setSaving(true);

        try {
            if (editingBatchId !== null) {
                // Do not send productId or quantity.
                // Batch editing must not change inventory stock.
                await api.put(`/batches/${editingBatchId}`, {
                    batchNumber: form.batchNumber.trim(),
                    manufacturingDate: form.manufacturingDate || null,
                    expiryDate: form.expiryDate,
                    purchasePrice,
                    receivedDate: form.receivedDate,
                });

                setSuccess(
                    "Batch details updated successfully. Expiry risk and clearance pricing will be recalculated."
                );
            } else {
                const quantity = Number(form.quantity);

                if (!form.productId) {
                    setError("Please select a product.");
                    return;
                }

                if (!Number.isInteger(quantity) || quantity <= 0) {
                    setError("Quantity must be a positive whole number.");
                    return;
                }

                await api.post("/batches", {
                    productId: Number(form.productId),
                    batchNumber: form.batchNumber.trim(),
                    manufacturingDate: form.manufacturingDate || null,
                    expiryDate: form.expiryDate,
                    quantity,
                    purchasePrice,
                    receivedDate: form.receivedDate,
                });

                setSuccess("Batch received successfully.");
            }

            closeForm();
            await loadData();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                (
                    editingBatchId !== null
                        ? "Unable to update batch. Check the entered values."
                        : "Unable to create batch. Check the entered values."
                )
            );
        } finally {
            setSaving(false);
        }
    }

    function startEditing(batch) {
        if (isArchived(batch)) {
            setError("Archived batches cannot be edited.");
            return;
        }

        setError("");
        setSuccess("");
        setEditingBatchId(batch.id);

        setForm({
            productId: String(getBatchProductId(batch) ?? ""),
            batchNumber: batch.batchNumber ?? "",
            manufacturingDate: getBatchDate(batch.manufacturingDate),
            expiryDate: getBatchDate(batch.expiryDate),
            quantity: "",
            purchasePrice: String(batch.purchasePrice ?? ""),
            receivedDate:
                getBatchDate(batch.receivedDate) ||
                new Date().toISOString().slice(0, 10),
        });

        setShowForm(true);
    }

    async function handleArchive(batch) {
        if (
            saving ||
            archivingBatchId !== null ||
            restoringBatchId !== null
        ) {
            return;
        }

        if (isArchived(batch)) {
            setError("This batch is already archived.");
            return;
        }

        if (Number(batch.quantity) !== 0) {
            setError(
                `Batch ${batch.batchNumber} cannot be archived because it still has ${batch.quantity} units. Resolve its remaining stock first.`
            );
            return;
        }

        const confirmed = window.confirm(
            `Archive batch "${batch.batchNumber}"?\n\nThis action is intended for a batch with zero remaining stock. Its inventory history will be retained.`
        );

        if (!confirmed) return;

        setError("");
        setSuccess("");
        setArchivingBatchId(batch.id);

        try {
            await api.patch(`/batches/${batch.id}/archive`);

            setSuccess(
                `Batch ${batch.batchNumber} archived successfully. Its history has been retained.`
            );

            await loadData();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to archive this batch. Verify its stock and try again."
            );
        } finally {
            setArchivingBatchId(null);
        }
    }

    async function handleRestore(batch) {
        if (
            saving ||
            archivingBatchId !== null ||
            restoringBatchId !== null
        ) {
            return;
        }

        if (!isArchived(batch)) {
            setError("This batch is not archived.");
            return;
        }

        const confirmed = window.confirm(
            `Restore batch "${batch.batchNumber}"?\n\n` +
            "The batch will return to the regular inventory list. " +
            "Its quantity and transaction history will remain unchanged."
        );

        if (!confirmed) return;

        setError("");
        setSuccess("");
        setRestoringBatchId(batch.id);

        try {
            await api.patch(`/batches/${batch.id}/restore`);

            setSuccess(
                `Batch ${batch.batchNumber} restored successfully.`
            );

            await loadData();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to restore this batch. Please try again."
            );
        } finally {
            setRestoringBatchId(null);
        }
    }

    function getRiskClass(risk) {
        return `risk-badge risk-${String(
            risk || "unknown"
        ).toLowerCase()}`;
    }

    function formatPrice(price) {
        const value = Number(price);

        return Number.isFinite(value)
            ? `₹${value.toFixed(2)}`
            : "—";
    }

    const today = new Date().toISOString().slice(0, 10);

    const activeBatches = batches.filter(
        (batch) =>
            !isArchived(batch) &&
            (!batch.expiryDate || batch.expiryDate >= today)
    );

    const expiredBatchesList = batches.filter(
        (batch) =>
            !isArchived(batch) &&
            batch.expiryDate &&
            batch.expiryDate < today
    );

    const archivedBatches = batches.filter(isArchived);

    const totalUnits = batches
        .filter((batch) => !isArchived(batch))
        .reduce(
            (total, batch) =>
                total + Math.max(0, Number(batch.quantity) || 0),
            0
        );

    const nearExpiryBatches = activeBatches.filter((batch) => {
        const risk = String(
            risks[batch.id]?.riskLevel || ""
        ).toUpperCase();

        return ["HIGH", "CRITICAL"].includes(risk);
    }).length;

    function renderBatchActions(batch) {
        if (isArchived(batch)) {
            return (
                <div className="batch-form-actions">
                    <button
                        type="button"
                        className="batch-secondary-button"
                        onClick={() => handleRestore(batch)}
                        disabled={
                            saving ||
                            restoringBatchId !== null ||
                            archivingBatchId !== null
                        }
                    >
                        {restoringBatchId === batch.id
                            ? "Restoring..."
                            : "Restore"}
                    </button>
                </div>
            );
        }

        return (
            <div className="batch-form-actions">
                <button
                    type="button"
                    className="batch-secondary-button"
                    onClick={() => startEditing(batch)}
                    disabled={
                        saving ||
                        archivingBatchId !== null ||
                        restoringBatchId !== null
                    }
                >
                    Edit
                </button>

                <button
                    type="button"
                    className="batch-secondary-button"
                    onClick={() => handleArchive(batch)}
                    disabled={
                        saving ||
                        archivingBatchId !== null ||
                        restoringBatchId !== null ||
                        Number(batch.quantity) !== 0
                    }
                    title={
                        Number(batch.quantity) !== 0
                            ? "Only batches with zero remaining stock can be archived."
                            : "Archive this batch"
                    }
                >
                    {archivingBatchId === batch.id
                        ? "Archiving..."
                        : "Archive"}
                </button>
            </div>
        );
    }

    function renderBatchTable(list, type) {
        const expired = type === "expired";
        const archived = type === "archived";

        return (
            <div className="batches-table-wrapper">
                <table className="batches-table">
                    <thead>
                    <tr>
                        <th>Batch</th>
                        <th>Product</th>
                        <th>Quantity</th>
                        <th>Expiry Date</th>

                        {archived ? (
                            <th>Status</th>
                        ) : expired ? (
                            <>
                                <th>Days Overdue</th>
                                <th>Status</th>
                            </>
                        ) : (
                            <>
                                <th>Days Left</th>
                                <th>Risk</th>
                            </>
                        )}

                        <th>Discount</th>
                        <th>Clearance Price</th>
                        <th>Actions</th>
                    </tr>
                    </thead>

                    <tbody>
                    {list.map((batch) => {
                        const risk = risks[batch.id];
                        const days = risk?.daysUntilExpiry;

                        const daysOverdue =
                            days != null
                                ? Math.abs(days)
                                : batch.expiryDate
                                    ? Math.max(
                                        0,
                                        Math.floor(
                                            (
                                                new Date(
                                                    today + "T00:00:00"
                                                ) -
                                                new Date(
                                                    batch.expiryDate +
                                                    "T00:00:00"
                                                )
                                            ) / 86400000
                                        )
                                    )
                                    : "—";

                        return (
                            <tr key={batch.id}>
                                <td>{batch.batchNumber}</td>
                                <td>{getBatchProductName(batch)}</td>
                                <td>{batch.quantity}</td>
                                <td>{batch.expiryDate || "—"}</td>

                                {archived ? (
                                    <td>
                                            <span className="risk-badge risk-unknown">
                                                ARCHIVED
                                            </span>
                                    </td>
                                ) : expired ? (
                                    <>
                                        <td>
                                                <span className="days-overdue">
                                                    {daysOverdue}
                                                </span>
                                        </td>
                                        <td>
                                                <span className="risk-badge risk-expired">
                                                    EXPIRED
                                                </span>
                                        </td>
                                    </>
                                ) : (
                                    <>
                                        <td>
                                            {risk?.daysUntilExpiry ?? "—"}
                                        </td>
                                        <td>
                                                <span
                                                    className={getRiskClass(
                                                        risk?.riskLevel
                                                    )}
                                                >
                                                    {risk?.riskLevel || "Unknown"}
                                                </span>
                                        </td>
                                    </>
                                )}

                                <td>
                                    {risk?.discountPercent != null
                                        ? `${risk.discountPercent}%`
                                        : expired
                                            ? "100%"
                                            : "—"}
                                </td>

                                <td>
                                    {risk?.clearancePrice != null
                                        ? formatPrice(risk.clearancePrice)
                                        : expired
                                            ? "₹0.00"
                                            : "—"}
                                </td>

                                <td>{renderBatchActions(batch)}</td>
                            </tr>
                        );
                    })}
                    </tbody>
                </table>
            </div>
        );
    }

    return (
        <div className="batches-page">
            <header className="batches-header">
                <div>
                    <h1>Batch Management</h1>
                    <p>
                        Track inventory quantities, expiry dates and risk.
                    </p>
                </div>

                <button
                    type="button"
                    className="batch-primary-button"
                    onClick={() => {
                        if (showForm) {
                            closeForm();
                        } else {
                            setEditingBatchId(null);
                            resetForm();
                            setShowForm(true);
                        }

                        setError("");
                        setSuccess("");
                    }}
                >
                    {showForm ? "Cancel" : "+ Add Batch"}
                </button>
            </header>

            {success && (
                <div className="batch-success" role="status">
                    {success}
                </div>
            )}

            {error && (
                <div className="batch-error" role="alert">
                    {error}
                </div>
            )}

            <section className="batch-summary">
                <div className="batch-summary-card">
                    <span>Total Batches</span>
                    <strong>{batches.length}</strong>
                </div>

                <div className="batch-summary-card">
                    <span>Inventory Units</span>
                    <strong>{totalUnits}</strong>
                </div>

                <div className="batch-summary-card">
                    <span>Expired Batches</span>
                    <strong>{expiredBatchesList.length}</strong>
                </div>

                <div className="batch-summary-card">
                    <span>High/Critical Risk</span>
                    <strong>{nearExpiryBatches}</strong>
                </div>
            </section>

            {showForm && (
                <section className="batch-form-card">
                    <h2>
                        {editingBatchId !== null
                            ? "Edit Batch Details"
                            : "Receive New Batch"}
                    </h2>

                    {editingBatchId !== null && (
                        <p>
                            Product and quantity cannot be changed here.
                            Inventory adjustments must use the stock
                            transaction workflow.
                        </p>
                    )}

                    <form onSubmit={handleSubmit}>
                        <div className="batch-form-grid">
                            {editingBatchId === null && (
                                <label>
                                    Product
                                    <select
                                        name="productId"
                                        value={form.productId}
                                        onChange={handleChange}
                                        required
                                    >
                                        <option value="">
                                            Select product
                                        </option>

                                        {products
                                            .filter(
                                                (product) =>
                                                    product.active !== false
                                            )
                                            .map((product) => (
                                                <option
                                                    key={product.id}
                                                    value={product.id}
                                                >
                                                    {product.name} ({product.sku})
                                                </option>
                                            ))}
                                    </select>
                                </label>
                            )}

                            <label>
                                Batch Number
                                <input
                                    name="batchNumber"
                                    value={form.batchNumber}
                                    onChange={handleChange}
                                    maxLength={100}
                                    placeholder="e.g. PCM-B02"
                                    required
                                />
                            </label>

                            <label>
                                Manufacturing Date
                                <input
                                    type="date"
                                    name="manufacturingDate"
                                    value={form.manufacturingDate}
                                    onChange={handleChange}
                                    max={form.expiryDate || undefined}
                                />
                            </label>

                            <label>
                                Expiry Date
                                <input
                                    type="date"
                                    name="expiryDate"
                                    value={form.expiryDate}
                                    onChange={handleChange}
                                    min={form.manufacturingDate || undefined}
                                    required
                                />
                            </label>

                            {editingBatchId === null && (
                                <label>
                                    Quantity Received
                                    <input
                                        type="number"
                                        name="quantity"
                                        min="1"
                                        step="1"
                                        value={form.quantity}
                                        onChange={handleChange}
                                        placeholder="e.g. 100"
                                        required
                                    />
                                </label>
                            )}

                            <label>
                                Purchase Price per Unit (₹)
                                <input
                                    type="number"
                                    name="purchasePrice"
                                    min="0.01"
                                    step="0.01"
                                    value={form.purchasePrice}
                                    onChange={handleChange}
                                    placeholder="e.g. 15.00"
                                    required
                                />
                            </label>

                            <label>
                                Received Date
                                <input
                                    type="date"
                                    name="receivedDate"
                                    value={form.receivedDate}
                                    onChange={handleChange}
                                    required
                                />
                            </label>
                        </div>

                        <div className="batch-form-actions">
                            <button
                                type="button"
                                className="batch-secondary-button"
                                onClick={closeForm}
                                disabled={saving}
                            >
                                Cancel
                            </button>

                            <button
                                type="submit"
                                className="batch-primary-button"
                                disabled={saving}
                            >
                                {saving
                                    ? "Saving..."
                                    : editingBatchId !== null
                                        ? "Save Changes"
                                        : "Receive Batch"}
                            </button>
                        </div>

                        {editingBatchId === null &&
                            products.filter(
                                (product) => product.active !== false
                            ).length === 0 && (
                                <p>
                                    Create an active product before receiving
                                    a batch.
                                </p>
                            )}
                    </form>
                </section>
            )}

            <section className="batches-table-card">
                <div className="batches-table-heading">
                    <div>
                        <h2>Inventory Batches</h2>
                        <p>
                            Monitor stock levels, expiry risk, and clearance
                            pricing.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="batch-secondary-button"
                        onClick={loadData}
                        disabled={loading}
                    >
                        {loading ? "Loading..." : "Refresh"}
                    </button>
                </div>

                {loading ? (
                    <p>Loading batches...</p>
                ) : batches.length === 0 ? (
                    <p>
                        No batches found. Receive your first batch to get
                        started.
                    </p>
                ) : (
                    <>
                        <section className="batch-section">
                            <div className="batch-section-heading batch-active-heading">
                                <div className="batch-section-title">
                                    <span className="batch-section-indicator" />
                                    <div>
                                        <h3>Active Inventory Batches</h3>
                                        <p>
                                            Batches that have not passed their
                                            expiry date and are not archived.
                                        </p>
                                    </div>
                                </div>

                                <span className="batch-section-count">
                                    {activeBatches.length}
                                </span>
                            </div>

                            {activeBatches.length === 0 ? (
                                <div className="batch-empty-state">
                                    <strong>No active batches</strong>
                                    <p>
                                        There are currently no unarchived
                                        batches with an expiry date today or
                                        later.
                                    </p>
                                </div>
                            ) : (
                                renderBatchTable(activeBatches, "active")
                            )}
                        </section>

                        <section className="batch-section batch-expired-section">
                            <div className="batch-section-heading batch-expired-heading">
                                <div className="batch-section-title">
                                    <span className="batch-section-indicator" />
                                    <div>
                                        <h3>Expired Batches</h3>
                                        <p>
                                            Expired batches retained for
                                            inventory tracking and auditing.
                                        </p>
                                    </div>
                                </div>

                                <span className="batch-section-count">
                                    {expiredBatchesList.length}
                                </span>
                            </div>

                            {expiredBatchesList.length === 0 ? (
                                <div className="batch-empty-state batch-expired-empty">
                                    <strong>No expired batches</strong>
                                    <p>
                                        Batches that pass their expiry dates
                                        will appear here automatically.
                                    </p>
                                </div>
                            ) : (
                                renderBatchTable(
                                    expiredBatchesList,
                                    "expired"
                                )
                            )}
                        </section>

                        <section className="batch-section">
                            <div className="batch-section-heading">
                                <div className="batch-section-title">
                                    <span className="batch-section-indicator" />
                                    <div>
                                        <h3>Archived Batches</h3>
                                        <p>
                                            Archived records are retained for
                                            historical tracking and are not
                                            available for normal inventory use.
                                        </p>
                                    </div>
                                </div>

                                <span className="batch-section-count">
                                    {archivedBatches.length}
                                </span>
                            </div>

                            {archivedBatches.length === 0 ? (
                                <div className="batch-empty-state">
                                    <strong>No archived batches</strong>
                                    <p>
                                        Batches archived after their stock
                                        reaches zero will appear here.
                                    </p>
                                </div>
                            ) : (
                                renderBatchTable(
                                    archivedBatches,
                                    "archived"
                                )
                            )}
                        </section>
                    </>
                )}
            </section>
        </div>
    );
}

export default Batches;