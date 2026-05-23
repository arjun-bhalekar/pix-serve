import { useCallback, useState, useEffect, useRef } from "react";
import config from "./config";
import { authFetch } from "./auth";

export default function ImageModal({ images, selectedIndex, onClose }) {
  const [currentIndex, setCurrentIndex] = useState(selectedIndex);
  const [isPlaying, setIsPlaying] = useState(false);
  const [showInfo, setShowInfo] = useState(false);
  const [imageInfo, setImageInfo] = useState(null);
  const [infoLoading, setInfoLoading] = useState(false);
  const [infoError, setInfoError] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const intervalRef = useRef(null);
  const touchStartRef = useRef(null);
  const infoCacheRef = useRef({});

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



  const handleInfoToggle = () => {
    setShowInfo((prev) => !prev);
  };

  const handleTouchStart = (event) => {
    const touch = event.changedTouches[0];
    touchStartRef.current = {
      x: touch.clientX,
      y: touch.clientY,
    };
  };

  const handleTouchEnd = (event) => {
    if (!touchStartRef.current) return;

    const touch = event.changedTouches[0];
    const deltaX = touch.clientX - touchStartRef.current.x;
    const deltaY = touch.clientY - touchStartRef.current.y;
    touchStartRef.current = null;

    if (Math.abs(deltaX) < 50 || Math.abs(deltaX) < Math.abs(deltaY)) {
      return;
    }

    if (deltaX < 0) {
      showNextImage();
    } else {
      showPrevImage();
    }
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

  useEffect(() => {
    if (!showInfo) return;

    const imageId = images[currentIndex]?.id;

    if (!imageId) return;

    // ✅ Use cache if available
    if (infoCacheRef.current[imageId]) {
      setImageInfo(infoCacheRef.current[imageId]);
      setInfoError("");
      return;
    }

    let cancelled = false;

    const fetchImageInfo = async () => {
      setInfoLoading(true);
      setInfoError("");

      try {
        const response = await authFetch(
            `${config.apiBaseUrl}/images/${imageId}/metadata`
        );

        if (!response.ok) {
          throw new Error("Failed to load image info");
        }

        const data = await response.json();

        infoCacheRef.current[imageId] = data;

        if (!cancelled) {
          setImageInfo(data);
        }
      } catch (error) {
        console.error("Image info load error:", error);

        if (!cancelled) {
          setImageInfo(null);
          setInfoError("Unable to load image info.");
        }
      } finally {
        if (!cancelled) {
          setInfoLoading(false);
        }
      }
    };

    fetchImageInfo();

    return () => {
      cancelled = true;
    };
  }, [currentIndex, showInfo, images]);

  return (
    <div className="modal-overlay image-modal-overlay" onClick={onClose}>
      <div
        className="modal-content"
        onClick={(e) => e.stopPropagation()}
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
      >
        
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

        {showInfo && (
          <div className="image-info-panel">
            <div className="image-info-title">Image Info</div>
            {infoLoading && <div>Loading info...</div>}
            {!infoLoading && infoError && <div>{infoError}</div>}
            {!infoLoading && !infoError && imageInfo && (
              <>
                <div>Name: <b>{imageInfo.name || "N/A"} </b></div>
                <div>
                  Date: <b> {" "}
                  {imageInfo.takenInfo
                    ? `${imageInfo.takenInfo.day}-${imageInfo.takenInfo.month}-${imageInfo.takenInfo.year}`
                    : "N/A"} </b>
                </div>
                <dib>Camera : <b> {imageInfo.camera || "NA"} </b> </dib>
                <div>Tags: <b> {imageInfo.tags?.length ? imageInfo.tags.join(", ") : "None"} </b> </div>
                <div>Location : <b>{imageInfo.location}</b> </div>
              </>
            )}
          </div>
        )}

        <button className="nav-btn next-btn" onClick={showNextImage}>➡️</button>

        <div className="modal-actions">
          <button className="info-btn" onClick={handleInfoToggle}>
            i
          </button>
          <button className="play-btn" onClick={togglePlay}>
            {isPlaying ? "⏸" : "▶️"}
          </button>
          <button className="close-btn" onClick={onClose}>✖</button>
        </div>
      </div>
    </div>
  );
}
