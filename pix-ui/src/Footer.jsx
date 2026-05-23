const APP_VERSION = "1.6.4";
const COPYRIGHT_YEAR = "2025";
const DEVELOPER_NAME = "Arjun-Bhalekar";
const DEVELOPER_URL = "https://github.com/arjun-bhalekar";

export default function Footer() {
  return (
    <footer className="footer">
      <p>
        PixServe | Version : {APP_VERSION} © {COPYRIGHT_YEAR} | Developed by -{" "}
        <a href={DEVELOPER_URL} target="_blank" rel="noreferrer">
          {DEVELOPER_NAME}
        </a>
      </p>
    </footer>
  );
}
