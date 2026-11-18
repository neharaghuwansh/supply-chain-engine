
import {
    BrowserRouter,
    Navigate,
    Route,
    Routes,
} from "react-router-dom";

import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Products from "./pages/Products";
import Batches from "./pages/Batches";
import Invoices from "./pages/Invoices";
import POS from "./pages/POS";
import Analytics from "./pages/Analytics";
import LowStock from "./pages/LowStock";
import SalesHistory from "./pages/SalesHistory";

import ProtectedRoute from "./components/ProtectedRoute";
import ManagerLayout from "./components/ManagerLayout";
import { getRole } from "./services/authService";

function ManagerRoute({ children }) {
    return (
        <ProtectedRoute>
            {getRole() === "MANAGER" ? (
                <ManagerLayout>{children}</ManagerLayout>
            ) : (
                <Navigate to="/pos" replace />
            )}
        </ProtectedRoute>
    );
}

function POSRoute({ children }) {
    return (
        <ProtectedRoute>
            {["MANAGER", "CASHIER"].includes(getRole()) ? (
                <ManagerLayout>{children}</ManagerLayout>
            ) : (
                <Navigate to="/login" replace />
            )}
        </ProtectedRoute>
    );
}

function App() {
    const role = getRole();

    const defaultRoute = role === "CASHIER"
        ? "/pos"
        : "/dashboard";

    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<Login />} />

                <Route
                    path="/dashboard"
                    element={
                        <ManagerRoute>
                            <Dashboard />
                        </ManagerRoute>
                    }
                />

                <Route
                    path="/products"
                    element={
                        <ManagerRoute>
                            <Products />
                        </ManagerRoute>
                    }
                />

                <Route
                    path="/batches"
                    element={
                        <ManagerRoute>
                            <Batches />
                        </ManagerRoute>
                    }
                />

                <Route
                    path="/low-stock"
                    element={
                        <ManagerRoute>
                            <LowStock />
                        </ManagerRoute>
                    }
                />

                <Route
                    path="/invoices"
                    element={
                        <ManagerRoute>
                            <Invoices />
                        </ManagerRoute>
                    }
                />

                <Route
                    path="/analytics"
                    element={
                        <ManagerRoute>
                            <Analytics />
                        </ManagerRoute>
                    }
                />

                <Route
                    path="/pos"
                    element={
                        <POSRoute>
                            <POS />
                        </POSRoute>
                    }
                />

                <Route
                    path="/sales-history"
                    element={
                        <POSRoute>
                            <SalesHistory />
                        </POSRoute>
                    }
                />

                <Route
                    path="/"
                    element={<Navigate to={defaultRoute} replace />}
                />

                <Route
                    path="*"
                    element={<Navigate to={defaultRoute} replace />}
                />
            </Routes>
        </BrowserRouter>
    );
}

export default App;

