import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api.js";
import "./Dashboard.css";

function Dashboard() {
    const navigate = useNavigate();

    const [analytics, setAnalytics] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    async function loadAnalytics() {
        setLoading(true);
        setError("");

        try {
            const response = await api.get("/analytics");
            setAnalytics(response.data);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load dashboard data."
            );
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadAnalytics();
    }, []);

    const money = (value) =>
        `₹${Number(value ?? 0).toLocaleString("en-IN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
        })}`;

    const cards = [
        {
            label: "Total Products",
            value: analytics?.totalProducts ?? 0,
            description: "Registered products",
            icon: "▦",
            color: "blue",
            path: "/products",
        },
        {
            label: "Total Batches",
            value: analytics?.totalBatches ?? 0,
            description: "Tracked inventory batches",
            icon: "▤",
            color: "purple",
            path: "/batches",
        },
        {
            label: "Inventory Quantity",
            value: analytics?.totalInventoryQuantity ?? 0,
            description: "Units currently in stock",
            icon: "▥",
            color: "green",
            path: "/batches",
        },
        {
            label: "Expired Batches",
            value: analytics?.expiredBatches ?? 0,
            description: "Require attention",
            icon: "!",
            color: "red",
            path: "/batches",
        },
        {
            label: "Near Expiry",
            value: analytics?.nearExpiryBatches ?? 0,
            description: "Monitor expiry risk",
            icon: "◷",
            color: "orange",
            path: "/batches",
        },
        {
            label: "Low Stock Products",
            value: analytics?.lowStockProducts ?? 0,
            description: "Review replenishment needs",
            icon: "↓",
            color: "amber",
            path: "/low-stock",
        },
        {
            label: "Total Sales",
            value: analytics?.totalSales ?? 0,
            description: "Recorded sales transactions",
            icon: "↗",
            color: "cyan",
            path: "/analytics",
        },
        {
            label: "Sales Revenue",
            value: money(analytics?.totalSalesRevenue),
            description: "Revenue from recorded sales",
            icon: "₹",
            color: "green",
            path: "/analytics",
        },
    ];

    if (loading && !analytics) {
        return (
            <div className="dashboard-state">
                <div className="dashboard-spinner" />
                <p>Loading your dashboard...</p>
            </div>
        );
    }

    if (error && !analytics) {
        return (
            <div className="dashboard-state dashboard-state-error">
                <h2>Unable to load dashboard</h2>
                <p>{error}</p>
                <button
                    className="dashboard-button"
                    onClick={loadAnalytics}
                    disabled={loading}
                >
                    Try Again
                </button>
            </div>
        );
    }

    if (!analytics) return null;

    return (
        <div className="dashboard-page">
            <header className="dashboard-header">
                <div>
                    <span className="dashboard-eyebrow">
                        SUPPLY CHAIN MANAGEMENT
                    </span>

                    <h1>Dashboard</h1>

                    <p>
                        Monitor your inventory, expiry risks and sales
                        performance in one place.
                    </p>
                </div>

                <button
                    className="dashboard-button"
                    onClick={loadAnalytics}
                    disabled={loading}
                >
                    <span className={loading ? "refresh-spinning" : ""}>
                        ↻
                    </span>
                    {loading ? "Refreshing..." : "Refresh Data"}
                </button>
            </header>

            {error && (
                <div className="dashboard-inline-error">
                    {error}
                </div>
            )}

            <section className="dashboard-section">
                <div className="dashboard-section-heading">
                    <div>
                        <h2>Business Overview</h2>
                        <p>Your current inventory and sales snapshot</p>
                    </div>

                    <span className="dashboard-live-badge">
                        <span />
                        LIVE DATA
                    </span>
                </div>

                <div className="stats-grid">
                    {cards.map((card) => (
                        <button
                            type="button"
                            className={`stat-card stat-${card.color}`}
                            key={card.label}
                            onClick={() => navigate(card.path)}
                            aria-label={`${card.label}: ${card.value}. Open related page.`}
                        >
                            <div className="stat-card-top">
                                <span className="stat-label">
                                    {card.label}
                                </span>

                                <span className="stat-icon">
                                    {card.icon}
                                </span>
                            </div>

                            <strong className="stat-value">
                                {card.value}
                            </strong>

                            <div className="stat-card-footer">
                                <span>{card.description}</span>
                                <span className="stat-arrow">↗</span>
                            </div>
                        </button>
                    ))}
                </div>
            </section>

            <section className="dashboard-bottom">
                <div className="inventory-value-card">
                    <div className="inventory-value-icon">₹</div>

                    <div className="inventory-value-content">
                        <span>INVENTORY VALUATION</span>
                        <h2>Inventory Purchase Value</h2>

                        <strong>
                            {money(
                                analytics.totalInventoryPurchaseValue
                            )}
                        </strong>

                        <p>
                            Purchase cost of the inventory currently
                            represented by your system's valuation.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="inventory-value-link"
                        onClick={() => navigate("/batches")}
                    >
                        View Batches <span>→</span>
                    </button>
                </div>

                <div className="dashboard-tip-card">
                    <span className="dashboard-tip-icon">✓</span>

                    <div>
                        <h3>Inventory at a glance</h3>
                        <p>
                            Review expiry risks and stock availability
                            regularly to help prevent avoidable losses.
                        </p>

                        <button
                            type="button"
                            onClick={() => navigate("/low-stock")}
                        >
                            Review Low Stock <span>→</span>
                        </button>
                    </div>
                </div>
            </section>
        </div>
    );
}

export default Dashboard;