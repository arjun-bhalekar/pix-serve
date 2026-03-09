import ImageUpload from "./ImageUpload";
import ImageGallery from "./ImageGallery";
import { useRef } from "react";
import BulkImageUpload from "./BulkImageUpload";
import TagManager from "./TagManager";

function App() {
  const galleryRef = useRef();

  const handleUploadSuccess = () => {
    if (galleryRef.current) {
      galleryRef.current.reload();
    }
  };

  const handleTagAdded = () => {
    if (galleryRef.current) {
      galleryRef.current.reload(); // optional: reload gallery/filter if tags used
    }
  };

  return (
    <div className="page">
      <h1 className="header">PixServe</h1>
       <div className="upload-bar">
        {/* <ImageUpload onUpload={handleUploadSuccess} /> */}
        {/* <BulkImageUpload onUpload={handleUploadSuccess} /> */}
        {/* <TagManager onTagAdded={handleTagAdded} />  */}
      </div>
      <ImageGallery ref={galleryRef} />

      {/* Footer */}
      <footer className="footer">
        <p>PixServe Version : 1.5 © 2025 | Developed by - <a href="https://github.com/arjun-bhalekar" target="_blank">Arjun-Bhalekar</a></p>
      </footer>
    </div>
  );
}

export default App;
