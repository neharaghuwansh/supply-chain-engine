import { useEffect, useState, useCallback } from "react";
import api from "../services/api";
import "./SalesHistory.css";

const PAGE_SIZE = 20;

function formatCurrency(amount) {
    return new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(Number(amount) || 0);
}

function formatDate(date) {
    if (!date) return "—";

    const parsedDate = new Date(date);

    if (Number.isNaN(parsedDate.getTime())) return "—";

    return parsedDate.toLocaleString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
}

function getSaleItems(sale) {
    return Array.isArray(sale?.items) ? sale.items : [];
}

function getTotalQuantity(sale) {
    return getSaleItems(sale).reduce(
        (total, item) => total + Number(item.quantity || 0),
        0
    );
}

function getProductSummary(sale) {
    const items = getSaleItems(sale);
    const products = new Map();

    items.forEach((item) => {
        const productName = item.product?.name || "Unknown product";
        const key = item.product?.id ?? productName;

        if (!products.has(key)) {
            products.set(key, {
                name: productName,
                quantity: 0,
            });
        }

        products.get(key).quantity += Number(item.quantity || 0);
    });

    return Array.from(products.values());
}

function getBatchSummary(sale) {
    const batches = getSaleItems(sale)
        .map((item) => item.batch?.batchNumber)
        .filter(Boolean);

    return [...new Set(batches)];
}

function SalesHistory() {
    const [sales, setSales] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const [searchInput, setSearchInput] = useState("");
    const [search, setSearch] = useState("");
    const [fromDate, setFromDate] = useState("");
    const [toDate, setToDate] = useState("");
    const [appliedFromDate, setAppliedFromDate] = useState("");
    const [appliedToDate, setAppliedToDate] = useState("");

    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);

    const [selectedSale, setSelectedSale] = useState(null);
    const [returnQuantities, setReturnQuantities] = useState({});
    const [submittingItem, setSubmittingItem] = useState(null);

    const [returnedQuantities, setReturnedQuantities] = useState({});
    const [returnHistory, setReturnHistory] = useState([]);
    const [loadingDetails, setLoadingDetails] = useState(false);

    const loadSales = useCallback(async (pageToLoad = 0) => {
        setLoading(true);
        setError("");

        try {
            const params = {
                page: pageToLoad,
                size: PAGE_SIZE,
            };

            if (search.trim()) {
                params.search = search.trim();
            }

            if (appliedFromDate) {
                params.fromDate = appliedFromDate;
            }

            if (appliedToDate) {
                params.toDate = appliedToDate;
            }

            const response = await api.get("/sales/search", { params });
            const data = response.data;

            setSales(Array.isArray(data.content) ? data.content : []);
            setTotalPages(Number(data.totalPages) || 0);
            setTotalElements(Number(data.totalElements) || 0);
            setPage(Number(data.number) || 0);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load sales history."
            );
        } finally {
            setLoading(false);
        }
    }, [search, appliedFromDate, appliedToDate]);

    useEffect(() => {
        loadSales(0);
    }, [loadSales]);

    const applyFilters = (event) => {
        event.preventDefault();

        if (fromDate && toDate && fromDate > toDate) {
            setError("Start date cannot be after end date.");
            return;
        }

        setError("");
        setPage(0);
        setSearch(searchInput.trim());
        setAppliedFromDate(fromDate);
        setAppliedToDate(toDate);
    };

    const resetFilters = () => {
        setSearchInput("");
        setSearch("");
        setFromDate("");
        setToDate("");
        setAppliedFromDate("");
        setAppliedToDate("");
        setPage(0);
        setError("");
        setSuccess("");
    };

    const openSaleDetails = async (sale) => {
        setSelectedSale(sale);
        setLoadingDetails(true);
        setError("");
        setReturnHistory([]);
        setReturnedQuantities({});
        setReturnQuantities({});

        try {
            const [saleResponse, returnsResponse] = await Promise.all([
                api.get(`/sales/${sale.id}`),
                api.get(`/sales/${sale.id}/returns`),
            ]);

            const transactions = Array.isArray(returnsResponse.data)
                ? returnsResponse.data
                : [];

            const quantities = {};

            transactions.forEach((transaction) => {
                const itemId = transaction.saleItem?.id;

                if (itemId != null) {
                    quantities[itemId] =
                        (quantities[itemId] || 0) +
                        (Number(transaction.quantity) || 0);
                }
            });

            setSelectedSale(saleResponse.data);
            setReturnHistory(transactions);
            setReturnedQuantities(quantities);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                `Unable to load details for sale #${sale.id}.`
            );
        } finally {
            setLoadingDetails(false);
        }
    };

    const handleReturn = async (sale, item) => {
        setError("");
        setSuccess("");

        const quantity = Number(returnQuantities[item.id]);
        const returned = returnedQuantities[item.id] || 0;
        const remaining = Math.max(
            Number(item.quantity) - returned,
            0
        );

        if (!Number.isInteger(quantity) || quantity <= 0) {
            setError("Enter a positive whole-number return quantity.");
            return;
        }

        if (quantity > remaining) {
            setError(`Only ${remaining} unit(s) remain returnable.`);
            return;
        }

        const confirmed = window.confirm(
            `Process return of ${quantity} unit(s) of ${
                item.product?.name || "this product"
            } from sale #${sale.id}?`
        );

        if (!confirmed) return;

        setSubmittingItem(item.id);

        try {
            await api.post("/sales/returns", {
                saleId: sale.id,
                saleItemId: item.id,
                quantity,
            });

            setSuccess(
                `Return processed successfully for ${
                    item.product?.name || `sale item #${item.id}`
                }.`
            );

            setReturnQuantities((previous) => ({
                ...previous,
                [item.id]: "",
            }));

            await openSaleDetails(sale);
            await loadSales(page);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                err.response?.data?.error ||
                "Return failed. Please try again."
            );
        } finally {
            setSubmittingItem(null);
        }
    };

    const changePage = (newPage) => {
        if (
            newPage < 0 ||
            newPage >= totalPages ||
            newPage === page
        ) {
            return;
        }

        loadSales(newPage);
    };

    const closeSaleDetails = () => {
        if (submittingItem !== null) return;

        setSelectedSale(null);
        setReturnHistory([]);
        setReturnedQuantities({});
        setReturnQuantities({});
    };

    const pageStart = totalElements === 0
        ? 0
        : page * PAGE_SIZE + 1;

    const pageEnd = Math.min(
        (page + 1) * PAGE_SIZE,
        totalElements
    );

    return (
        <div className="sales-history-page">
            <header className="sales-history-header">
                <div>
                    <span className="sales-history-eyebrow">
                        SALES MANAGEMENT
                    </span>

                    <h1>Sales History</h1>

                    <p>
                        Search transactions, identify sold products and batches,
                        and manage returns.
                    </p>
                </div>

                <button
                    type="button"
                    className="sales-btn sales-btn-secondary"
                    onClick={() => loadSales(page)}
                    disabled={loading}
                >
                    ↻ Refresh
                </button>
            </header>

            {error && (
                <div className="sales-alert sales-alert-error" role="alert">
                    <span>{error}</span>

                    <button
                        type="button"
                        onClick={() => setError("")}
                        aria-label="Dismiss error"
                    >
                        ×
                    </button>
                </div>
            )}

            {success && (
                <div
                    className="sales-alert sales-alert-success"
                    role="status"
                >
                    <span>{success}</span>

                    <button
                        type="button"
                        onClick={() => setSuccess("")}
                        aria-label="Dismiss success message"
                    >
                        ×
                    </button>
                </div>
            )}

            <section className="sales-history-summary">
                <article className="sales-summary-card">
                    <div className="sales-summary-icon">▤</div>

                    <div>
                        <span>Matching Transactions</span>
                        <strong>
                            {totalElements.toLocaleString("en-IN")}
                        </strong>
                    </div>
                </article>
            </section>

            <section className="sales-filter-card">
                <div className="sales-section-heading">
                    <div>
                        <h2>Find a transaction</h2>
                        <p>
                            Search by sale ID or product name and narrow by date.
                        </p>
                    </div>
                </div>

                <form
                    className="sales-filter-form"
                    onSubmit={applyFilters}
                >
                    <label className="sales-search-field">
                        <span>Search</span>

                        <input
                            type="search"
                            placeholder="Sale ID or product name..."
                            value={searchInput}
                            onChange={(event) =>
                                setSearchInput(event.target.value)
                            }
                        />
                    </label>

                    <label>
                        <span>From date</span>

                        <input
                            type="date"
                            value={fromDate}
                            onChange={(event) =>
                                setFromDate(event.target.value)
                            }
                        />
                    </label>

                    <label>
                        <span>To date</span>

                        <input
                            type="date"
                            value={toDate}
                            onChange={(event) =>
                                setToDate(event.target.value)
                            }
                        />
                    </label>

                    <div className="sales-filter-actions">
                        <button
                            className="sales-btn sales-btn-primary"
                            type="submit"
                        >
                            Apply Filters
                        </button>

                        <button
                            className="sales-btn sales-btn-secondary"
                            type="button"
                            onClick={resetFilters}
                        >
                            Reset
                        </button>
                    </div>
                </form>
            </section>

            <section className="sales-results-card">
                <div className="sales-results-header">
                    <div>
                        <h2>Transactions</h2>

                        <p>
                            {totalElements.toLocaleString("en-IN")} matching
                            transaction(s)
                        </p>
                    </div>
                </div>

                {loading ? (
                    <div className="sales-loading">
                        <span className="sales-loading-spinner" />
                        Loading transactions...
                    </div>
                ) : sales.length === 0 ? (
                    <div className="sales-empty-state">
                        <div className="sales-empty-icon">⌕</div>
                        <h3>No matching transactions</h3>
                        <p>Try changing your search or date filters.</p>
                    </div>
                ) : (
                    <div className="sales-main-table-wrap">
                        <table className="sales-main-table">
                            <thead>
                            <tr>
                                <th>Transaction</th>
                                <th>Date &amp; Time</th>
                                <th>Product</th>
                                <th>Batch</th>
                                <th>Quantity</th>
                                <th>Total Amount</th>
                                <th>Action</th>
                            </tr>
                            </thead>

                            <tbody>
                            {sales.map((sale) => {
                                const products = getProductSummary(sale);
                                const batches = getBatchSummary(sale);

                                return (
                                    <tr key={sale.id}>
                                        <td>
                                                <span className="sales-id">
                                                    #{sale.id}
                                                </span>
                                        </td>

                                        <td>{formatDate(sale.saleDate)}</td>

                                        <td>
                                            <div className="sales-product-list">
                                                {products.length === 0 ? (
                                                    <span>
                                                            Product details unavailable
                                                        </span>
                                                ) : (
                                                    products.map(
                                                        (product, index) => (
                                                            <div
                                                                className="sales-product-entry"
                                                                key={`${sale.id}-${index}`}
                                                            >
                                                                <strong>
                                                                    {product.name}
                                                                </strong>

                                                                {products.length > 1 && (
                                                                    <span>
                                                                            Quantity:{" "}
                                                                        {product.quantity}
                                                                        </span>
                                                                )}
                                                            </div>
                                                        )
                                                    )
                                                )}
                                            </div>
                                        </td>

                                        <td>
                                            <div className="sales-batch-list">
                                                {batches.length > 0
                                                    ? batches.join(", ")
                                                    : "—"}
                                            </div>
                                        </td>

                                        <td>
                                                <span className="sales-quantity">
                                                    {getTotalQuantity(sale)}
                                                    <small> unit(s)</small>
                                                </span>
                                        </td>

                                        <td className="sales-amount">
                                            {formatCurrency(sale.totalAmount)}
                                        </td>

                                        <td>
                                            <button
                                                type="button"
                                                className="sales-btn sales-btn-view"
                                                onClick={() =>
                                                    openSaleDetails(sale)
                                                }
                                            >
                                                View Details →
                                            </button>
                                        </td>
                                    </tr>
                                );
                            })}
                            </tbody>
                        </table>
                    </div>
                )}

                {!loading && totalElements > 0 && (
                    <footer className="sales-pagination">
                        <span>
                            Showing {pageStart}–{pageEnd} of{" "}
                            {totalElements.toLocaleString("en-IN")} transactions
                        </span>

                        <div>
                            <button
                                type="button"
                                className="sales-btn sales-btn-secondary"
                                disabled={page === 0 || loading}
                                onClick={() => changePage(page - 1)}
                            >
                                ← Previous
                            </button>

                            <button
                                type="button"
                                className="sales-btn sales-btn-secondary"
                                disabled={
                                    page >= totalPages - 1 || loading
                                }
                                onClick={() => changePage(page + 1)}
                            >
                                Next →
                            </button>
                        </div>
                    </footer>
                )}
            </section>

            {selectedSale && (
                <div
                    className="sales-drawer-backdrop"
                    onMouseDown={(event) => {
                        if (event.target === event.currentTarget) {
                            closeSaleDetails();
                        }
                    }}
                >
                    <aside
                        className="sales-details-drawer"
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="sales-details-title"
                    >
                        <header className="sales-drawer-header">
                            <div>
                                <span className="sales-history-eyebrow">
                                    TRANSACTION DETAILS
                                </span>

                                <h2 id="sales-details-title">
                                    Sale #{selectedSale.id}
                                </h2>

                                <p>{formatDate(selectedSale.saleDate)}</p>
                            </div>

                            <button
                                type="button"
                                className="sales-drawer-close"
                                onClick={closeSaleDetails}
                                aria-label="Close details"
                                disabled={submittingItem !== null}
                            >
                                ×
                            </button>
                        </header>

                        <div className="sales-drawer-total">
                            <span>Total sale amount</span>

                            <strong>
                                {formatCurrency(selectedSale.totalAmount)}
                            </strong>
                        </div>

                        {loadingDetails ? (
                            <div className="sales-loading">
                                Loading item and return details...
                            </div>
                        ) : (
                            <>
                                <section className="sales-drawer-section">
                                    <h3>Purchased items</h3>

                                    <div className="sales-drawer-items">
                                        {getSaleItems(selectedSale).map((item) => {
                                            const sold =
                                                Number(item.quantity) || 0;

                                            const returned =
                                                returnedQuantities[item.id] || 0;

                                            const remaining = Math.max(
                                                sold - returned,
                                                0
                                            );

                                            return (
                                                <article
                                                    className="sales-drawer-item"
                                                    key={item.id}
                                                >
                                                    <div className="sales-item-top">
                                                        <div>
                                                            <h4>
                                                                {item.product?.name ||
                                                                    "Product"}
                                                            </h4>

                                                            <p>
                                                                Batch:{" "}
                                                                {item.batch?.batchNumber ||
                                                                    "—"}
                                                                {" · "}Item #{item.id}
                                                            </p>
                                                        </div>

                                                        <strong>
                                                            {formatCurrency(
                                                                Number(item.sellingPrice) *
                                                                sold
                                                            )}
                                                        </strong>
                                                    </div>

                                                    <div className="sales-item-metrics">
                                                        <div>
                                                            <span>Sold</span>
                                                            <strong>{sold}</strong>
                                                        </div>

                                                        <div>
                                                            <span>Returned</span>
                                                            <strong>{returned}</strong>
                                                        </div>

                                                        <div>
                                                            <span>Returnable</span>
                                                            <strong>{remaining}</strong>
                                                        </div>
                                                    </div>

                                                    <div className="sales-item-price">
                                                        Unit price:{" "}
                                                        {formatCurrency(
                                                            item.sellingPrice
                                                        )}
                                                    </div>

                                                    {remaining > 0 ? (
                                                        <form
                                                            className="sales-return-form"
                                                            onSubmit={(event) => {
                                                                event.preventDefault();
                                                                handleReturn(
                                                                    selectedSale,
                                                                    item
                                                                );
                                                            }}
                                                        >
                                                            <label>
                                                                <span>
                                                                    Return quantity
                                                                </span>

                                                                <input
                                                                    type="number"
                                                                    min="1"
                                                                    max={remaining}
                                                                    step="1"
                                                                    required
                                                                    value={
                                                                        returnQuantities[
                                                                            item.id
                                                                            ] ?? ""
                                                                    }
                                                                    disabled={
                                                                        submittingItem !== null
                                                                    }
                                                                    onChange={(event) =>
                                                                        setReturnQuantities(
                                                                            (previous) => ({
                                                                                ...previous,
                                                                                [item.id]:
                                                                                event.target.value,
                                                                            })
                                                                        )
                                                                    }
                                                                />
                                                            </label>

                                                            <button
                                                                className="sales-btn sales-btn-primary"
                                                                type="submit"
                                                                disabled={
                                                                    submittingItem !== null
                                                                }
                                                            >
                                                                {submittingItem === item.id
                                                                    ? "Processing..."
                                                                    : "Process Return"}
                                                            </button>
                                                        </form>
                                                    ) : (
                                                        <div className="sales-fully-returned">
                                                            ✓ Fully returned
                                                        </div>
                                                    )}
                                                </article>
                                            );
                                        })}
                                    </div>
                                </section>

                                <section className="sales-drawer-section">
                                    <h3>Return transaction history</h3>

                                    {returnHistory.length === 0 ? (
                                        <p className="sales-muted-text">
                                            No returns recorded for this sale.
                                        </p>
                                    ) : (
                                        <div className="sales-return-history">
                                            {returnHistory.map((transaction) => (
                                                <div
                                                    className="sales-return-history-row"
                                                    key={transaction.id}
                                                >
                                                    <div>
                                                        <strong>
                                                            Return #{transaction.id}
                                                        </strong>

                                                        <span>
                                                            {formatDate(
                                                                transaction.transactionDate
                                                            )}
                                                        </span>

                                                        <span>
                                                            Item #{
                                                            transaction.saleItem?.id ??
                                                            "—"
                                                        }
                                                        </span>
                                                    </div>

                                                    <strong>
                                                        {transaction.quantity} unit(s)
                                                    </strong>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </section>
                            </>
                        )}
                    </aside>
                </div>
            )}
        </div>
    );
}

export default SalesHistory;