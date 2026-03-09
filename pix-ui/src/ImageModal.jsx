import { useState, useEffect, useRef } from "react";
import config from "./config";

export default function ImageModal({ images, selectedIndex, onClose }) {
  const [currentIndex, setCurrentIndex] = useState(selectedIndex);
  const [isPlaying, setIsPlaying] = useState(false);
  const intervalRef = useRef(null);

  useEffect(() => {
    setCurrentIndex(selectedIndex);
  }, [selectedIndex]);

  const showPrevImage = () => {
    setCurrentIndex((prev) =>
      prev === 0 ? images.length - 1 : prev - 1
    );
  };

  const showNextImage = () => {
    setCurrentIndex((prev) =>
      (prev + 1) % images.length
    );
  };

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

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        
        <button className="nav-btn prev-btn" onClick={showPrevImage}>⬅️</button>

        <img
          src={`${config.apiBaseUrl}/images/${images[currentIndex].id}/view`}
          alt="Full"
          className="full-image"
        />

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
