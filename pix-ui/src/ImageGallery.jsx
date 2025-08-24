import { useEffect, useState, forwardRef, useImperativeHandle } from "react";
import "./styles.css";
import config from "./config";

const ImageGallery = forwardRef((props, ref) => {
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedImage, setSelectedImage] = useState(null); // modal image
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  //const pageSize = 24; // number of images per page
  // page size
  const [pageSize, setPageSize] = useState(24);


  const [filterYear, setFilterYear] = useState("");
  const [filterMonth, setFilterMonth] = useState(""); 
  const [filterDay, setFilterDay] = useState("");

  // Load images with current page + filters
  const loadImages = (pageNum = 0) => {
    setLoading(true);

    let url = `${config.apiBaseUrl}/images/list?page=${pageNum}&size=${pageSize}`;
    if (filterYear) url += `&year=${filterYear}`;
    if (filterMonth) url += `&month=${filterMonth}`;
    if (filterDay) url += `&day=${filterDay}`;

    fetch(url)
      .then((res) => res.json())
      .then((data) => {
        setImages(Array.isArray(data?.content) ? data.content : []);
        setTotalPages(data?.totalPages || 1);
        setPage(pageNum);
        setLoading(false);
      })
      .catch((err) => {
        console.error("Failed to fetch images", err);
        setLoading(false);
      });
  };

  const handleDelete = async (id) => {
    const confirmDelete = window.confirm("Are you sure you want to delete this image?");
    if (!confirmDelete) return;

    try {
      const response = await fetch(`${config.apiBaseUrl}/images/${id}`, { method: "DELETE" });
      if (response.status === 204) {
        // Refresh current page after deletion
        loadImages(page);
      } else if (response.status === 404) {
        alert("Image not found!");
      } else {
        alert("Failed to delete image.");
      }
    } catch (error) {
      console.error("Error deleting image:", error);
      alert("Error deleting image.");
    }
  };

  useImperativeHandle(ref, () => ({
    reload: () => loadImages(page),
  }));

const handleViewImage = (id) => {
  setSelectedImage(`${config.apiBaseUrl}/images/${id}/view`);
};

  // initial load
useEffect(() => {
  loadImages(0, pageSize);
}, []);

// reload when filters or pageSize change
useEffect(() => {
  loadImages(0, pageSize);
}, [pageSize, filterYear, filterMonth, filterDay]);

  const closeModal = () => {
    setSelectedImage(null);
  };

  const handlePrev = () => {
    if (page > 0) loadImages(page - 1);
  };

  const handleNext = () => {
    if (page < totalPages - 1) loadImages(page + 1);
  };

  return (
    <div className="gallery-wrap">
      {/* <h2 className="gallery-title">Gallery</h2> */}

      {/* Filter Bar */}
      <div className="filters">
        Filter By :
        <input
          type="number"
          placeholder="Year"
          value={filterYear}
          onChange={(e) => setFilterYear(e.target.value)}
          className="filter-input"
        />
        <input
          type="number"
          placeholder="Month"
          value={filterMonth}
          onChange={(e) => setFilterMonth(e.target.value)}
          className="filter-input"
          min="1"
          max="12"
        />
        <input
          type="number"
          placeholder="Day"
          value={filterDay}
          onChange={(e) => setFilterDay(e.target.value)}
          className="filter-input"
          min="1"
          max="31"
        />
        <button
          className="btn btn-secondary"
          onClick={() => {
            setFilterYear("");
            setFilterMonth("");
            setFilterDay("");
          }}
        >
          Clear Filters
        </button>

        <select value={pageSize} onChange={(e) => setPageSize(Number(e.target.value))}>
          <option value={12}>12</option>
          <option value={24}>24</option>
          <option value={48}>48</option>
          <option value={96}>96</option>
        </select>
            
            <button className="btn btn-primary" onClick={handlePrev} disabled={page === 0}>
              Prev
            </button>
            
            <button className="btn btn-primary" onClick={handleNext} disabled={page + 1 === totalPages}>
              Next
            </button>
            
            <span>
              Page {page + 1} of {totalPages}
            </span>
          
      
      </div>

      {loading ? (
        <p className="loading">Loading images...</p>
      ) : images.length === 0 ? (
        <p className="empty">No images uploaded yet.</p>
      ) : (
        <>
          <div className="gallery-grid">
            {images.map((img) => (
              <div key={img.id} className="card">
                <div className="image-container">
                  <img
                    className="thumb"
                    src={img.thumbnail}
                    alt={img.name}
                    onClick={() => handleViewImage(img.id)}
                    style={{ cursor: "pointer" }}
                  />
                  <button className="delete-btn" onClick={() => handleDelete(img.id)}>
                    🗑️
                  </button>
                </div>
                <div className="name">
                  {img.takenInfo?.dateTime
                    ? new Date(Number(img.takenInfo.dateTime)).toLocaleDateString("en-GB", {
                        year: "numeric",
                        month: "short",
                        day: "2-digit",
                      })
                    : img.createdOn
                    ? new Date(Number(img.createdOn)).toLocaleDateString("en-GB", {
                        year: "numeric",
                        month: "short",
                        day: "2-digit",
                      })
                    : "Unknown"}
                </div>
              </div>
            ))}
          </div>

          {/* Pagination Controls */}
          {/* <br />
          <div className="pagination">
            <button className="btn btn-primary" onClick={handlePrev} disabled={page === 0}>
              Prev
            </button>
            <span> </span>
            <button className="btn btn-primary" onClick={handleNext} disabled={page + 1 === totalPages}>
              Next
            </button>
            <span> </span>
            <span>
              Page {page + 1} of {totalPages}
            </span>
          </div> */}
          
        </>
      )}

      {/* Modal for full image */}
      {selectedImage && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <img src={selectedImage} alt="Full" className="full-image" />
            <button className="close-btn" onClick={closeModal}>
              ✖
            </button>
          </div>
        </div>
      )}
    </div>
  );
});

export default ImageGallery;
