import { NavLink } from "react-router-dom";
import { getRole, getUsername, logout } from "../services/authService";
import "./Sidebar.css";

function Sidebar() {
    const role = getRole();
    const isManager = role === "MANAGER";
    const username = getUsername();

    const managerLinks = [
        { to: "/dashboard", icon: "▦", label: "Dashboard" },
        { to: "/products", icon: "▣", label: "Products" },
        { to: "/batches", icon: "▤", label: "Batch Management" },
        { to: "/low-stock", icon: "!", label: "Low Stock" },
        { to: "/invoices", icon: "▧", label: "AI Invoice Scanner" },
        { to: "/analytics", icon: "◈", label: "Analytics & Reports" },
        { to: "/pos", icon: "▣", label: "Point of Sale" },
        { to: "/sales-history", icon: "↻", label: "Sales History" },
    ];

    const cashierLinks = [
        { to: "/pos", icon: "▣", label: "Point of Sale" },
        { to: "/sales-history", icon: "↻", label: "Sales History" },
    ];

    const links = isManager ? managerLinks : cashierLinks;

    return (
        <aside className="sidebar">
            <div className="sidebar-brand">
                <div className="brand-logo">SC</div>

                <div className="brand-text">
                    <h2>Supply Chain</h2>
                    <span>Expiry Risk Engine</span>
                </div>
            </div>

            <div className="sidebar-section-label">
                {isManager ? "MANAGEMENT" : "SALES TERMINAL"}
            </div>

            <nav className="sidebar-nav">
                {links.map((link) => (
                    <NavLink
                        key={link.to}
                        to={link.to}
                        className={({ isActive }) =>
                            `sidebar-link ${isActive ? "active" : ""}`
                        }
                    >
                        <span className="sidebar-link-icon">
                            {link.icon}
                        </span>

                        <span>{link.label}</span>
                    </NavLink>
                ))}
            </nav>

            <div className="sidebar-bottom">
                <div className="sidebar-status">
                    <span className="sidebar-status-dot"></span>
                    System connected
                </div>

                <div className="user-info">
                    <div className="user-avatar">
                        {(username || "U").charAt(0).toUpperCase()}
                    </div>

                    <div className="user-details">
                        <strong>{username || "User"}</strong>
                        <span>{isManager ? "Manager" : "Cashier"}</span>
                    </div>
                </div>

                <button
                    className="logout-button"
                    onClick={logout}
                    type="button"
                >
                    <span>↪</span>
                    Logout
                </button>
            </div>
        </aside>
    );
}

export default Sidebar;

