#!/bin/bash

echo "================================"
echo "PixServe - Full Build Process"
echo "================================"

# ================================
# Set backend URL here
# ================================
BASE_URL=

# ================================
# Step 0: Prepare React env file
# ================================
echo
echo "[0/4] Setting backend URL for React UI..."

cd pix-ui || exit

# Copy template env file
cp .env.template .env

# Replace PLACEHOLDER with actual URL
sed -i '' "s|PLACEHOLDER|$BASE_URL|g" .env

cd ..

# ================================
# Step 1: Build React UI
# ================================
echo
echo "[1/4] Building React UI..."

cd pix-ui || exit

npm install
npm run build

if [ $? -ne 0 ]; then
    echo "React build failed!"
    exit 1
fi

cd ..

# ================================
# Step 2: Copy dist -> Spring Boot static
# ================================
echo
echo "[2/4] Copying UI build to Spring Boot static..."

rm -rf pix-service/src/main/resources/static
mkdir -p pix-service/src/main/resources/static

cp -R pix-ui/dist/* pix-service/src/main/resources/static/

# ================================
# Step 3: Package Spring Boot JAR
# ================================
echo
echo "[3/4] Building Spring Boot JAR..."

cd pix-service || exit

chmod +x mvnw

./mvnw clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Maven build failed!"
    exit 1
fi

cd ..

# ================================
# Step 4: Move JAR to build folder
# ================================
echo

mkdir -p build

echo "Moving JAR to build folder..."

find pix-service/target -name "*.jar" -exec cp {} build/ \;

echo "================================"
echo "Build finished."
echo "JAR available in /build folder."
echo "================================"
