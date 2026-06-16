import ImageGallery from "./ImageGallery";
import { useRef, useState } from "react";
import LoginPage from "./LoginPage";
import {authFetch, clearAuthToken, getAuthToken} from "./auth";
import Footer from "./Footer";
import config from "./config.js";

function App() {
  const galleryRef = useRef();
  const [token, setToken] = useState(() => getAuthToken());
  const [toolbarState, setToolbarState] = useState({
    selectedCount: 0,
    selectAll: false,
  });

  const handleLogout = () => {
    clearAuthToken();
    setToken(null);
  };

  const [importing, setImporting] = useState(false);

  const handleBulkImport = async () => {

    const confirmed = window.confirm(
        "Start bulk import from configured source directory?"
    );
    if (!confirmed) {
      return;
    }
    try {
      setImporting(true);
      const response = await authFetch(
          `${config.apiBaseUrl}/admin/bulk-import`,
          {
            method: "POST"
          }
      );
      if (!response.ok) {
        throw new Error("Bulk import failed");
      }
      const data = await response.json();
      alert(
          `Imported ${data["files-uploaded"]} of ${data["files-found"]} files`
      );
      // refresh gallery
      galleryRef.current?.reload();

    } catch (error) {
      console.error(error);
      alert("Bulk import failed");
    } finally {
      setImporting(false);
    }

  };

  if (!token) {
    return <LoginPage onLogin={setToken} />;
  }

  return (
    <div className="page">
      <div className="top-bar">
        <button className="brand-menu-item" onClick={() => window.location.reload()}>
          PixServe
        </button>
        <nav className="top-menu" aria-label="Primary actions">
          <button className="menu-item" onClick={() => galleryRef.current?.openUpload()}>
            Upload
          </button>
          <button className="menu-item" onClick={() => galleryRef.current?.openBulkUpload()}>
            Bulk Upload
          </button>

          <button className="menu-item" onClick={handleBulkImport} disabled={importing}>
            {importing ? "Importing..." : "Bulk-Import-Server"}
          </button>
          <button className="menu-item" onClick={() => galleryRef.current?.toggleSelectAll()}>
            {toolbarState.selectAll ? "Unselect All" : "Select All"}
          </button>
          <button
            className="menu-item"
            onClick={() => galleryRef.current?.deleteSelected()}
            disabled={toolbarState.selectedCount === 0}
            title="Delete selected images"
          >
            Delete
          </button>
          <button
            className="menu-item"
            onClick={() => galleryRef.current?.editSelectedTime()}
            disabled={toolbarState.selectedCount === 0}
            title="Set time taken for selected images"
          >
            Edit Time
          </button>
          <button className="menu-item" onClick={() => galleryRef.current?.openAddTag()}>
            Add Tag
          </button>
          <button className="menu-item menu-item-danger" onClick={handleLogout}>
            Logout
          </button>
        </nav>
      </div>
      <ImageGallery ref={galleryRef} onToolbarStateChange={setToolbarState} />

      <Footer />
    </div>
  );
}

export default App;
