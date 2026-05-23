import React, { forwardRef, useEffect, useImperativeHandle, useState } from "react";
import config from "./config";
import { authFetch } from "./auth";

const BulkImageUpload = forwardRef(({ onUpload, hideTrigger = false }, ref) => {
  const [files, setFiles] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [tags, setTags] = useState([]);
  const [selectedTag, setSelectedTag] = useState("");

  useImperativeHandle(ref, () => ({
    open: () => setIsOpen(true),
  }));

  useEffect(() => {
    if (!isOpen) return;

    authFetch(`${config.apiBaseUrl}/tags`)
      .then((res) => res.json())
      .then((data) => {
        if (Array.isArray(data)) {
          setTags(data);
        }
      })
      .catch((error) => console.error("Failed to fetch tags", error));
  }, [isOpen]);

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
      const tagQuery = selectedTag ? `?tagName=${encodeURIComponent(selectedTag)}` : "";
      const response = await authFetch(`${config.apiBaseUrl}/images/upload/bulk${tagQuery}`, {
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
      setSelectedTag("");
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
      {!hideTrigger && (
        <div className="upload-bar">
          <button className="btn btn-success" onClick={() => setIsOpen(true)}>
            Bulk Upload
          </button>
        </div>
      )}

      {/* Modal */}
      {isOpen && (
        <div className="modal-overlay" onClick={() => setIsOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>Bulk Upload Images</h3>
            <select
              className="upload-tag-select"
              value={selectedTag}
              onChange={(e) => setSelectedTag(e.target.value)}
            >
              <option value="">No Tag</option>
              {tags.map((tag) => (
                <option key={tag.id} value={tag.name}>
                  {tag.name}
                </option>
              ))}
            </select>
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
});

export default BulkImageUpload;
