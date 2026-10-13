# Recollect 📝

**Save it now. Recollect it anytime.**

Recollect is an Android application designed to help users save and organize useful information in one place. Whether it's a note, a link, a code snippet, or an image, Recollect helps you keep important things handy so you can find them later.

## ✨ Features

- **Save Notes:** Store important thoughts, reminders, and information.
- **Save Links:** Keep useful URLs for quick access later.
- **Save Snippets:** Store text snippets or small pieces of code.
- **Save Images:** Save images for future reference.
- **Search:** Find saved items using their title, content, or tags.
- **Organize Items:** Filter items by type and view your inbox or archived items.
- **Archive and Restore:** Keep your inbox organized without permanently deleting items.
- **Edit and Delete:** Update saved information or remove items you no longer need.
- **Local Storage:** Keep saved items on your device using Room.
- **Firebase Authentication:** Sign in or create an account using Firebase Authentication.
- **Cloud Storage:** Save supported items to the cloud using Firebase and access cloud-backed data.
- **Simple Interface:** Manage your saved information through a clean Jetpack Compose interface.

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Kotlin | Main programming language |
| Android Studio | Development environment |
| Jetpack Compose | UI development |
| Material 3 | UI components and styling |
| Room Database | Local data persistence |
| Firebase Authentication | User sign-in and registration |
| Cloud Firestore | Cloud data storage |
| Kotlin Coroutines | Asynchronous operations |

## 📱 App Structure

The project is organized into several parts:

- `data/` — Local data models and database-related code.
- `repository/` — Data access and repository logic.
- `ui/screens/` — Authentication, item editor, saved-item card, and main save screen.
- `viewmodel/` — UI state and application logic.
- `MainActivity.kt` — Application entry point and authentication-based navigation.

*Note: Folder names may vary slightly depending on the final project structure.*

## 🚀 Getting Started

### Prerequisites

Before running Recollect, make sure you have:

- Android Studio installed.
- A compatible Android SDK.
- A physical Android device or Android emulator.
- A Firebase project configured for the application.

### Installation

1. Clone this repository:

   ```bash
   git clone https://github.com/YOUR-USERNAME/Recollect.git
   ```

2. Open Android Studio.
3. Select **Open** and choose the cloned project folder.
4. Configure Firebase for the project and add your own `google-services.json` file to the app module.
5. Sync the project with Gradle.
6. Run the app on an emulator or connected Android device.

Replace `YOUR-USERNAME` with your GitHub username.

## 🔥 Firebase Setup

Recollect uses Firebase Authentication and Cloud Firestore for cloud-related functionality.

1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Register an Android app using the package name `com.example.recollect`, if that is still the package name in your project.
3. Enable **Email/Password** sign-in under Firebase Authentication.
4. Create a Cloud Firestore database.
5. Add your Firebase configuration file to the app module.
6. Configure Firestore security rules so each authenticated user can access only their own data.

**Security note:** Never commit Firebase service-account keys, private credentials, or other secrets to a public repository. Keep `google-services.json` out of Git if you intend to exclude it using `.gitignore`.

## 🔐 Privacy and Data

- Items can be stored locally using Room.
- Cloud functionality depends on Firebase configuration and authentication.
- Firestore security rules should restrict access to each user's own records.
- Review your Firebase security rules before publishing the application.

## 🗺️ Future Improvements

Potential improvements include:

- Automatic synchronization between local and cloud data.
- Improved offline synchronization and conflict handling.
- Better tag management and filtering.
- Image upload and cloud image storage.
- Export and backup options.
- UI and accessibility improvements.


---

**Built with Kotlin, Jetpack Compose, Room, and Firebase.**
