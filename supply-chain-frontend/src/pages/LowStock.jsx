import { useCallback, useEffect, useState } from "react";
import api from "../services/api";
import "./LowStock.css";

function LowStock() {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [search, setSearch] = useState("");

    const loadLowStock = useCallback(async () => {
        setLoading(true);
        setError("");

        try {
            const response = await api.get("/low-stock");
            const data = response.data;

            const records = Array.isArray(data)
                ? data
                : Array.isArray(data?.content)
                    ? data.content
                    : Array.isArray(data?.data)
                        ? data.data
                        : [];

            setProducts(records);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load low-stock information. Please try again."
            );
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadLowStock();
    }, [loadLowStock]);

    function getProductName(product) {
        return product.productName ?? product.name ?? "Unknown product";
    }

    function getAvailableStock(product) {
        return product.sellableStock
            ?? product.availableQuantity
            ?? product.sellableQuantity
            ?? product.currentStock
            ?? product.quantity
            ?? null;
    }

    function getThreshold(product) {
        return product.reorderThreshold ?? product.threshold ?? null;
    }

    function formatQuantity(value) {
        if (value === null || value === undefined) {
            return "—";
        }

        const number = Number(value);

        return Number.isFinite(number)
            ? number.toLocaleString("en-IN")
            : "—";
    }

    const filteredProducts = products.filter((product) => {
        const query = search.trim().toLowerCase();
        const name = String(getProductName(product)).toLowerCase();
        const sku = String(product.sku ?? "").toLowerCase();

        return name.includes(query) || sku.includes(query);
    });

    const totalReorderUnits = filteredProducts.reduce(
        (total, product) => {
            const stock = Number(getAvailableStock(product));
            const threshold = Number(getThreshold(product));

            if (
                getAvailableStock(product) === null ||
                getThreshold(product) === null ||
                !Number.isFinite(stock) ||
                !Number.isFinite(threshold)
            ) {
                return total;
            }

            return total + Math.max(0, threshold - stock);
        },
        0
    );

    return (
        <div className="low-stock-page">
            <header className="low-stock-header">
                <div>
                    <span className="low-stock-eyebrow">
                        INVENTORY CONTROL
                    </span>

                    <h1>Low Stock Monitoring</h1>

                    <p>
                        Identify products that require replenishment.
                        Expired batches are excluded from sellable stock.
                    </p>
                </div>

                <button
                    type="button"
                    className="low-stock-refresh"
                    onClick={loadLowStock}
                    disabled={loading}
                >
                    {loading ? "Refreshing..." : "↻ Refresh"}
                </button>
            </header>

            <section className="low-stock-summary">
                <div className="low-stock-summary-card">
                    <span>Products Requiring Attention</span>
                    <strong>{loading ? "—" : products.length}</strong>
                    <small>Based on backend stock rules</small>
                </div>

                <div className="low-stock-summary-card">
                    <span>Search Results</span>
                    <strong>{loading ? "—" : filteredProducts.length}</strong>
                    <small>Matching products</small>
                </div>

                <div className="low-stock-summary-card">
                    <span>Estimated Reorder Units</span>
                    <strong>{loading ? "—" : formatQuantity(totalReorderUnits)}</strong>
                    <small>Based on available stock and thresholds</small>
                </div>
            </section>

            <section className="low-stock-panel">
                <div className="low-stock-panel-header">
                    <div>
                        <h2>Replenishment Watchlist</h2>
                        <p>
                            Review available stock and reorder thresholds.
                        </p>
                    </div>

                    <input
                        type="search"
                        className="low-stock-search"
                        placeholder="Search name or SKU..."
                        value={search}
                        onChange={(event) => setSearch(event.target.value)}
                        aria-label="Search low-stock products"
                    />
                </div>

                {error && (
                    <div className="low-stock-error" role="alert">
                        <span>{error}</span>

                        <button
                            type="button"
                            onClick={loadLowStock}
                            disabled={loading}
                        >
                            Retry
                        </button>
                    </div>
                )}

                {loading ? (
                    <div className="low-stock-state">
                        Loading stock information...
                    </div>
                ) : error ? null : filteredProducts.length === 0 ? (
                    <div className="low-stock-state">
                        <div className="low-stock-empty-icon">✓</div>

                        <h3>
                            {products.length === 0
                                ? "No low-stock products"
                                : "No matching products"}
                        </h3>

                        <p>
                            {products.length === 0
                                ? "There are currently no products on the low-stock watchlist."
                                : "Try searching with a different product name or SKU."}
                        </p>
                    </div>
                ) : (
                    <div className="low-stock-table-wrapper">
                        <table className="low-stock-table">
                            <thead>
                            <tr>
                                <th>Product</th>
                                <th>SKU</th>
                                <th>Available Stock</th>
                                <th>Reorder Threshold</th>
                                <th>Reorder Quantity</th>
                                <th>Status</th>
                            </tr>
                            </thead>

                            <tbody>
                            {filteredProducts.map((product, index) => {
                                const stock = getAvailableStock(product);
                                const threshold = getThreshold(product);

                                const validStock =
                                    stock !== null &&
                                    stock !== undefined &&
                                    Number.isFinite(Number(stock));

                                const validThreshold =
                                    threshold !== null &&
                                    threshold !== undefined &&
                                    Number.isFinite(Number(threshold));

                                const reorderQuantity =
                                    validStock && validThreshold
                                        ? Math.max(
                                            0,
                                            Number(threshold) - Number(stock)
                                        )
                                        : null;

                                const lowStock =
                                    product.lowStock === true;

                                return (
                                    <tr
                                        key={
                                            product.productId ??
                                            product.id ??
                                            product.sku ??
                                            index
                                        }
                                    >
                                        <td className="low-stock-product-name">
                                            {getProductName(product)}
                                        </td>

                                        <td>{product.sku || "—"}</td>

                                        <td>{formatQuantity(stock)}</td>

                                        <td>{formatQuantity(threshold)}</td>

                                        <td>{formatQuantity(reorderQuantity)}</td>

                                        <td>
                                                <span className="low-stock-status">
                                                    {lowStock
                                                        ? "Replenishment Required"
                                                        : "Stock Sufficient"}
                                                </span>
                                        </td>
                                    </tr>
                                );
                            })}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>

            <div className="low-stock-note">
                <strong>Inventory rule:</strong> Expired and zero-quantity
                batches are excluded from sellable stock calculations.
                Reorder quantities are estimates based on the difference
                between the reorder threshold and available stock.
            </div>
        </div>
    );
}

export default LowStock;