import { useEffect, useState, forwardRef, useImperativeHandle } from "react";
import "./styles.css";

const ImageGallery = forwardRef((props, ref) => {
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadImages = () => {
    setLoading(true);
    fetch("http://localhost:8080/api/images/list?page=0&size=30")
      .then((res) => res.json())
      .then((data) => {
        setImages(Array.isArray(data?.content) ? data.content : []);
        setLoading(false);
      })
      .catch((err) => {
        console.error("Failed to fetch images", err);
        setLoading(false);
      });
  };

  useImperativeHandle(ref, () => ({
    reload: loadImages,
  }));

  useEffect(() => {
    loadImages();
  }, []);

  return (
    <div className="gallery-wrap">
      <h2 className="gallery-title">Uploaded Images</h2>

      {loading ? (
        <p className="loading">Loading images...</p>
      ) : images.length === 0 ? (
        <p className="empty">No images uploaded yet.</p>
      ) : (
        <div className="gallery-grid">
          {images.map((img) => (
            <div key={img.id} className="card">
              {/* `thumbnail` is already a data URL: "data:image/jpeg;base64,..." */}
              <img className="thumb" src={img.thumbnail} alt={img.name} />
              <div className="name">{img.name}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
});

export default ImageGallery;
