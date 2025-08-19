import { useState, useRef } from "react";
import ImageGallery from "./ImageGallery";
import "./styles.css";

function App() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const galleryRef = useRef();

  const handleFileChange = (event) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setPreviewUrl(URL.createObjectURL(file));
    }
  };

  const handleUploadClick = () => {
    document.getElementById("fileInput").click();
  };

  const handleUploadToServer = async () => {
    if (!selectedFile) return;

    const formData = new FormData();
    formData.append("file", selectedFile);

    try {
      const response = await fetch("http://localhost:8080/api/images/upload", {
        method: "POST",
        body: formData,
      });

      if (!response.ok) throw new Error("Upload failed with status " + response.status);

      const result = await response.json();
      alert("✅ Upload successful: " + result.name);

      // refresh gallery
      galleryRef.current?.reload();

      // reset selection
      setSelectedFile(null);
      setPreviewUrl(null);
    } catch (error) {
      console.error("Upload failed", error);
      alert("❌ Upload failed: " + error.message);
    }
  };

  return (
    <div className="page">
      <h1 className="header">Welcome to PixServe 📸</h1>

      <div className="upload-area">
        <input
          id="fileInput"
          type="file"
          accept="image/*"
          style={{ display: "none" }}
          onChange={handleFileChange}
        />

        <button className="btn btn-primary" onClick={handleUploadClick}>
          Choose Image
        </button>

        {selectedFile && (
          <>
            <div>Selected: <strong>{selectedFile.name}</strong></div>
            {previewUrl && <img src={previewUrl} alt="Preview" className="preview" />}
            <button className="btn btn-success" onClick={handleUploadToServer}>
              Upload to Server
            </button>
          </>
        )}
      </div>

      <ImageGallery ref={galleryRef} />
    </div>
  );
}

export default App;
