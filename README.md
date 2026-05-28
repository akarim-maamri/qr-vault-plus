<img width="1280" height="960" alt="WhatsApp Image 2026-05-28 at 11 48 28 AM" src="https://github.com/user-attachments/assets/68be4584-3721-4e6f-849c-7c577ac5fe0f" />
# +QR Vault

A simple Android application for scanning, generating, and storing QR codes within a secure local vault.

---

## Features

### 1. QR & Barcode Scanning
* Scan QR codes and barcodes using the device camera.
* View local scan history.
* Copy and share scan results.

### 2. QR Code Generation
* Support for text and URLs.
* Support for WiFi networks (Network name, password, and security type: WPA/WEP/None).
* Save generated codes locally as PNG images or share them.

### 3. Secure Local Vault
* Lock contents with a 4-digit PIN.
* Support for biometric authentication (Fingerprint and Face Unlock) depending on device availability.
* Local data encryption on the device.
* Backup recovery code to restore access if the PIN is forgotten.
* Automated photo capture after 3 consecutive incorrect PIN attempts.

### 4. Backup & Restore
* Export the database as an encrypted local file.
* Upload backups directly to the user's personal Google Drive storage.
* Optional backup reminders every 15 days (starting 15 days after installation).

### 5. Home Screen Widget
* A simple widget for quick access to scanning, generation, and vault features.

---

## Technical Specifications

| Item | Value |
| :--- | :--- |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose |
| **Minimum Android SDK** | API 24 (Android 7.0) |
| **Target Android SDK** | API 36 |
| **Database** | Room (SQLite) |
| **Scanning Tech** | ML Kit Barcode Scanning + CameraX |
| **Generation Tech** | ZXing |
| **Authentication** | Firebase Google Sign-In |
| **Cloud Backup** | Google Drive API v3 |
| **Version** | 1.0 |

---

## Requirements

* Android Studio Hedgehog or newer.
* JDK 17.
* A `google-services.json` file from Firebase (required for Google Sign-In).
* Google Drive API enabled in your Google Cloud Console.

---

## How to Run the Project Locally

```bash
# Clone the repository
git clone [https://github.com/akarim-maamri/qr-vault-plus.git](https://github.com/akarim-maamri/qr-vault-plus.git)

# Navigate to the project directory
cd qr-vault-plus

# Note: Please place your google-services.json file inside the app/ directory before building.
# Build the project
./gradlew assembleDebug

Notes
Privacy: The application works completely offline. It does not send data to external servers; all data remains on your device or your personal Google Drive storage.

Security: The google-services.json file is not included in this repository for security reasons. You must generate it from your own Firebase Console to build the project.

Privacy Policy: You can review the privacy policy here.

Developer
Name: MAAMRI ABDELKARIM

Email: Jussor.Tech@gmail.com

GitHub: akarim-maamri
