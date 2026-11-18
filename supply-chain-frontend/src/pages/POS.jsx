import { useCallback, useEffect, useMemo, useState } from "react";
import api from "../services/api";
import "./POS.css";

const PENDING_CHECKOUT_KEY = "supply_chain_pending_checkout";

function getPendingCheckout(payload) {
    try {
        const stored = sessionStorage.getItem(PENDING_CHECKOUT_KEY);

        if (stored) {
            const pending = JSON.parse(stored);
            const fingerprint = JSON.stringify(payload);

            if (pending.fingerprint === fingerprint) {
                return pending.idempotencyKey;
            }
        }
    } catch (error) {
        console.error("Could not retrieve pending checkout:", error);
    }

    const idempotencyKey = crypto.randomUUID();

    try {
        sessionStorage.setItem(
            PENDING_CHECKOUT_KEY,
            JSON.stringify({
                fingerprint: JSON.stringify(payload),
                idempotencyKey,
            })
        );
    } catch (error) {
        console.error("Could not save pending checkout:", error);
    }

    return idempotencyKey;
}

function clearPendingCheckout() {
    sessionStorage.removeItem(PENDING_CHECKOUT_KEY);
}

function POS() {
    const [batches, setBatches] = useState([]);
    const [cart, setCart] = useState([]);
    const [search, setSearch] = useState("");
    const [loading, setLoading] = useState(true);
    const [checkingOut, setCheckingOut] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [receipt, setReceipt] = useState(null);

    const loadBatches = useCallback(async () => {
        setLoading(true);
        setError("");

        try {
            const response = await api.get("/batches");
            const data = Array.isArray(response.data)
                ? response.data
                : [];

            const now = new Date();
            const today = [
                now.getFullYear(),
                String(now.getMonth() + 1).padStart(2, "0"),
                String(now.getDate()).padStart(2, "0"),
            ].join("-");

            const eligibleBatches = data.filter(
                (batch) =>
                    Number(batch.quantity) > 0 &&
                    batch.expiryDate &&
                    batch.expiryDate > today &&
                    batch.product?.active !== false
            );

            const enrichedBatches = await Promise.all(
                eligibleBatches.map(async (batch) => {
                    try {
                        const riskResponse = await api.get(
                            `/batches/${batch.id}/expiry-risk`
                        );

                        return {
                            ...batch,
                            ...riskResponse.data,
                        };
                    } catch (riskError) {
                        console.error(
                            `Could not load expiry risk for batch ${batch.batchNumber}:`,
                            riskError
                        );

                        return batch;
                    }
                })
            );

            setBatches(enrichedBatches);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load available inventory."
            );
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadBatches();
    }, [loadBatches]);

    const filteredBatches = useMemo(() => {
        const term = search.trim().toLowerCase();

        if (!term) return batches;

        return batches.filter((batch) => {
            const productName = batch.product?.name || "";
            const sku = batch.product?.sku || "";
            const batchNumber = batch.batchNumber || "";

            return (
                productName.toLowerCase().includes(term) ||
                sku.toLowerCase().includes(term) ||
                batchNumber.toLowerCase().includes(term)
            );
        });
    }, [batches, search]);

    function getSellingPrice(batch) {
        const basePrice = Number(
            batch.product?.baseSellingPrice ?? 0
        );

        const discount = Number(batch.discountPercent ?? 0);
        const clearancePrice = Number(batch.clearancePrice);

        if (
            batch.clearancePrice != null &&
            Number.isFinite(clearancePrice) &&
            clearancePrice >= 0
        ) {
            return clearancePrice;
        }

        if (discount > 0 && discount <= 100) {
            return basePrice * (1 - discount / 100);
        }

        return basePrice;
    }

    function getSellableQuantity(productId) {
        return batches
            .filter((batch) => batch.product?.id === productId)
            .reduce(
                (total, batch) =>
                    total + Number(batch.quantity || 0),
                0
            );
    }

    function addToCart(batch) {
        setError("");
        setSuccess("");
        setReceipt(null);

        const productId = batch.product?.id;

        if (productId == null) {
            setError(
                "The product ID is missing. Refresh inventory and try again."
            );
            return;
        }

        const existingItem = cart.find(
            (item) => item.productId === productId
        );

        const currentQuantity = existingItem?.quantity || 0;
        const availableQuantity = getSellableQuantity(productId);

        if (currentQuantity >= availableQuantity) {
            setError(
                `Only ${availableQuantity} sellable units are available for ${batch.product.name}.`
            );
            return;
        }

        if (existingItem) {
            setCart((previous) =>
                previous.map((item) =>
                    item.productId === productId
                        ? {
                            ...item,
                            quantity: item.quantity + 1,
                            availableQuantity,
                        }
                        : item
                )
            );
            return;
        }

        setCart((previous) => [
            ...previous,
            {
                productId,
                productName: batch.product?.name || "Unknown product",
                sku: batch.product?.sku || "—",
                quantity: 1,
                availableQuantity,
                estimatedUnitPrice: getSellingPrice(batch),
            },
        ]);
    }

    function updateQuantity(productId, value) {
        const requestedQuantity = Number(value);

        const item = cart.find(
            (cartItem) => cartItem.productId === productId
        );

        if (!item || !Number.isInteger(requestedQuantity)) return;

        if (requestedQuantity < 1) {
            removeFromCart(productId);
            return;
        }

        if (requestedQuantity > item.availableQuantity) {
            setError(
                `Only ${item.availableQuantity} sellable units are available for ${item.productName}.`
            );
            return;
        }

        setError("");

        setCart((previous) =>
            previous.map((cartItem) =>
                cartItem.productId === productId
                    ? {
                        ...cartItem,
                        quantity: requestedQuantity,
                    }
                    : cartItem
            )
        );
    }

    function removeFromCart(productId) {
        setCart((previous) =>
            previous.filter(
                (item) => item.productId !== productId
            )
        );

        setError("");
        setSuccess("");
        setReceipt(null);
    }

    const totalQuantity = cart.reduce(
        (total, item) => total + item.quantity,
        0
    );

    const estimatedTotal = cart.reduce(
        (total, item) =>
            total + item.quantity * item.estimatedUnitPrice,
        0
    );

    async function handleCheckout() {
        if (checkingOut) return;

        if (cart.length === 0) {
            setError("Add at least one item before checkout.");
            return;
        }

        setCheckingOut(true);
        setError("");
        setSuccess("");
        setReceipt(null);

        try {
            const payload = {
                items: cart.map((item) => ({
                    productId: item.productId,
                    quantity: item.quantity,
                })),
            };

            const idempotencyKey = getPendingCheckout(payload);

            const response = await api.post("/sales", {
                ...payload,
                idempotencyKey,
            });

            clearPendingCheckout();

            setReceipt(response.data);
            setSuccess("Checkout completed successfully.");
            setCart([]);

            await loadBatches();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                err.response?.data?.error ||
                "Checkout failed. Retry using the same cart to avoid duplicate sales."
            );

            await loadBatches();
        } finally {
            setCheckingOut(false);
        }
    }

    function printReceipt() {
        const receipt = document.querySelector(".pos-receipt");

        if (!receipt) {
            alert("Receipt not found.");
            return;
        }

        const printWindow = window.open("", "_blank", "width=900,height=700");

        if (!printWindow) {
            alert("Allow pop-ups for this website, then try again.");
            return;
        }

        const clone = receipt.cloneNode(true);
        clone.querySelectorAll("button").forEach((button) => button.remove());

        printWindow.document.open();
        printWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Sale Receipt</title>
            <style>
                @page {
                    size: A4 portrait;
                    margin: 15mm;
                }

                * {
                    box-sizing: border-box;
                }

                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    background: white;
                    color: #111;
                    font-family: Arial, sans-serif;
                    font-size: 14px;
                }

                body {
                    padding: 0;
                }

                .pos-receipt {
                    display: block;
                    width: 100%;
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 24px;
                    border: 1px solid #ccc;
                    border-radius: 8px;
                    background: white;
                    box-shadow: none;
                    color: #111;
                }

                .pos-receipt h3 {
                    margin: 0 0 24px;
                    padding-bottom: 16px;
                    border-bottom: 1px dashed #777;
                    text-align: center;
                    font-size: 22px;
                }

                .pos-receipt-header,
                .pos-receipt-item-heading,
                .pos-receipt-total {
                    display: flex;
                    justify-content: space-between;
                    align-items: flex-start;
                    gap: 16px;
                }

                .pos-receipt-header {
                    margin-bottom: 16px;
                    padding-bottom: 16px;
                    border-bottom: 1px dashed #777;
                }

                .pos-receipt-items {
                    display: block;
                }

                .pos-receipt-item {
                    display: block;
                    padding: 16px 0;
                    border-bottom: 1px solid #ddd;
                    break-inside: avoid;
                    page-break-inside: avoid;
                }

                .pos-receipt-item-heading {
                    margin-bottom: 10px;
                    font-size: 15px;
                }

                .pos-receipt-item-details {
                    display: flex;
                    flex-direction: column;
                    gap: 7px;
                    margin-top: 8px;
                    font-size: 13px;
                    line-height: 1.5;
                }

                .pos-receipt-total {
                    margin-top: 24px;
                    padding-top: 16px;
                    border-top: 2px solid #111;
                    font-weight: bold;
                    break-inside: avoid;
                }

                .pos-receipt-total strong {
                    font-size: 20px;
                }

                button {
                    display: none !important;
                }
            </style>
        </head>
        <body>
            ${clone.outerHTML}
        </body>
        </html>
        `);

        printWindow.document.close();

        printWindow.onload = () => {
            printWindow.focus();
            setTimeout(() => printWindow.print(), 500);
        };
    }

    return (
        <div className="pos-page">
            <header className="pos-header">
                <div>
                    <span className="pos-eyebrow">
                        POINT OF SALE
                    </span>

                    <h1>Checkout</h1>

                    <p>
                        Select products and quantities. The backend
                        allocates stock using FEFO.
                    </p>
                </div>

                <button
                    type="button"
                    className="pos-refresh-button"
                    onClick={loadBatches}
                    disabled={loading || checkingOut}
                >
                    ↻ Refresh Stock
                </button>
            </header>

            <div className="pos-workspace">
                <section className="pos-inventory-panel">
                    <div className="pos-panel-heading">
                        <div>
                            <h2>Available inventory</h2>
                            <p>Select a batch to add its product.</p>
                        </div>

                        <span className="pos-count">
                            {filteredBatches.length} batches
                        </span>
                    </div>

                    <div className="pos-search">
                        <span>⌕</span>

                        <input
                            type="search"
                            value={search}
                            onChange={(event) =>
                                setSearch(event.target.value)
                            }
                            placeholder="Search product, SKU or batch..."
                            aria-label="Search available inventory"
                        />
                    </div>

                    {loading ? (
                        <div className="pos-empty-state">
                            Loading available inventory...
                        </div>
                    ) : filteredBatches.length === 0 ? (
                        <div className="pos-empty-state">
                            <strong>No available batches found</strong>
                            <span>
                                Try another search or receive new inventory.
                            </span>
                        </div>
                    ) : (
                        <div className="pos-product-list">
                            {filteredBatches.map((batch) => {
                                const productId = batch.product?.id;

                                const inCart = cart.some(
                                    (item) =>
                                        item.productId === productId
                                );

                                const sellingPrice =
                                    getSellingPrice(batch);

                                return (
                                    <article
                                        className="pos-product-card"
                                        key={batch.id}
                                    >
                                        <div className="pos-product-icon">
                                            {(batch.product?.name || "P")
                                                .charAt(0)
                                                .toUpperCase()}
                                        </div>

                                        <div className="pos-product-details">
                                            <h3>
                                                {batch.product?.name ||
                                                    "Unknown product"}
                                            </h3>

                                            <span>
                                                SKU:{" "}
                                                {batch.product?.sku || "—"}
                                            </span>

                                            <span>
                                                Batch: {batch.batchNumber}
                                            </span>

                                            <span>
                                                Expires: {batch.expiryDate}
                                            </span>

                                            <span className="pos-stock-label">
                                                {batch.quantity} units in
                                                this batch
                                            </span>
                                        </div>

                                        <div className="pos-product-action">
                                            {Number(batch.discountPercent) > 0 && (
                                                <span className="pos-clearance-discount">
                                                    {Number(batch.discountPercent)}% OFF
                                                </span>
                                            )}

                                            {batch.riskLevel && (
                                                <span
                                                    className={`pos-risk-badge pos-risk-${String(
                                                        batch.riskLevel
                                                    ).toLowerCase()}`}
                                                >
                                                    {String(batch.riskLevel).replace("_", " ")}
                                                </span>
                                            )}

                                            <strong>
                                                ₹{sellingPrice.toFixed(2)}
                                            </strong>

                                            {Number(batch.discountPercent) > 0 && (
                                                <span className="pos-original-price">
                                                    Original: ₹
                                                    {Number(
                                                        batch.product?.baseSellingPrice ?? 0
                                                    ).toFixed(2)}
                                                </span>
                                            )}

                                            <button
                                                type="button"
                                                onClick={() => addToCart(batch)}
                                                disabled={
                                                    Number(batch.quantity) <= 0 ||
                                                    productId == null ||
                                                    checkingOut
                                                }
                                            >
                                                {inCart ? "+ Add More" : "+ Add"}
                                            </button>
                                        </div>
                                    </article>
                                );
                            })}
                        </div>
                    )}
                </section>

                <aside className="pos-cart-panel">
                    <div className="pos-cart-heading">
                        <div>
                            <h2>Current sale</h2>
                            <p>Review products and quantities.</p>
                        </div>

                        <span className="pos-cart-count">
                            {totalQuantity}
                        </span>
                    </div>

                    {cart.length === 0 ? (
                        <div className="pos-cart-empty">
                            <div className="pos-cart-empty-icon">
                                ▤
                            </div>

                            <strong>Your cart is empty</strong>

                            <span>
                                Select available inventory to begin.
                            </span>
                        </div>
                    ) : (
                        <div className="pos-cart-items">
                            {cart.map((item) => (
                                <div
                                    className="pos-cart-item"
                                    key={item.productId}
                                >
                                    <div className="pos-cart-item-top">
                                        <div>
                                            <strong>
                                                {item.productName}
                                            </strong>

                                            <span>SKU {item.sku}</span>

                                            <span>
                                                FEFO batch allocation at
                                                checkout
                                            </span>
                                        </div>

                                        <button
                                            type="button"
                                            className="pos-remove-button"
                                            onClick={() =>
                                                removeFromCart(
                                                    item.productId
                                                )
                                            }
                                            aria-label={`Remove ${item.productName}`}
                                            disabled={checkingOut}
                                        >
                                            ×
                                        </button>
                                    </div>

                                    <div className="pos-cart-item-bottom">
                                        <div className="pos-quantity-control">
                                            <button
                                                type="button"
                                                onClick={() =>
                                                    updateQuantity(
                                                        item.productId,
                                                        item.quantity - 1
                                                    )
                                                }
                                                disabled={checkingOut}
                                                aria-label="Decrease quantity"
                                            >
                                                −
                                            </button>

                                            <input
                                                type="number"
                                                min="1"
                                                max={item.availableQuantity}
                                                value={item.quantity}
                                                disabled={checkingOut}
                                                onChange={(event) =>
                                                    updateQuantity(
                                                        item.productId,
                                                        event.target.value
                                                    )
                                                }
                                                aria-label="Item quantity"
                                            />

                                            <button
                                                type="button"
                                                onClick={() =>
                                                    updateQuantity(
                                                        item.productId,
                                                        item.quantity + 1
                                                    )
                                                }
                                                disabled={
                                                    checkingOut ||
                                                    item.quantity >=
                                                    item.availableQuantity
                                                }
                                                aria-label="Increase quantity"
                                            >
                                                +
                                            </button>
                                        </div>

                                        <strong>
                                            ₹
                                            {(
                                                item.quantity *
                                                item.estimatedUnitPrice
                                            ).toFixed(2)}
                                        </strong>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    <div className="pos-cart-summary">
                        <div>
                            <span>Total items</span>
                            <strong>{totalQuantity}</strong>
                        </div>

                        <div className="pos-total-row">
                            <span>Estimated total</span>

                            <strong>
                                ₹{estimatedTotal.toFixed(2)}
                            </strong>
                        </div>

                        <button
                            type="button"
                            className="pos-checkout-button"
                            onClick={handleCheckout}
                            disabled={
                                checkingOut || cart.length === 0
                            }
                        >
                            {checkingOut
                                ? "Processing sale..."
                                : "Complete Checkout →"}
                        </button>

                        <p>
                            The final total may differ because the backend
                            calculates prices for the batches actually
                            allocated using FEFO.
                        </p>
                    </div>
                </aside>
            </div>

            {error && (
                <div
                    className="pos-alert pos-alert-error"
                    role="alert"
                >
                    {error}
                </div>
            )}

            {success && (
                <div
                    className="pos-alert pos-alert-success"
                    role="status"
                >
                    <strong>{success}</strong>

                    {receipt && (
                        <div className="pos-receipt">
                            <h3>🧾 Sale Receipt</h3>

                            <div className="pos-receipt-header">
                                <span>Sale ID</span>
                                <strong>#{receipt.id}</strong>
                            </div>

                            <div className="pos-receipt-items">
                                {Array.isArray(receipt.items) &&
                                    receipt.items.map((item, index) => (
                                        <div
                                            className="pos-receipt-item"
                                            key={item.id ?? index}
                                        >
                                            <div className="pos-receipt-item-heading">
                                                <strong>
                                                    {item.product?.name ||
                                                        "Product"}
                                                </strong>

                                                <strong>
                                                    ₹
                                                    {(
                                                        Number(
                                                            item.sellingPrice || 0
                                                        ) *
                                                        Number(
                                                            item.quantity || 0
                                                        )
                                                    ).toFixed(2)}
                                                </strong>
                                            </div>

                                            <div className="pos-receipt-item-details">
                                                <span>
                                                    Batch:{" "}
                                                    {item.batch?.batchNumber ||
                                                        "—"}
                                                </span>

                                                <span>
                                                    Quantity: {item.quantity}
                                                </span>

                                                <span>
                                                    Unit price: ₹
                                                    {Number(
                                                        item.sellingPrice || 0
                                                    ).toFixed(2)}
                                                </span>
                                            </div>
                                        </div>
                                    ))}
                            </div>

                            <div className="pos-receipt-total">
                                <span>Final total</span>

                                <strong>
                                    ₹
                                    {Number(
                                        receipt.totalAmount || 0
                                    ).toFixed(2)}
                                </strong>
                            </div>

                            <button
                                type="button"
                                className="pos-refresh-button"
                                onClick={printReceipt}
                            >
                                Print Receipt
                            </button>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

export default POS;