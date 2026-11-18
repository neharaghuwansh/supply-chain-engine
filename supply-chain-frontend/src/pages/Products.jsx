import { useEffect, useState } from "react";
import api from "../services/api";
import "./Products.css";

function Products() {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [search, setSearch] = useState("");

    const [showForm, setShowForm] = useState(false);
    const [saving, setSaving] = useState(false);
    const [success, setSuccess] = useState("");
    const [editingProduct, setEditingProduct] = useState(null);

    const initialForm = {
        name: "",
        sku: "",
        category: "",
        unit: "piece",
        baseSellingPrice: "",
        active: true,
    };

    const [form, setForm] = useState(initialForm);

    async function fetchProducts() {
        setLoading(true);
        setError("");

        try {
            const response = await api.get("/products");

            setProducts(
                Array.isArray(response.data) ? response.data : []
            );
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load products. Please try again."
            );
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        fetchProducts();
    }, []);

    function handleChange(event) {
        const { name, value, type, checked } = event.target;

        setForm((previous) => ({
            ...previous,
            [name]: type === "checkbox" ? checked : value,
        }));
    }

    function resetForm() {
        setForm(initialForm);
        setEditingProduct(null);
        setShowForm(false);
        setError("");
    }

    function startAdd() {
        if (showForm && !editingProduct) {
            resetForm();
            return;
        }

        setForm(initialForm);
        setEditingProduct(null);
        setShowForm(true);
        setError("");
        setSuccess("");
    }

    function startEdit(product) {
        setEditingProduct(product);

        setForm({
            name: product.name ?? "",
            sku: product.sku ?? "",
            category: product.category ?? "",
            unit: product.unit ?? "piece",
            baseSellingPrice: String(product.baseSellingPrice ?? ""),
            active: Boolean(product.active),
        });

        setShowForm(true);
        setError("");
        setSuccess("");

        window.scrollTo({
            top: 0,
            behavior: "smooth",
        });
    }

    async function handleSubmit(event) {
        event.preventDefault();

        if (saving) return;

        setError("");
        setSuccess("");

        const name = form.name.trim();
        const sku = form.sku.trim();
        const category = form.category.trim();
        const unit = form.unit.trim();
        const price = Number(form.baseSellingPrice);

        if (!name || !sku || !category || !unit) {
            setError("Please fill in all required fields.");
            return;
        }

        if (
            form.baseSellingPrice.trim() === "" ||
            !Number.isFinite(price) ||
            price <= 0
        ) {
            setError("Selling price must be greater than zero.");
            return;
        }

        const normalizedSku = sku.toUpperCase();

        const duplicateSku = products.some(
            (product) =>
                product.id !== editingProduct?.id &&
                product.sku?.trim().toUpperCase() === normalizedSku
        );

        if (duplicateSku) {
            setError(
                "A product with this SKU already exists. Please use a unique SKU."
            );
            return;
        }

        const payload = {
            name,
            sku,
            category,
            unit,
            baseSellingPrice: price,
            active: form.active,
        };

        setSaving(true);

        try {
            if (editingProduct) {
                await api.put(
                    `/products/${editingProduct.id}`,
                    payload
                );

                setSuccess("Product updated successfully.");
            } else {
                await api.post("/products", payload);

                setSuccess("Product created successfully.");
            }

            setForm(initialForm);
            setEditingProduct(null);
            setShowForm(false);

            await fetchProducts();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                err.response?.data?.error ||
                `Unable to ${editingProduct ? "update" : "create"} product. Please check the details.`
            );
        } finally {
            setSaving(false);
        }
    }

    const filteredProducts = products.filter((product) => {
        const query = search.trim().toLowerCase();

        return (
            product.name?.toLowerCase().includes(query) ||
            product.sku?.toLowerCase().includes(query) ||
            product.category?.toLowerCase().includes(query)
        );
    });

    const activeProducts = filteredProducts.filter(
        (product) => product.active
    );

    const inactiveProducts = filteredProducts.filter(
        (product) => !product.active
    );

    const totalActive = products.filter(
        (product) => product.active
    ).length;

    const totalInactive = products.filter(
        (product) => !product.active
    ).length;

    function renderProductTable(productList, isInactive = false) {
        if (productList.length === 0) {
            return (
                <div className={`product-empty-state ${isInactive ? "empty-inactive" : "empty-active"}`}>
                    <div className="empty-state-icon">
                        {isInactive ? "—" : "+"}
                    </div>

                    <strong>
                        {search.trim()
                            ? "No matching products"
                            : isInactive
                                ? "No inactive products"
                                : "No active products"}
                    </strong>

                    <p>
                        {search.trim()
                            ? "Try searching with a different name, SKU, or category."
                            : isInactive
                                ? "Products you deactivate will appear here."
                                : "Add a product or activate an existing product to see it here."}
                    </p>
                </div>
            );
        }

        return (
            <div className="products-table-wrapper">
                <table className="products-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Product Name</th>
                            <th>SKU</th>
                            <th>Category</th>
                            <th>Unit</th>
                            <th>Selling Price</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>

                    <tbody>
                        {productList.map((product) => (
                            <tr key={product.id}>
                                <td>
                                    <span className="product-id">
                                        #{product.id}
                                    </span>
                                </td>

                                <td className="product-name-cell">
                                    {product.name}
                                </td>

                                <td>
                                    <span className="product-sku">
                                        {product.sku}
                                    </span>
                                </td>

                                <td>{product.category || "—"}</td>
                                <td>{product.unit || "—"}</td>

                                <td className="product-price">
                                    ₹
                                    {Number(
                                        product.baseSellingPrice ?? 0
                                    ).toFixed(2)}
                                </td>

                                <td>
                                    <span
                                        className={`product-status ${
    product.active
        ? "status-active"
        : "status-inactive"
}`}
                                    >
                                        <span className="status-dot" />
                                        {product.active
                                            ? "Active"
                                            : "Inactive"}
                                    </span>
                                </td>

                                <td>
                                    <button
                                        type="button"
                                        className="product-edit-button"
                                        onClick={() => startEdit(product)}
                                        disabled={saving}
                                    >
                                        Edit <span aria-hidden="true">↗</span>
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        );
    }

    return (
        <div className="products-page">
            <header className="products-header">
                <div className="products-title-group">
                    <span className="products-eyebrow">
                        INVENTORY MANAGEMENT
                    </span>

                    <h1>Product Catalogue</h1>

                    <p>
                        Manage your products, pricing, and availability
                        from one place.
                    </p>
                </div>

                <button
                    type="button"
                    className="primary-button add-product-button"
                    onClick={startAdd}
                >
                    <span className="add-icon" aria-hidden="true">+</span>
                    {showForm && !editingProduct
                        ? "Cancel"
                        : "Add Product"}
                </button>
            </header>

            <section className="products-summary">
                <div className="summary-card summary-total">
                    <div className="summary-card-top">
                        <span className="summary-icon total-icon">▦</span>
                        <span className="summary-label">ALL PRODUCTS</span>
                    </div>

                    <strong>{products.length}</strong>

                    <p>Products registered in your catalogue</p>
                </div>

                <div className="summary-card summary-active">
                    <div className="summary-card-top">
                        <span className="summary-icon active-icon">✓</span>
                        <span className="summary-label">ACTIVE PRODUCTS</span>
                    </div>

                    <strong>{totalActive}</strong>

                    <p>Products currently marked as active</p>
                </div>

                <div className="summary-card summary-inactive">
                    <div className="summary-card-top">
                        <span className="summary-icon inactive-icon">Ⅱ</span>
                        <span className="summary-label">INACTIVE PRODUCTS</span>
                    </div>

                    <strong>{totalInactive}</strong>

                    <p>Products currently marked as inactive</p>
                </div>
            </section>

            {success && (
                <div className="products-success" role="status">
                    <span className="message-icon">✓</span>
                    {success}
                </div>
            )}

            {error && (
                <div className="products-error" role="alert">
                    <span className="message-icon">!</span>
                    {error}
                </div>
            )}

            {showForm && (
                <section className="product-form-card">
                    <div className="form-heading">
                        <div className="form-heading-icon">✎</div>

                        <div>
                            <h2>
                                {editingProduct
                                    ? "Edit Product"
                                    : "Add New Product"}
                            </h2>

                            <p>
                                Enter the product details below.
                            </p>
                        </div>
                    </div>

                    <form onSubmit={handleSubmit}>
                        <div className="product-form-grid">
                            <div className="product-field">
                                <label htmlFor="name">
                                    Product Name
                                </label>

                                <input
                                    id="name"
                                    name="name"
                                    type="text"
                                    value={form.name}
                                    onChange={handleChange}
                                    placeholder="e.g. Paracetamol 500mg"
                                    maxLength={150}
                                    required
                                />
                            </div>

                            <div className="product-field">
                                <label htmlFor="sku">SKU</label>

                                <input
                                    id="sku"
                                    name="sku"
                                    type="text"
                                    value={form.sku}
                                    onChange={handleChange}
                                    placeholder="e.g. PCM-500"
                                    maxLength={100}
                                    required
                                />
                            </div>

                            <div className="product-field">
                                <label htmlFor="category">
                                    Category
                                </label>

                                <input
                                    id="category"
                                    name="category"
                                    type="text"
                                    value={form.category}
                                    onChange={handleChange}
                                    placeholder="e.g. Medicine"
                                    maxLength={100}
                                    required
                                />
                            </div>

                            <div className="product-field">
                                <label htmlFor="unit">Unit</label>

                                <input
                                    id="unit"
                                    name="unit"
                                    type="text"
                                    value={form.unit}
                                    onChange={handleChange}
                                    placeholder="e.g. piece, box, kg"
                                    maxLength={50}
                                    required
                                />
                            </div>

                            <div className="product-field">
                                <label htmlFor="baseSellingPrice">
                                    Base Selling Price (₹)
                                </label>

                                <input
                                    id="baseSellingPrice"
                                    name="baseSellingPrice"
                                    type="number"
                                    min="0.01"
                                    step="0.01"
                                    value={form.baseSellingPrice}
                                    onChange={handleChange}
                                    placeholder="e.g. 25.00"
                                    required
                                />
                            </div>

                            <div className="product-field checkbox-field">
                                <label className="active-checkbox-label">
                                    <input
                                        name="active"
                                        type="checkbox"
                                        checked={form.active}
                                        onChange={handleChange}
                                    />

                                    <span>
                                        <strong>Active product</strong>
                                        <small>
                                            Available for operational use
                                        </small>
                                    </span>
                                </label>
                            </div>
                        </div>

                        <div className="product-form-actions">
                            <button
                                type="button"
                                className="secondary-button"
                                onClick={resetForm}
                                disabled={saving}
                            >
                                Cancel
                            </button>

                            <button
                                type="submit"
                                className="primary-button"
                                disabled={saving}
                            >
                                {saving
                                    ? "Saving..."
                                    : editingProduct
                                        ? "Save Changes"
                                        : "Create Product"}
                            </button>
                        </div>
                    </form>
                </section>
            )}

            <section className="products-catalogue">
                <div className="catalogue-header">
                    <div>
                        <span className="products-eyebrow">
                            YOUR INVENTORY
                        </span>

                        <h2>Manage Products</h2>

                        <p>
                            Browse, search, and update your registered products.
                        </p>
                    </div>

                    <label className="search-container">
                        <span className="search-icon" aria-hidden="true">
                            ⌕
                        </span>

                        <input
                            className="product-search"
                            type="search"
                            placeholder="Search name, SKU or category..."
                            value={search}
                            onChange={(event) =>
                                setSearch(event.target.value)
                            }
                            aria-label="Search products"
                        />

                        {search && (
                            <button
                                type="button"
                                className="clear-search"
                                onClick={() => setSearch("")}
                                aria-label="Clear search"
                            >
                                ×
                            </button>
                        )}
                    </label>
                </div>

                {loading ? (
                    <div className="products-message">
                        <span className="loading-indicator" />
                        Loading your products...
                    </div>
                ) : error && products.length === 0 ? (
                    <div className="products-message">
                        <p>Unable to display products. Please try again.</p>

                        <button
                            type="button"
                            className="secondary-button"
                            onClick={fetchProducts}
                            disabled={loading}
                        >
                            Retry
                        </button>
                    </div>
                ) : (
                    <>
                        <section className="product-section">
                            <div className="product-section-heading active-section-heading">
                                <div className="section-heading-left">
                                    <span className="section-icon active-section-icon">
                                        ✓
                                    </span>

                                    <div>
                                        <h3>Active Products</h3>
                                        <p>
                                            Products currently enabled in your catalogue
                                        </p>
                                    </div>
                                </div>

                                <span className="product-section-count active-count">
                                    {activeProducts.length}
                                </span>
                            </div>

                            {renderProductTable(activeProducts)}
                        </section>

                        <section className="product-section inactive-product-section">
                            <div className="product-section-heading inactive-section-heading">
                                <div className="section-heading-left">
                                    <span className="section-icon inactive-section-icon">
                                        Ⅱ
                                    </span>

                                    <div>
                                        <h3>Inactive Products</h3>
                                        <p>
                                            Discontinued or disabled catalogue items
                                        </p>
                                    </div>
                                </div>

                                <span className="product-section-count inactive-count">
                                    {inactiveProducts.length}
                                </span>
                            </div>

                            <div className="inactive-products-card">
                                {renderProductTable(inactiveProducts, true)}
                            </div>
                        </section>

                        <div className="catalogue-footer">
                            <span>
                                Showing <strong>{filteredProducts.length}</strong> of{" "}
                                <strong>{products.length}</strong> products
                            </span>

                            <span className="footer-status">
                                <span className="footer-status-dot" />
                                Catalogue synced
                            </span>
                        </div>
                    </>
                )}
            </section>
        </div>
    );
}

export default Products;

