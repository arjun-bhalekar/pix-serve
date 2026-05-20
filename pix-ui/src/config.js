const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
const serviceBaseUrl = apiBaseUrl.endsWith("/api")
  ? apiBaseUrl.slice(0, -4)
  : apiBaseUrl;

const config = {
  apiBaseUrl,
  authBaseUrl: import.meta.env.VITE_AUTH_BASE_URL || serviceBaseUrl,
};

export default config;
