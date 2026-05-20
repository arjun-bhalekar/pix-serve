import config from "./config";

const TOKEN_KEY = "pixserve_auth_token";

export function getAuthToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setAuthToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearAuthToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export async function login(username, password) {
  const response = await fetch(`${config.authBaseUrl}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });

  if (!response.ok) {
    throw new Error(response.status === 401 ? "Invalid username or password" : "Login failed");
  }

  const data = await response.json();
  if (!data?.token) {
    throw new Error("Login response did not include a token");
  }

  setAuthToken(data.token);
  return data.token;
}

export function authFetch(url, options = {}) {
  const token = getAuthToken();
  const headers = new Headers(options.headers || {});

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  return fetch(url, {
    ...options,
    headers,
  });
}
