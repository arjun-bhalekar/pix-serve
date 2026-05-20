import { useCallback, useEffect, useState, forwardRef, useImperativeHandle } from "react";
import "./styles.css";
import config from "./config";
import ImageUpload from "./ImageUpload";
import BulkImageUpload from "./BulkImageUpload";
import TagManager from "./TagManager";
import ImageModal from "./ImageModal";
import { authFetch, clearAuthToken } from "./auth";



const ImageGallery = forwardRef((props, ref) => {
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(true);
  //const [selectedImage, setSelectedImage] = useState(null); // modal image
  const [selectedImageIndex, setSelectedImageIndex] = useState(null);


  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [pageSize, setPageSize] = useState(28);
  const [selectedImages, setSelectedImages] = useState([]);
  const [selectAll, setSelectAll] = useState(false);

  const [filterYear, setFilterYear] = useState("");
  const [filterMonth, setFilterMonth] = useState(""); 
  const [filterDay, setFilterDay] = useState("");
  const [filterTag, setFilterTag] = useState("");

  const [tags, setTags] = useState([]);

  const handleUnauthorized = useCallback(() => {
    clearAuthToken();
    window.location.reload();
  }, []);

  // Load images with current page + filters
  const loadImages = useCallback((pageNum = 0) => {
    setLoading(true);

    let url = `${config.apiBaseUrl}/images/list?page=${pageNum}&size=${pageSize}`;
    if (filterYear) url += `&year=${filterYear}`;
    if (filterMonth) url += `&month=${filterMonth}`;
    if (filterDay) url += `&day=${filterDay}`;
    if(filterTag) url += `&tagName=${filterTag}`;

    authFetch(url)
      .then((res) => {
        if (res.status === 401 || res.status === 403) {
          handleUnauthorized();
          return null;
        }
        return res.json();
      })
      .then((data) => {
        if (!data) return;
        setImages(Array.isArray(data?.content) ? data.content : []);
        setTotalPages(data?.totalPages || 1);
        setPage(pageNum);
        setTotalElements(data?.totalElements || 0);
        setLoading(false);
      })
      .catch((err) => {
        console.error("Failed to fetch images", err);
        setLoading(false);
      });
  }, [filterDay, filterMonth, filterTag, filterYear, handleUnauthorized, pageSize]);

  useImperativeHandle(ref, () => ({
    reload: () => loadImages(page),
  }), [loadImages, page]);

const handleViewImage = (id) => {
   //setSelectedImage(`${config.apiBaseUrl}/images/${id}/view`);
   const index = images.findIndex((img) => img.id === id);
  setSelectedImageIndex(index);
 };

const closeModal = () => {
  setSelectedImageIndex(null);
};

// initial tag load
useEffect(() => {
  authFetch(`${config.apiBaseUrl}/tags`)
      .then((res) => {
        if (res.status === 401 || res.status === 403) {
          handleUnauthorized();
          return null;
        }
        return res.json();
      })
      .then((data) => {
        if (data) setTags(data);
      })
      .catch((err) => console.error("Failed to fetch tags", err));
}, [handleUnauthorized]);

// reload when filters or pageSize change
useEffect(() => {
  loadImages(0);
}, [loadImages]);

  const handlePrev = () => {
    if (page > 0) loadImages(page - 1);
  };

  const handleNext = () => {
    if (page < totalPages - 1) loadImages(page + 1);
  };

  const handleBulkDelete = async (ids) => {
    const confirmDelete = window.confirm("Are you sure you want to perform bulk delete ?");
    if (!confirmDelete) return;
    try {
      const response = await authFetch(`${config.apiBaseUrl}/images/bulk-delete`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(ids),
      });
      if (response.status === 401 || response.status === 403) {
        handleUnauthorized();
        return;
      }
      if (response.status === 204) {
        // Refresh current page after deletion
        alert("Deleted successfully");
        loadImages(page);
        // Refresh gallery after delete
        //setImages(images.filter((img) => !ids.includes(img.id)));
        setSelectedImages([]);
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


  //bulk edit call 
  const handleBulkEdit = async (ids, newTakenTime) => {
  if (!newTakenTime) {
    alert("Please select a valid time.");
    return;
  }

  const confirmEdit = window.confirm("Are you sure you want to update time taken for selected images?");
  if (!confirmEdit) return;

  try {
    const response = await authFetch(`${config.apiBaseUrl}/images/bulk-edit-time`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        imageIds: ids,
        takenTime: newTakenTime, // epoch ms or ISO string, depending on backend
      }),
    });
    if (response.status === 401 || response.status === 403) {
      handleUnauthorized();
      return;
    }

    if (response.status === 204) {
      alert("Updated successfully");
      loadImages(page); // refresh current page
      setSelectedImages([]);
    } else if (response.status === 404) {
      alert("One or more images not found!");
    } else {
      alert("Failed to update images.");
    }
  } catch (error) {
    console.error("Error updating images:", error);
    alert("Error updating images.");
  }
};

//bulk edit call 
  const handleBulkTagEdit  = async (ids, tagName) => {
  if (tagName=='') {
    alert("Please select Tag Name");
    return;
  }

  const confirmEdit = window.confirm("Are you sure you want to update Tag for selected images?");
  if (!confirmEdit) return;

  try {
    const response = await authFetch(`${config.apiBaseUrl}/images/bulk-edit-tag`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        imageIds: ids,
        tagName: tagName, // epoch ms or ISO string, depending on backend
      }),
    });
    if (response.status === 401 || response.status === 403) {
      handleUnauthorized();
      return;
    }

    if (response.status === 204) {
      alert("Updated successfully");
      loadImages(page); // refresh current page
      setSelectedImages([]);
    } else if (response.status === 404) {
      alert("One or more images not found!");
    } else {
      alert("Failed to update images.");
    }
  } catch (error) {
    console.error("Error updating images:", error);
    alert("Error updating images.");
  }
};

const handleSelectAll = () => {
  if (selectAll) {
    // unselect all
    setSelectedImages([]);
  } else {
    // select all ids from current page
    const allIds = images.map((img) => img.id);
    setSelectedImages(allIds);
  }
  setSelectAll(!selectAll);
};



  const handleUploadSuccess = () => {
    window.location.reload();
  };

  const handleTagAdded = () => {
    window.location.reload();
  };

  return (
    <div className="gallery-wrap">
      {/* <h2 className="gallery-title">Gallery</h2> */}

      {/* Filter Bar */}
      <div className="filters">
        Filter By :
        <input
          style={{width: "45px"}}
          type="number"
          placeholder="Yr"
          value={filterYear}
          onChange={(e) => setFilterYear(e.target.value)}
          className="filter-input"
        />
        <input
          style={{width: "40px"}}
          type="number"
          placeholder="Mnt"
          value={filterMonth}
          onChange={(e) => setFilterMonth(e.target.value)}
          className="filter-input"
          min="1"
          max="12"
        />
        <input
          style={{width: "38px"}}
          type="number"
          placeholder="Day"
          value={filterDay}
          onChange={(e) => setFilterDay(e.target.value)}
          className="filter-input"
          min="1"
          max="31"
        />
        |
        {/* ✅ Tag select dropdown */}
        <select title="Filter Based on Tag Name"
          value={filterTag}
          onChange={(e) => setFilterTag(e.target.value)}
          className="filter-input"
        >
          <option value="">All Tags</option>
          {tags.map((tag) => (
            <option key={tag.id} value={tag.name}>
              {tag.name}
            </option>
          ))}
        </select>

        <button
          className="btn btn-secondary"
          onClick={() => {
            setFilterYear("");
            setFilterMonth("");
            setFilterDay("");
            setFilterTag("");
          }}
        >
          Clear Filters
        </button>

        <select value={pageSize} onChange={(e) => setPageSize(Number(e.target.value))}>
          <option value={14}>14</option>
          <option value={28} >28</option>
          <option value={56}>56</option>
          <option value={112}>112</option>
          <option value={224}>224</option>
        </select>
            
            <button className="btn btn-primary" onClick={handlePrev} disabled={page === 0}>
              Prev
            </button>
            
            <button className="btn btn-primary" onClick={handleNext} disabled={page + 1 === totalPages}>
              Next
            </button>
            <button className="btn btn-success" onClick={handleSelectAll}>
              {selectAll ? "Unselect All" : "Select All"}
            </button>
            <button title="Delete Selected Images" className="btn btn-primary" onClick={() => handleBulkDelete(selectedImages)} >
              Delete
            </button>

            <button title="Set Time taken for selected images" className="btn btn-primary" onClick={() => {
              const newTime = prompt("Enter new taken time (YYYY-MM-DD HH:mm):");
              if (newTime) {
                const epochMillis = new Date(newTime).getTime();
                handleBulkEdit(selectedImages, epochMillis);
              }
            }} >
              Edit Time
            </button>

            <select
              disabled={selectedImages.length === 0}
              onChange={(e) => {
                if (e.target.value) {
                  handleBulkTagEdit(selectedImages, e.target.value);
                }
              }}
            >
              <option value="">-- Edit Tag --</option>
              {tags.map((tag) => (
                <option key={tag.id} value={tag.name}>{tag.name}</option>
              ))}
            </select>

            <ImageUpload onUpload={handleUploadSuccess} />
            <BulkImageUpload onUpload={handleUploadSuccess} />
            <TagManager onTagAdded={handleTagAdded} /> 

            <span style={{color: "red"}}>{selectedImages.length} selected</span>
            <span>
              | Page {page + 1} of {totalPages} | Total Images: <b> {totalElements.toLocaleString()} </b>
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
                  {/* <button className="delete-btn" onClick={() => handleDelete(img.id)}>🗑️</button> */}
                  
                </div>
                  
                <div className="name">
                  <input
                    type="checkbox"
                    checked={selectedImages.includes(img.id)}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedImages([...selectedImages, img.id]);
                      } else {
                        setSelectedImages(selectedImages.filter((id) => id !== img.id));
                      }
                    }}                   
                  />
                <span style={{ marginLeft: "3px", fontFamily: "monospace", fontSize: "1.2em" }}>
                  {img.takenInfo.day}{"-"}
                  {new Date(0, img.takenInfo.month - 1).toLocaleString("default", { month: "short" })}{"-"}
                  {String(img.takenInfo.year).slice(-2)}
                </span>
                  {img.tags && img.tags.length > 0 && (
                    <span 
                      title={img.tags.join(", ")} 
                      style={{marginLeft: "8px", color: "#999", fontSize: "1.2em", cursor: "pointer"}}
                    > 🏷️
                    </span>
                  )}
                  
                </div>
              </div>
            ))}
          </div>
          
        </>
      )}

      {/* Modal for full image */}
      {/* {selectedImage && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <button className="nav-btn prev-btn" onClick={showPrevImage}>⬅️</button>
            <img src={selectedImage} alt="Full" className="full-image" />
            <button className="nav-btn next-btn" onClick={showNextImage}>➡️</button>
            <div className="modal-actions">
              <button className="play-btn" onClick={playImages}>▶️</button>
              <button className="close-btn" onClick={closeModal}>✖</button>
            </div>

          </div>
        </div>
      )} */}
      {selectedImageIndex !== null && (
        <ImageModal
          images={images}
          selectedIndex={selectedImageIndex}
          onClose={closeModal}
        />
      )}
      
    </div>
  );
});

export default ImageGallery;
