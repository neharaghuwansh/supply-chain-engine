import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { login } from "../services/authService";
import "./login.css";

function Login() {
    const navigate = useNavigate();

    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    async function handleSubmit(event) {
        event.preventDefault();

        setError("");
        setLoading(true);

        try {
            // Remove accidental spaces from the username only.
            const response = await login(username.trim(), password);

            if (response.role === "MANAGER") {
                navigate("/dashboard");
            } else if (response.role === "CASHIER") {
                navigate("/pos");
            } else {
                setError("Your account does not have a supported role.");
            }
        } catch (err) {
            if (err.response) {
                setError(
                    err.response.data?.message ||
                    `Login failed (HTTP ${err.response.status}).`
                );
            } else if (err.request) {
                setError(
                    "Cannot connect to the server. Check that the backend is running."
                );
            } else {
                setError("An unexpected error occurred. Please try again.");
            }
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="login-page">
            <div className="login-card">
                <div className="login-header">
                    <div className="logo">SC</div>

                    <h1>Supply Chain Engine</h1>

                    <p>Inventory & Expiry Risk Management</p>
                </div>

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="username">Username</label>

                        <input
                            id="username"
                            type="text"
                            value={username}
                            onChange={(event) =>
                                setUsername(event.target.value)
                            }
                            placeholder="Enter username"
                            autoComplete="username"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="password">Password</label>

                        <input
                            id="password"
                            type="password"
                            value={password}
                            onChange={(event) =>
                                setPassword(event.target.value)
                            }
                            placeholder="Enter password"
                            autoComplete="current-password"
                            required
                        />
                    </div>

                    {error && (
                        <div className="error-message" role="alert">
                            {error}
                        </div>
                    )}

                    <button type="submit" disabled={loading}>
                        {loading ? "Signing in..." : "Sign In"}
                    </button>
                </form>

                <div className="demo-info">
                    <strong>Demo Accounts</strong>

                    <span>Manager: manager1 / Manager@123</span>

                    <span>Cashier: cashier1 / Cashier@123</span>
                </div>
            </div>
        </div>
    );
}

export default Login;