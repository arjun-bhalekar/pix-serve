import { useCallback, useState, useEffect, useRef } from "react";
import config from "./config";
import { authFetch } from "./auth";

export default function ImageModal({ images, selectedIndex, onClose }) {
  const [currentIndex, setCurrentIndex] = useState(selectedIndex);
  const [isPlaying, setIsPlaying] = useState(false);
  const [imageUrl, setImageUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const intervalRef = useRef(null);

  useEffect(() => {
    setCurrentIndex(selectedIndex);
  }, [selectedIndex]);

  const showPrevImage = useCallback(() => {
    setCurrentIndex((prev) =>
      prev === 0 ? images.length - 1 : prev - 1
    );
  }, [images.length]);

  const showNextImage = useCallback(() => {
    setCurrentIndex((prev) =>
      (prev + 1) % images.length
    );
  }, [images.length]);

  // ✅ Slideshow effect
  useEffect(() => {
    if (isPlaying) {
      intervalRef.current = setInterval(() => {
        setCurrentIndex((prev) => (prev + 1) % images.length);
      }, 3000); // change every 3s
    } else {
      clearInterval(intervalRef.current);
    }

    return () => clearInterval(intervalRef.current);
  }, [isPlaying, images.length]);

  const togglePlay = () => {
    setIsPlaying((prev) => !prev);
  };

  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === "ArrowLeft") {
        showPrevImage();
      }

      if (event.key === "ArrowRight") {
        showNextImage();
      }

      if (event.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose, showNextImage, showPrevImage]);

  useEffect(() => {
    let objectUrl = "";
    let cancelled = false;

    const loadImage = async () => {
      setLoading(true);
      try {
        const response = await authFetch(`${config.apiBaseUrl}/images/${images[currentIndex].id}/view`);
        if (!response.ok) {
          throw new Error("Failed to load image");
        }
        const blob = await response.blob();
        objectUrl = URL.createObjectURL(blob);
        if (!cancelled) {
          setImageUrl(objectUrl);
        }
      } catch (error) {
        console.error("Image load error:", error);
        if (!cancelled) {
          setImageUrl("");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadImage();

    return () => {
      cancelled = true;
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [currentIndex, images]);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        
        <button className="nav-btn prev-btn" onClick={showPrevImage}>⬅️</button>

        {loading && <p className="modal-loading">Loading image...</p>}
        {!loading && imageUrl && (
          <img
            src={imageUrl}
            alt="Full"
            className="full-image"
          />
        )}
        {!loading && !imageUrl && <p className="modal-loading">Unable to load image.</p>}

        <button className="nav-btn next-btn" onClick={showNextImage}>➡️</button>

        <div className="modal-actions">
          <button className="play-btn" onClick={togglePlay}>
            {isPlaying ? "⏸" : "▶️"}
          </button>
          <button className="close-btn" onClick={onClose}>✖</button>
        </div>
      </div>
    </div>
  );
}
