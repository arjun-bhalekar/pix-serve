# Pix Service

Spring Boot backend API for storing, organizing, and serving image metadata for the Pix Serve application.

The service accepts image uploads, extracts image metadata, generates thumbnails, stores metadata in MongoDB, supports tags and bulk edits, and protects API routes with JWT bearer authentication.

## Tech Stack

- Java 21
- Spring Boot 3.5.14
- Spring Web
- Spring Data MongoDB
- Spring Security
- JWT
- Thumbnailator
- metadata-extractor
- Maven Wrapper

## Project Structure

```text
src/main/java/com/pixserve
├── controller      REST controllers for auth, images, and tags
├── dto             Request/response DTOs
├── model           MongoDB document models
├── repository      MongoDB repositories
├── runner          Startup bulk upload runner
├── security        JWT filter, token utility, and Spring Security config
├── service         Image metadata, storage, and bulk upload services
└── util            Image hashing and metadata extraction utilities
```

Static frontend build assets are currently stored under:

```text
src/main/resources/static
```

## Requirements

- Java 21
- MongoDB running locally or a reachable MongoDB URI
- A writable storage directory for original images and thumbnails

## Configuration

Configuration is defined in:

```text
src/main/resources/application.properties
```

Current important properties:

```properties
server.port=9091

spring.servlet.multipart.max-file-size=25MB
spring.servlet.multipart.max-request-size=50MB

base.dir.path=/Users/arjunbhalekar/pix-serve-storage

spring.data.mongodb.uri=mongodb://localhost:27017/pixserve-prod
spring.data.mongodb.database=pixserve-prod

bulk.upload.enabled=false
bulk.upload.src.dir.path=D:\\temp
bulk.upload.batch.size=20
bulk.upload.thread.pool.size=8
```

Before running locally, make sure `base.dir.path` exists or can be created by the application. Uploaded images are stored under:

```text
<base.dir.path>/images/<year>/<month>
<base.dir.path>/thumbs/<year>/<month>
```

## Run Locally

Start MongoDB, then run:

```bash
./mvnw spring-boot:run
```

The backend starts on:

```text
http://localhost:9091
```

## Build and Test

Run tests:

```bash
./mvnw test
```

Build the jar:

```bash
./mvnw clean package
```

Run the packaged jar:

```bash
java -jar target/pix-service-1.6.jar
```

## Docker

Build the jar first because the Dockerfile copies the packaged jar from `target`:

```bash
./mvnw clean package
```

Build the Docker image:

```bash
docker build -t pix-service:latest .
```

Run the image with the `prod` profile configuration:

```bash
docker run -d \
  --name pix-service \
  -p 8082:8082 \
  -e PIXSERVE_AUTH_USERNAME=admin \
  -e PIXSERVE_AUTH_PASSWORD='Test@123' \
  -e BASE_DIR_PATH=/data/pixserve \
  -e SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/pixserve-prod \
  -e SPRING_DATA_MONGODB_DATABASE=pixserve-prod \
  -e BULK_UPLOAD_ENABLED=false \
  -e BULK_UPLOAD_DIR_PATH=/data/pixserve/bulk-upload \
  -v /Users/arjunbhalekar/pix-serve-prod/storage:/data/pixserve \
  pix-service:latest
```

## Authentication

The project currently implements stateless JWT bearer-token authentication.

Login endpoint:

```http
POST /auth/login
Content-Type: application/json
```

Request body:

```json
{
  "username": "admin",
  "password": "Test@123"
}
```

Successful response:

```json
{
  "token": "<jwt-token>"
}
```

Use the token on protected API calls:

```http
Authorization: Bearer <jwt-token>
```

Current authentication limitations:

- Credentials are hardcoded in `AuthController`.
- JWT signing secret is hardcoded in `JwtUtil`.
- There are no database-backed users.
- There are no roles or permissions.
- There are no refresh tokens.

## API Endpoints

### Auth

| Method | Path | Description | Auth |
| --- | --- | --- | --- |
| POST | `/auth/login` | Login and receive JWT token | No |

### Images

| Method | Path | Description | Auth |
| --- | --- | --- | --- |
| GET | `/api/images/list` | Paginated image list with thumbnails | Yes |
| POST | `/api/images/upload` | Upload one image | Yes |
| POST | `/api/images/upload/bulk` | Upload multiple images | Yes |
| GET | `/api/images/{id}/view` | Return original image bytes | Yes |
| DELETE | `/api/images/{id}` | Delete image metadata and files | Yes |
| POST | `/api/images/bulk-delete` | Delete multiple images | Yes |
| POST | `/api/images/bulk-edit-time` | Update taken date for multiple images | Yes |
| POST | `/api/images/bulk-edit-tag` | Add tag to multiple images | Yes |
| POST | `/api/images/compute-hashes` | Compute missing SHA-256 hashes | Yes |

Image list query parameters:

```text
page      default: 0
size      default: 10
year      optional
month     optional
day       optional
tagName   optional
```

Example:

```http
GET /api/images/list?page=0&size=20&year=2026&month=5
Authorization: Bearer <jwt-token>
```

Single image upload:

```http
POST /api/images/upload
Authorization: Bearer <jwt-token>
Content-Type: multipart/form-data

file=<image-file>
```

Bulk image upload:

```http
POST /api/images/upload/bulk
Authorization: Bearer <jwt-token>
Content-Type: multipart/form-data

files=<image-file-1>
files=<image-file-2>
```

Bulk delete body:

```json
[
  "image-id-1",
  "image-id-2"
]
```

Bulk edit time body:

```json
{
  "imageIds": ["image-id-1", "image-id-2"],
  "takenTime": 1779273000000
}
```

Bulk edit tag body:

```json
{
  "imageIds": ["image-id-1", "image-id-2"],
  "tagName": "travel"
}
```

### Tags

| Method | Path | Description | Auth |
| --- | --- | --- | --- |
| GET | `/api/tags` | List all tags | Yes |
| POST | `/api/tags` | Create tag if it does not already exist | Yes |

Create tag body:

```json
{
  "name": "travel"
}
```

## Image Processing Behavior

When an image is uploaded:

1. The file is saved temporarily.
2. EXIF metadata is extracted.
3. GPS metadata is extracted when available.
4. Camera make/model is extracted when available.
5. A SHA-256 hash is calculated.
6. Existing records are checked for the same hash to avoid exact duplicates.
7. The original image is copied to the configured storage directory.
8. A thumbnail is generated.
9. Metadata is saved in MongoDB.
10. The temporary file is deleted.

If no EXIF taken date is available, the current server time is used.

## Startup Bulk Upload

The application includes a startup bulk upload runner controlled by:

```properties
bulk.upload.enabled=false
bulk.upload.src.dir.path=D:\\temp
bulk.upload.batch.size=20
bulk.upload.thread.pool.size=8
```

When `bulk.upload.enabled=true`, the runner scans `bulk.upload.src.dir.path` for image files, processes them in batches, and deletes the source files after successful processing.

Supported extensions:

```text
.jpg, .jpeg, .png, .gif, .bmp, .webp
```

The runner also reads tags from:

```text
<bulk.upload.src.dir.path>/tags.txt
```

Tags should be comma-separated.

## Known Issues and Improvements

- Hardcoded auth credentials should be replaced with database-backed users and hashed passwords.
- JWT secret should be externalized into environment-specific configuration.
- Uploaded filenames should be sanitized before being used in filesystem paths.
- The current test class is commented out, so `./mvnw test` verifies compilation but does not provide real behavior coverage.
- `application.properties` contains machine-specific paths and production-like MongoDB defaults; use profiles or environment variables for local/prod separation.
