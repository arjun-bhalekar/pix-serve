import { useState } from "react";
import config from "./config";
import { authFetch } from "./auth";

function TagManager({ onTagAdded }) {
  const [newTag, setNewTag] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleAddTag = async () => {
    if (!newTag.trim()) {
      alert("Please enter a tag name");
      return;
    }
    setLoading(true);
    try {
      const response = await authFetch(`${config.apiBaseUrl}/tags`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: newTag.trim() }),
      });

      if (response.ok) {
        alert("Tag added successfully");
        if (onTagAdded) onTagAdded(newTag.trim()); // notify parent
        setNewTag("");
        setIsOpen(false); // close modal
      } else {
        alert("Failed to add tag");
      }
    } catch (error) {
      console.error("Error adding tag:", error);
      alert("Error adding tag");
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      {/* Open Modal Button */}
      <div className="upload-bar">
        <button className="btn btn-info" onClick={() => setIsOpen(true)}>
          Add Tag
        </button>
      </div>

      {/* Modal */}
      {isOpen && (
        <div className="modal-overlay" onClick={() => setIsOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>Add New Tag</h3>
            
            <input
              type="text"
              placeholder="New Tag"
              value={newTag}
              onChange={(e) => setNewTag(e.target.value)}
              className="filter-input"
            />

            <div className="modal-buttons">
              <button
                className="btn btn-primary"
                onClick={handleAddTag}
                disabled={loading}
              >
                {loading ? "Adding..." : "Add Tag"}
              </button>
              <button
                className="btn btn-secondary"
                onClick={() => setIsOpen(false)}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

export default TagManager;
