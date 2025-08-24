import React, { useState } from "react";
import config from "./config";

export default function BulkImageUpload({ onUpload }) {
  const [files, setFiles] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  // Handle multiple file selection
  const handleFilesChange = (e) => {
    const selectedFiles = Array.from(e.target.files);
    setFiles(selectedFiles);

    // Generate previews
    const previewUrls = selectedFiles.map((file) => URL.createObjectURL(file));
    setPreviews(previewUrls);
  };

  // Bulk upload function
  const handleUpload = async () => {
    if (files.length === 0) return;

    setLoading(true);
    const formData = new FormData();
    files.forEach((file) => formData.append("files", file)); // matches backend

    try {
      const response = await fetch(`${config.apiBaseUrl}/images/upload/bulk`, {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        throw new Error("Bulk upload failed");
      }

      const data = await response.json();
      onUpload(data); // refresh gallery with new images
      setFiles([]);
      setPreviews([]);
      setIsOpen(false);
    } catch (error) {
      console.error("Bulk upload error:", error);
      alert("Bulk upload failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      {/* Upload button */}
      <div className="upload-bar">
        <button className="btn btn-success" onClick={() => setIsOpen(true)}>
          Bulk Upload
        </button>
      </div>

      {/* Modal */}
      {isOpen && (
        <div className="modal-overlay" onClick={() => setIsOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>Bulk Upload Images</h3>
            <input
              type="file"
              accept="image/*"
              multiple
              onChange={handleFilesChange}
            />

            {/* Previews */}
            {previews.length > 0 && (
              <div className="preview-container">
                {previews.map((src, idx) => (
                  <img key={idx} src={src} alt={`Preview ${idx}`} className="preview" />
                ))}
              </div>
            )}

            <div className="modal-buttons">
              <button
                className="btn btn-primary"
                onClick={handleUpload}
                disabled={loading}
              >
                {loading ? "Uploading..." : "Upload All"}
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
