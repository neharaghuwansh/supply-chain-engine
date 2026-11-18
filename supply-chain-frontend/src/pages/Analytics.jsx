import { useCallback, useEffect, useState } from "react";
import api from "../services/api";
import "./Analytics.css";

function getLocalDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

function getDefaultStartDate() {
    const date = new Date();
    date.setDate(date.getDate() - 29);
    return getLocalDate(date);
}

function formatCurrency(value) {
    const amount = Number(value ?? 0);

    return new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 2,
    }).format(Number.isFinite(amount) ? amount : 0);
}

function formatNumber(value) {
    const number = Number(value ?? 0);

    return Number.isFinite(number)
        ? number.toLocaleString("en-IN")
        : "0";
}

function formatDate(value) {
    if (!value) return "—";

    return new Date(`${value}T00:00:00`).toLocaleDateString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
    });
}

function Analytics() {
    const [reports, setReports] = useState(null);
    const [startDate, setStartDate] = useState(getDefaultStartDate);
    const [endDate, setEndDate] = useState(() =>
        getLocalDate(new Date())
    );

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [lastUpdated, setLastUpdated] = useState(null);

    const loadReports = useCallback(async () => {
        if (!startDate || !endDate) {
            setError("Please select both report dates.");
            return;
        }

        if (startDate > endDate) {
            setError("Start date cannot be after end date.");
            return;
        }

        setLoading(true);
        setError("");

        try {
            const response = await api.get("/analytics/reports", {
                params: {
                    startDate,
                    endDate,
                },
            });

            setReports(response.data);
            setLastUpdated(new Date());
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load reports. Check your manager account and backend."
            );
        } finally {
            setLoading(false);
        }
    }, [startDate, endDate]);

    useEffect(() => {
        loadReports();
    }, [loadReports]);

    const salesTrend = reports?.salesTrend ?? [];
    const topProducts = reports?.topSellingProducts ?? [];

    const movement = reports?.inventoryMovement ?? {
        purchased: 0,
        sold: 0,
        returned: 0,
        damaged: 0,
        expired: 0,
        adjusted: 0,
    };

    const expiryRisk = reports?.expiryRisk ?? {
        upcomingExpiryBatches: 0,
        upcomingExpiryQuantity: 0,
        purchaseValueAtRisk: 0,
    };

    const totalRevenue = salesTrend.reduce(
        (total, item) => total + Number(item.revenue ?? 0),
        0
    );

    const totalSales = salesTrend.reduce(
        (total, item) => total + Number(item.numberOfSales ?? 0),
        0
    );

    const maxRevenue = Math.max(
        1,
        ...salesTrend.map((item) => Number(item.revenue ?? 0))
    );

    const movementMetrics = [
        {
            label: "Units Purchased",
            value: movement.purchased,
            description: "Stock received into inventory",
            color: "blue",
            icon: "↓",
        },
        {
            label: "Units Sold",
            value: movement.sold,
            description: "Stock sold during the period",
            color: "green",
            icon: "↗",
        },
        {
            label: "Units Returned",
            value: movement.returned,
            description: "Recorded customer returns",
            color: "purple",
            icon: "↩",
        },
        {
            label: "Units Damaged",
            value: movement.damaged,
            description: "Stock written off as damaged",
            color: "red",
            icon: "!",
        },
        {
            label: "Units Expired",
            value: movement.expired,
            description: "Stock written off after expiry",
            color: "orange",
            icon: "◷",
        },
        {
            label: "Net Stock Adjustment",
            value: movement.adjusted,
            description: "Positive or negative adjustments combined",
            color: "yellow",
            icon: "±",
        },
    ];

    return (
        <div className="analytics-page">
            <header className="analytics-header">
                <div>
                    <span className="analytics-eyebrow">
                        BUSINESS INTELLIGENCE
                    </span>

                    <h1>Analytics & Reports</h1>

                    <p>
                        Understand sales performance, inventory movement,
                        product demand, and upcoming expiry exposure.
                    </p>

                    {lastUpdated && (
                        <small>
                            Last updated:{" "}
                            {lastUpdated.toLocaleTimeString("en-IN", {
                                hour: "2-digit",
                                minute: "2-digit",
                            })}
                        </small>
                    )}
                </div>

                <button
                    type="button"
                    className="analytics-refresh-button"
                    onClick={loadReports}
                    disabled={loading}
                >
                    ↻ {loading ? "Loading..." : "Refresh Reports"}
                </button>
            </header>

            <section className="analytics-filter-panel">
                <div className="analytics-filter-field">
                    <label htmlFor="analytics-start-date">
                        Start date
                    </label>

                    <input
                        id="analytics-start-date"
                        type="date"
                        value={startDate}
                        max={endDate}
                        onChange={(event) =>
                            setStartDate(event.target.value)
                        }
                    />
                </div>

                <div className="analytics-filter-field">
                    <label htmlFor="analytics-end-date">
                        End date
                    </label>

                    <input
                        id="analytics-end-date"
                        type="date"
                        value={endDate}
                        min={startDate}
                        max={getLocalDate(new Date())}
                        onChange={(event) =>
                            setEndDate(event.target.value)
                        }
                    />
                </div>

                <button
                    type="button"
                    className="analytics-apply-button"
                    onClick={loadReports}
                    disabled={loading || !startDate || !endDate}
                >
                    Apply Filters
                </button>
            </section>

            {error && (
                <div className="analytics-error" role="alert">
                    <span>{error}</span>

                    <button
                        type="button"
                        onClick={loadReports}
                        disabled={loading}
                    >
                        Retry
                    </button>
                </div>
            )}

            {loading && !reports ? (
                <div className="analytics-loading">
                    Loading business reports...
                </div>
            ) : reports ? (
                <>
                    {/* SALES PERFORMANCE */}

                    <section className="analytics-section">
                        <div className="analytics-section-heading">
                            <div>
                                <h2>Sales Performance</h2>
                                <p>
                                    Revenue and transaction activity for the
                                    selected period.
                                </p>
                            </div>

                            <span>SALES REPORT</span>
                        </div>

                        <div className="analytics-report-summary">
                            <article className="analytics-report-summary-card">
                                <span>Total Revenue</span>

                                <strong>
                                    {formatCurrency(totalRevenue)}
                                </strong>

                                <small>
                                    Revenue from recorded sales
                                </small>
                            </article>

                            <article className="analytics-report-summary-card">
                                <span>Total Transactions</span>

                                <strong>
                                    {formatNumber(totalSales)}
                                </strong>

                                <small>
                                    Recorded sales transactions
                                </small>
                            </article>

                            <article className="analytics-report-summary-card">
                                <span>Reporting Period</span>

                                <strong>
                                    {formatNumber(
                                        Math.round(
                                            (
                                                new Date(`${endDate}T00:00:00`) -
                                                new Date(`${startDate}T00:00:00`)
                                            ) / 86400000
                                        ) + 1
                                    )}{" "}
                                    days
                                </strong>

                                <small>
                                    {formatDate(startDate)} – {formatDate(endDate)}
                                </small>
                            </article>
                        </div>

                        <div className="analytics-report-panel">
                            <h3>Daily Revenue Trend</h3>

                            {salesTrend.length === 0 ? (
                                <p className="analytics-empty-state">
                                    No sales were recorded during this period.
                                </p>
                            ) : (
                                <div className="analytics-trend-list">
                                    {salesTrend.map((item) => {
                                        const revenue = Number(
                                            item.revenue ?? 0
                                        );

                                        const width =
                                            (revenue / maxRevenue) * 100;

                                        return (
                                            <div
                                                className="analytics-trend-row"
                                                key={item.date}
                                            >
                                                <span className="analytics-trend-date">
                                                    {formatDate(item.date)}
                                                </span>

                                                <div className="analytics-trend-track">
                                                    <div
                                                        className="analytics-trend-bar"
                                                        style={{
                                                            width: `${width}%`,
                                                        }}
                                                    />
                                                </div>

                                                <strong>
                                                    {formatCurrency(revenue)}
                                                </strong>

                                                <small>
                                                    {formatNumber(
                                                        item.numberOfSales
                                                    )}{" "}
                                                    sales
                                                </small>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    </section>

                    {/* TOP-SELLING PRODUCTS */}

                    <section className="analytics-section">
                        <div className="analytics-section-heading">
                            <div>
                                <h2>Top-Selling Products</h2>
                                <p>
                                    Products ranked by units sold during the
                                    selected period.
                                </p>
                            </div>

                            <span>PRODUCT DEMAND</span>
                        </div>

                        <div className="analytics-report-panel">
                            {topProducts.length === 0 ? (
                                <p className="analytics-empty-state">
                                    No product sales were recorded during
                                    this period.
                                </p>
                            ) : (
                                <div className="analytics-table-wrapper">
                                    <table className="analytics-report-table">
                                        <thead>
                                        <tr>
                                            <th>Rank</th>
                                            <th>Product</th>
                                            <th>Quantity Sold</th>
                                            <th>Revenue</th>
                                        </tr>
                                        </thead>

                                        <tbody>
                                        {topProducts.map(
                                            (product, index) => (
                                                <tr
                                                    key={product.productId}
                                                >
                                                    <td>
                                                            <span className="analytics-rank">
                                                                {index + 1}
                                                            </span>
                                                    </td>

                                                    <td>
                                                        <strong>
                                                            {product.productName}
                                                        </strong>
                                                    </td>

                                                    <td>
                                                        {formatNumber(
                                                            product.quantitySold
                                                        )}
                                                    </td>

                                                    <td>
                                                        <strong>
                                                            {formatCurrency(
                                                                product.revenue
                                                            )}
                                                        </strong>
                                                    </td>
                                                </tr>
                                            )
                                        )}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </section>

                    {/* INVENTORY MOVEMENT */}

                    <section className="analytics-section">
                        <div className="analytics-section-heading">
                            <div>
                                <h2>Inventory Movement</h2>
                                <p>
                                    Recorded stock movements during the
                                    selected period.
                                </p>
                            </div>

                            <span>STOCK ACTIVITY</span>
                        </div>

                        <div className="analytics-metric-grid">
                            {movementMetrics.map((metric) => (
                                <article
                                    className="analytics-metric-card"
                                    key={metric.label}
                                >
                                    <div className="analytics-metric-top">
                                        <span>{metric.label}</span>

                                        <div
                                            className={`analytics-metric-icon ${metric.color}`}
                                        >
                                            {metric.icon}
                                        </div>
                                    </div>

                                    <strong className="analytics-metric-value">
                                        {formatNumber(metric.value)}
                                    </strong>

                                    <span className="analytics-metric-description">
                                        {metric.description}
                                    </span>
                                </article>
                            ))}
                        </div>

                        <div className="analytics-report-note">
                            Stock adjustments are shown as a net quantity:
                            increases minus decreases. Returns represent
                            recorded return transactions and may not always
                            increase sellable stock.
                        </div>
                    </section>

                    {/* EXPIRY RISK */}

                    <section className="analytics-section">
                        <div className="analytics-section-heading">
                            <div>
                                <h2>Upcoming Expiry Risk</h2>
                                <p>
                                    Current inventory exposure for batches
                                    expiring within the next 30 days.
                                </p>
                            </div>

                            <span>EXPIRY MONITORING</span>
                        </div>

                        <div className="analytics-metric-grid">
                            <article className="analytics-metric-card">
                                <div className="analytics-metric-top">
                                    <span>At-Risk Batches</span>

                                    <div className="analytics-metric-icon orange">
                                        ◷
                                    </div>
                                </div>

                                <strong className="analytics-metric-value">
                                    {formatNumber(
                                        expiryRisk.upcomingExpiryBatches
                                    )}
                                </strong>

                                <span className="analytics-metric-description">
                                    Non-archived batches with remaining stock
                                </span>
                            </article>

                            <article className="analytics-metric-card">
                                <div className="analytics-metric-top">
                                    <span>Units at Risk</span>

                                    <div className="analytics-metric-icon red">
                                        !
                                    </div>
                                </div>

                                <strong className="analytics-metric-value">
                                    {formatNumber(
                                        expiryRisk.upcomingExpiryQuantity
                                    )}
                                </strong>

                                <span className="analytics-metric-description">
                                    Remaining units in upcoming expiry batches
                                </span>
                            </article>

                            <article className="analytics-financial-card">
                                <span>Purchase Value at Risk</span>

                                <strong>
                                    {formatCurrency(
                                        expiryRisk.purchaseValueAtRisk
                                    )}
                                </strong>

                                <small>
                                    Estimated purchase cost of remaining stock,
                                    not lost profit.
                                </small>
                            </article>
                        </div>

                        <div className="analytics-insight-card">
                            <div className="analytics-insight-icon">
                                ◈
                            </div>

                            <div>
                                <h2>Expiry Risk Assessment</h2>

                                {Number(
                                    expiryRisk.upcomingExpiryBatches ?? 0
                                ) > 0 ? (
                                    <p>
                                        There are{" "}
                                        {formatNumber(
                                            expiryRisk.upcomingExpiryBatches
                                        )}{" "}
                                        upcoming expiry batch(es), containing{" "}
                                        {formatNumber(
                                            expiryRisk.upcomingExpiryQuantity
                                        )}{" "}
                                        units with an estimated purchase value
                                        of{" "}
                                        {formatCurrency(
                                            expiryRisk.purchaseValueAtRisk
                                        )}
                                        . Review these batches and consider
                                        appropriate clearance pricing.
                                    </p>
                                ) : (
                                    <p>
                                        No non-archived batches with remaining
                                        stock are currently reported as expiring
                                        within the next 30 days.
                                    </p>
                                )}
                            </div>
                        </div>
                    </section>
                </>
            ) : (
                !loading && (
                    <div className="analytics-loading">
                        No analytics report data is available.
                    </div>
                )
            )}
        </div>
    );
}

export default Analytics;