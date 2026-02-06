
AegisPass - Secure Password Manager (Android Application)
AegisPass is a robust and highly secure Android application designed to help users manage their passwords safely and efficiently. Developed with a strong emphasis on data protection and user privacy, AegisPass stores passwords securely on the device using advanced encryption techniques and provides tools for assessing password strength and detecting data breaches.

✨ Features
Secure Password Storage:

Passwords are encrypted using AES-256 encryption with a unique salt for each entry, ensuring maximum security.

The master password, which secures access to the application, is protected using jBCrypt hashing and stored securely in EncryptedSharedPreferences.

All sensitive data resides on the device, never transmitted to external servers.

Digital Footprint & Risk Assessment:

Analyzes password strength, identifies password reuse, and assesses password age to help users maintain strong and unique credentials.

Integrates with the HaveIBeenPwned API (in a privacy-preserving manner) to check for data breaches, alerting users if their accounts might be compromised.

Advanced Security Mechanisms:

Root/Jailbreak Detection: Prevents the application from running on compromised devices, mitigating risks associated with insecure environments.

App Tamper Detection: Verifies the integrity of the application package at runtime, protecting against unauthorized modifications.

Code Obfuscation (ProGuard/R8): Applies obfuscation to the compiled code, making reverse engineering significantly more difficult.

Secure Logging: Implements practices to prevent sensitive information from being inadvertently exposed through application logs.

TLS Pinning: Ensures that the application only communicates with trusted servers by validating server certificates, preventing man-in-the-middle attacks.

🚀 Technologies Used
Language: Kotlin

Platform: Android SDK (Native Development)

Database: Room Persistence Library (for secure local data storage)

Encryption: AES-256, jBCrypt

API Integration: HaveIBeenPwned API

Security Tools: ProGuard/R8

🏗️ Architecture Overview
AegisPass follows a clean Android architecture, leveraging Kotlin for robust and concise code. The Room Persistence Library is used for local database management, providing an abstraction layer over SQLite for secure and efficient data storage. Security features are deeply integrated into the application's core, ensuring that data is protected from creation to storage.

🔒 Security Deep Dive
The security of your passwords is the highest priority for AegisPass:

Encryption at Rest: Each password entry is individually encrypted using AES-256 with a unique, cryptographically secure salt. This means even if the device storage is compromised, decrypting passwords without the master key is extremely difficult.

Master Password Hashing: Your master password is never stored directly. Instead, its jBCrypt hash is stored in EncryptedSharedPreferences, which itself is encrypted by the Android system.

On-Device Processing: All sensitive operations, including encryption/decryption and breach checks, are performed locally on your device. No password data ever leaves your device.

Proactive Threat Detection: Root/Jailbreak detection and app tamper detection actively work to identify and prevent the app from running in potentially insecure environments.

Secure API Interaction: TLS Pinning ensures that when checking against the HaveIBeenPwned API, the connection is always made to the legitimate server, preventing malicious intermediaries.

⚙️ Setup and Installation
To get AegisPass running on your local machine:

Clone the repository:

git clone https://github.com/Mspanti/AegisPass.git

Open in Android Studio:
Navigate to the cloned directory and open the project in Android Studio.

Sync Gradle:
Allow Gradle to sync and download all necessary dependencies.

Build and Run:
Connect an Android device or start an emulator and run the application.

Note: You may need to configure API keys for the HaveIBeenPwned service if they are not already integrated into the build configuration. Refer to the project's local.properties or similar configuration files for details.

🤝 Contributing
Contributions are welcome! If you have suggestions for improvements, new features, or find any issues, please feel free to open an issue or submit a pull request.

📄 License
This project is licensed under the MIT License. (Assuming MIT License, please replace with your actual license if different).


Project Screen Shots:
<div style="display: flex; flex-wrap: wrap; gap: 10px; justify-content: center;">

  <img src="https://github.com/user-attachments/assets/ef87dae8-f190-4225-8b95-08a6829e708a" width="200" />
  <img src="https://github.com/user-attachments/assets/4e1d29d6-a93a-42ef-9846-962f1f28b7cf" width="200" />
  <img src="https://github.com/user-attachments/assets/67952666-2bd6-40ad-8f2b-1b1cf368acab" width="200" />
  <img src="https://github.com/user-attachments/assets/c38d7855-b185-4ddb-9ee1-328cc81f3028" width="200" />

</div>

