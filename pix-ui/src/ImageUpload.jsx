import React, { useState } from "react";
import config from "./config";


export default function ImageUpload({ onUpload }) {
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleFileChange = (e) => {
    const selected = e.target.files[0];
    setFile(selected);
    if (selected) {
      setPreview(URL.createObjectURL(selected));
    } else {
      setPreview(null);
    }
  };

  const handleUpload = async () => {
    if (!file) return;

    setLoading(true);
    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await fetch(`${config.apiBaseUrl}/images/upload`, {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        throw new Error("Upload failed");
      }

      const data = await response.json();
      onUpload(data); // refresh gallery
      setFile(null);
      setPreview(null);
      setIsOpen(false);
    } catch (error) {
      console.error("Upload error:", error);
      alert("Upload failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      {/* Upload button on main screen */}
      <div className="upload-bar">
        <button className="btn btn-success" onClick={() => setIsOpen(true)}>
          Upload
        </button>
      </div>

      {/* Modal */}
      {isOpen && (
        <div className="modal-overlay" onClick={() => setIsOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>Upload Image</h3>
            <input type="file" accept="image/*" onChange={handleFileChange} />
            
            {preview && <img src={preview} alt="Preview" className="preview" />}

            <div className="modal-buttons">
              <button
                className="btn btn-primary"
                onClick={handleUpload}
                disabled={loading}
              >
                {loading ? "Uploading..." : "Upload"}
              </button>
              <button className="btn btn-secondary" onClick={() => setIsOpen(false)}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
