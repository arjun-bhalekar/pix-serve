import ImageUpload from "./ImageUpload";
import ImageGallery from "./ImageGallery";
import { useRef } from "react";
import BulkImageUpload from "./BulkImageUpload";

function App() {
  const galleryRef = useRef();

  const handleUploadSuccess = () => {
    if (galleryRef.current) {
      galleryRef.current.reload();
    }
  };

  return (
    <div className="page">
      <h1 className="header">PixServe</h1>
       <div className="upload-bar">
        <ImageUpload onUpload={handleUploadSuccess} />
        <BulkImageUpload onUpload={handleUploadSuccess} />
      </div>
      <ImageGallery ref={galleryRef} />

      {/* Footer */}
      <footer className="footer">
        <p>PixServe Version : 1.2 © 2025 | Developed by - <a href="https://github.com/arjun-bhalekar" target="_blank">Arjun-Bhalekar</a></p>
      </footer>
    </div>
  );
}

export default App;
