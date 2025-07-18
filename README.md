NousGuard - Offline AI Mental Wellness Companion (Android Application)
NousGuard is a robust and secure Android application designed to be an offline AI-powered companion for mental wellness. It integrates advanced AI capabilities for sentiment and intent detection directly on the device, ensuring complete privacy and accessibility without any cloud dependencies. With a focus on user security and a seamless offline experience, NousGuard provides a secure journaling system and real-time emotional support.

✨ Features
Offline AI-Powered Sentiment & Intent Detection: Integrates a hybrid AI pipeline (TensorFlow Lite + ONNX) to analyze user input and detect emotional sentiment and conversational intent without requiring an internet connection.

Biometric-Access Secured Journaling System: Provides a private and secure space for users to journal their thoughts and feelings, protected by biometric authentication.

Multi-Layered Security Architecture: Implements comprehensive security measures including:

Root/Jailbreak Detection: Prevents the app from running on compromised devices.

App Tamper Detection: Ensures the app's integrity against unauthorized modifications.

AES-256 Encryption: Securely encrypts sensitive user data (like journal entries) on the device.

Optimized Mobile Performance: Utilizes model distillation and primitive type optimization techniques to ensure the AI models run efficiently on mobile devices with minimal resource consumption.

Zero Cloud Dependencies: All AI processing and data storage occur locally on the device, guaranteeing user privacy and full offline functionality.

Custom Synthetic Emotional Datasets: Models were trained using specially curated synthetic emotional datasets to enhance performance in sentiment and intent understanding.

🚀 Technologies Used
Language: Kotlin, Java

AI/ML Frameworks: TensorFlow Lite (TFLite), ONNX (Open Neural Network Exchange)

Database: Room Persistence Library (for secure journaling)

Security: AES-256 encryption, jBCrypt (for master password hashing), EncryptedSharedPreferences

Development Environment: Android SDK (Native Development)

🏗️ Architecture Overview
NousGuard employs a hybrid on-device AI architecture. User input is processed through a pipeline that leverages both TensorFlow Lite and ONNX runtime for efficient sentiment and intent detection. This dual-framework approach ensures robust performance and flexibility in model deployment. All data processing and storage are handled locally, adhering to a strict privacy-by-design principle.

🔒 Security Measures
Security is paramount in NousGuard. The application incorporates several advanced mechanisms to protect user data and ensure app integrity:

Data Encryption: All sensitive data, including journal entries and master password hashes, are encrypted using AES-256 with unique salts.

Master Password Protection: The master password is secured using jBCrypt hashing and stored in EncryptedSharedPreferences.

Device Integrity Checks: Implements sophisticated checks for rooted/jailbroken devices and detects any unauthorized tampering with the application package.

ProGuard/R8: Code obfuscation is applied to enhance security against reverse engineering.

⚙️ Setup and Installation
To set up and run NousGuard on your local machine:

Clone the repository:

git clone https://github.com/Mspanti/NousGuard.git

Open in Android Studio:
Navigate to the cloned directory and open the project in Android Studio.

Sync Gradle:
Allow Gradle to sync and download all necessary dependencies.

Build and Run:
Connect an Android device or start an emulator and run the application.

Note: This project involves pre-trained AI models. Ensure all necessary model files (e.g., TFLite, ONNX) are correctly placed in the app/src/main/assets folder as per the project structure.

🤝 Contributing
Contributions are welcome! If you have suggestions for improvements or find any issues, please open an issue or submit a pull request.

📄 License
This project is licensed under the MIT License. (Assuming MIT License, please replace with your actual license if different).
