import { useState } from "react";
import { login } from "./auth";
import Footer from "./Footer";

export default function LoginPage({ onLogin }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");

    if (!username.trim() || !password) {
      setError("Enter username and password.");
      return;
    }

    setLoading(true);
    try {
      const token = await login(username.trim(), password);
      onLogin(token);
    } catch (err) {
      setError(err.message || "Login failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <div className="login-content">
        <section className="login-panel">
          <div className="login-brand">
            <h1>PixServe</h1>
            <p>Sign in to manage your image library.</p>
          </div>

          <form className="login-form" onSubmit={handleSubmit}>
            <label>
              Username
              <input
                autoComplete="username"
                autoFocus
                type="text"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
              />
            </label>

            <label>
              Password
              <input
                autoComplete="current-password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </label>

            {error && <div className="login-error">{error}</div>}

            <button className="btn btn-primary login-submit" type="submit" disabled={loading}>
              {loading ? "Signing in..." : "Sign In"}
            </button>
          </form>
        </section>
        <Footer />
      </div>
    </main>
  );
}
