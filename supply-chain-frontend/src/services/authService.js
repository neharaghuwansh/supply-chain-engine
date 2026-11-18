import api from "./api";

export async function login(username, password) {
    const response = await api.post("/auth/login", {
        username,
        password,
    });

    const { token, username: loggedInUsername, role } = response.data;

    localStorage.setItem("token", token);
    localStorage.setItem("username", loggedInUsername);
    localStorage.setItem("role", role);

    return response.data;
}

export function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    localStorage.removeItem("role");

    window.location.href = "/login";
}

export function isAuthenticated() {
    return Boolean(localStorage.getItem("token"));
}

export function getRole() {
    return localStorage.getItem("role");
}

export function getUsername() {
    return localStorage.getItem("username");
}