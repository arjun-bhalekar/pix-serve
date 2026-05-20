import ImageGallery from "./ImageGallery";
import { useRef, useState } from "react";
import LoginPage from "./LoginPage";
import { clearAuthToken, getAuthToken } from "./auth";
import Footer from "./Footer";

function App() {
  const galleryRef = useRef();
  const [token, setToken] = useState(() => getAuthToken());

  const handleLogout = () => {
    clearAuthToken();
    setToken(null);
  };

  if (!token) {
    return <LoginPage onLogin={setToken} />;
  }

  return (
    <div className="page">
      <div className="top-bar">
        <h1 className="header">PixServe</h1>
        <button className="btn btn-warning" onClick={handleLogout}>
          Logout
        </button>
      </div>
      <ImageGallery ref={galleryRef} />

      <Footer />
    </div>
  );
}

export default App;
