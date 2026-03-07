# 🖼️ PixServe

**PixServe** is a personal image and media management web application built using **React (frontend)**, **Spring Boot (backend API)**, and **MongoDB (database)**.

It allows you to upload, organize, and view your local images along with metadata like tags, location, and timestamps—all stored efficiently and privately.

---

## 🚀 Tech Stack

- ⚛️ **Frontend**: React (`pix-ui/`) - React : 19.1.0 | vite : 7.0.4
- ☕ **Backend**: Spring Boot (`pix-service/`) - Spring boot : 3.5.4 | Java : 17
- 🍃 **Database**: MongoDB - Mongo DB CE : 8.0
- 🗂️ **Storage**: Local filesystem (image files in `uploads/`)

---

## 🎯 Features

- Upload images with tags, timestamp, and location
- Store image metadata in MongoDB
- Serve and preview uploaded images
- REST API to fetch metadata and files
- Designed for local or private deployment

---

## 📁 Project Structure

## 📁 Project Structure

pixserve/
├── pix-service/ # Spring Boot API service
│ ├── controller/
│ ├── model/
│ ├── repository/
│ ├── service/
│ └── application.properties
├── pix-ui/ # React app
│ ├── src/
│ └── public/
├── uploads/ # Uploaded images
└── README.md
