import React, { forwardRef, useEffect, useImperativeHandle, useState } from "react";
import config from "./config";
import { authFetch } from "./auth";


const MediaUpload = forwardRef(({ onUpload, hideTrigger = false }, ref) => {
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
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
      const tagQuery = selectedTag ? `?tagName=${encodeURIComponent(selectedTag)}` : "";
      const response = await authFetch(`${config.apiBaseUrl}/media/upload${tagQuery}`, {
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
      setSelectedTag("");
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
      {!hideTrigger && (
        <div className="upload-bar">
          <button className="btn btn-primary" onClick={() => setIsOpen(true)}>
            Upload
          </button>
        </div>
      )}

      {/* Modal */}
      {isOpen && (
        <div className="modal-overlay" onClick={() => setIsOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>Upload Media</h3>
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
            <input type="file" accept="image/*, video/*" onChange={handleFileChange} />

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
});

export default MediaUpload;
