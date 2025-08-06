# 🏋️‍♀️ CoreFlex - Personalized Pilates Studio Management App

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)](https://firebase.google.com/)
[![API Level](https://img.shields.io/badge/Min%20SDK-26-blue.svg)](https://developer.android.com/about/versions)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35-blue.svg)](https://developer.android.com/about/versions)


A comprehensive Android application for managing a personalized Pilates studio, featuring class scheduling, trainer management, social interactions, and real-time notifications.

## 📋 Table of Contents

- [✨ Features](#-features)
- [🏗️ Architecture](#️-architecture)
- [🛠️ Tech Stack](#️-tech-stack)
- [🚀 Installation](#-installation)
- [⚙️ Configuration](#️-configuration)


## ✨ Features

### 🔐 Authentication & User Management
- Firebase Authentication integration
- Role-based access control (Admin/User)
- Secure user registration and login
- Password recovery option
- Profile management with customizable goals and skill levels

### 📅 Class & Lesson Management
- Interactive calendar with lesson scheduling
- Multi-level Pilates classes (Beginner, Intermediate, Advanced)
- Real-time class capacity tracking
- Lesson filtering by date, trainer, and skill level
- Booking and cancellation system

### 👨‍🏫 Trainer Management (Admin)
- Add, edit, and remove trainers
- Trainer profile management with specialties
- Image upload support for trainer profiles
- Lesson assignment to trainers

### 👫 Social Features
- Friend system with request/accept functionality
- Invite friends to join specific classes
- View friends' lesson schedules
- Real-time friend notifications

### 🔔 Notifications
- Firebase Cloud Messaging integration
- Friend request notifications
- Class reminder notifications
- Real-time updates

### 📊 Personal Dashboard
- View upcoming and past lessons
- Track personal fitness goals
- Booking history and management
- Profile customization


## 🏗️ Architecture

CoreFlex follows modern Android architecture patterns

### Project Structure
```
app/src/main/java/com/example/coreflexpilates/
├── model/                          # Data models
│   ├── User.kt
│   ├── Lesson.kt
│   ├── Trainer.kt
│   ├── Booking.kt
│   └── Friendship.kt
├── ui/
│   ├── home/                      # Home screen with lesson listing
│   ├── admin/                     # Admin management screens
│   ├── friends/                   # Social features
│   ├── lesson/                    # Lesson booking and details
│   ├── profile/                   # User profile management
│   ├── login/                     # Authentication
│   └── notifications/             # Push notification handling
├── MainActivity.kt                 # Main activity with navigation
├── AuthActivity.kt                # Authentication flow
├── AdminActivity.kt               # Admin dashboard
└── LauncherActivity.kt            # App launcher and routing
```

## 🛠️ Tech Stack

### Frontend
- **Language:** Kotlin
- **UI Framework:** Android Views with ViewBinding
- **Navigation:** Android Navigation Component
- **Architecture Components:** LiveData, ViewModel
- **Material Design:** Material Design Components

### Backend & Services
- **Authentication:** Firebase Authentication
- **Database:** Cloud Firestore
- **Storage:** Firebase Cloud Storage
- **Notifications:** Firebase Cloud Messaging
- **Analytics:** Firebase Analytics

### Dependencies
- **Image Loading:** Glide
- **UI Components:** Material Design Components
- **Authentication:** Google Play Services Auth
- **Testing:** JUnit, Espresso

## 🚀 Installation

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 26 or higher
- Firebase project setup
- Google Services configuration

### Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone https://github.com/your-username/coreflex-pilates.git
   cd coreflex-pilates
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned directory and select it

3. **Firebase Configuration**
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Add your Android app to the Firebase project
   - Download the `google-services.json` file
   - Place it in the `app/` directory

4. **Build the Project**
   ```bash
   ./gradlew build
   ```

5. **Run the Application**
   - Connect an Android device or start an emulator
   - Click the "Run" button in Android Studio

## ⚙️ Configuration

### Firebase Setup

1. **Authentication**
   - Enable Email/Password authentication in Firebase Console
   - Configure sign-in methods as needed

2. **Firestore Database**
   - Create a Firestore database
   - Set up collections: `users`, `lessons`, `trainers`, `bookings`, `friendships`, `friend_requests`

3. **Cloud Messaging**
   - Enable Firebase Cloud Messaging
   - Configure server keys for push notifications

4. **Storage**
   - Enable Firebase Storage for trainer profile images

---

<p align="center">
  <strong>Built with ❤️ for the Pilates community</strong><br>
  <em>Transforming fitness through technology</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Made%20with-Kotlin-purple.svg" alt="Made with Kotlin">
  <img src="https://img.shields.io/badge/Powered%20by-Firebase-orange.svg" alt="Powered by Firebase">
  <img src="https://img.shields.io/badge/Built%20for-Android-green.svg" alt="Built for Android">
</p>