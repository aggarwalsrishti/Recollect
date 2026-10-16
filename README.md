# Recollect — Personal Save-for-Later App

Recollect is an Android save-for-later application built using Kotlin and Jetpack Compose. It allows users to save notes, links, and images, organize their saved content, and access their items through local storage and Firebase-backed cloud storage.

## Project Status

- **Stage 1 — Local Save-for-Later:** Implemented
- **Stage 2 — Authentication and Cloud Storage:** Implemented
- **Stage 3 — Share into App:** Implemented
- **Advanced Mission — Better Sync Behaviours:** Completed

## Features

### Stage 1 — Local Save-for-Later

- Create, view, edit, and delete saved items.
- Save notes, links, and images.
- Organize content using tags.
- Search and filter saved items.
- Archive and restore items.
- Persist saved content locally using Room Database.

### Stage 2 — Authentication and Cloud Storage

- Register and sign in using email and password.
- Authenticate users through Firebase Authentication.
- Store saved items in Cloud Firestore.
- Associate items with their respective user accounts.
- Access cloud-saved items after signing in on another device.
- Handle cloud synchronization failures and retry pending operations.

### Stage 3 — Share into App

- Receive shared text and URLs from other Android applications through the Share menu.
- Receive shared images.
- Open the item editor with shared content prefilled.
- Handle unsupported or malformed shared content without crashing the application.

### Advanced Mission — Better Sync Behaviours

Recollect includes improvements that make synchronization more resilient when network connectivity or cloud operations fail.

- **Local-first persistence:** Save items locally so temporary cloud failures do not prevent local data from being stored.
- **Retry pending synchronization:** Retry cloud operations that have not completed successfully.
- **Offline resilience:** Keep locally stored content available when cloud synchronization is unavailable.
- **Deletion synchronization:** Track deletions so removed items can be handled during synchronization.
- **Conflict handling:** Use timestamps to help reconcile differences between local and cloud versions of an item.

## Authentication, Data Storage and Privacy

### Authentication

Recollect uses Firebase Authentication with email and password. Users must authenticate to access their account's cloud data.

### Where Data Is Stored

- **Local storage:** Room Database stores saved items on the device.
- **Cloud storage:** Cloud Firestore stores user-specific saved items under the corresponding user's data path.
- **Authentication data:** Firebase Authentication manages user sign-in and account identity.

### How User Data Is Protected

Cloud data access is protected by backend Firestore Security Rules rather than relying only on UI restrictions.

The intended access model is that an authenticated user can access only their own saved items. Requests attempting to access another user's data must be rejected by Firestore Security Rules, even if the requester changes an item ID or constructs a different request.

**Security configuration:** Ensure the deployed Firestore Security Rules enforce this ownership restriction and have been tested with separate user accounts before submission.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **UI Components:** Material 3
- **Local Database:** Room
- **Authentication:** Firebase Authentication
- **Cloud Database:** Cloud Firestore
- **Architecture Components:** ViewModel, Kotlin Coroutines and Flow
- **Android Integration:** Intents and Activity Result APIs

## Architecture Diagram

The architecture diagram illustrates the interaction between the user interface, application logic, authentication, cloud services, and local persistence.

![Recollect Architecture Diagram](docs/architecture.png)

## Project Structure

```text
com.example.recollect/
├── data/
│   ├── cloud/
│   │   └── CloudRepository.kt
│   └── room_database/
│       ├── SaveDatabase.kt
│       ├── SaveItem.kt
│       └── SaveItemDao.kt
├── repository/
│   └── SaveRepository.kt
├── ui/
│   └── screens/
│       ├── AuthScreen.kt
│       ├── ItemEditorDialog.kt
│       ├── SaveItemCard.kt
│       └── SaveScreen.kt
├── viewmodel/
│   ├── AuthViewModel.kt
│   └── SaveViewModel.kt
└── MainActivity.kt
```

## How to Run

### Prerequisites

- Android Studio
- A compatible Android SDK
- An Android emulator or physical Android device
- A Firebase project configured for the application

### Setup Instructions

1. Clone this repository:

   ```bash
   git clone <YOUR_GITHUB_REPOSITORY_URL>
   ```

2. Open the project in Android Studio.
3. Configure Firebase and add the appropriate `google-services.json` file to the app module.
4. Enable Email/Password authentication in the Firebase Console.
5. Configure Cloud Firestore and deploy the required user-ownership security rules.
6. Sync the project with Gradle.
7. Build and run the application on an emulator or connected Android device.

## APK Download

Download and install the Recollect Android application using the link below.

**[Download Recollect APK](https://drive.google.com/file/d/1YLfM0QMJz5TVAbKGbEyObD55wizafQki/view?usp=drive_link](https://drive.google.com/file/d/1gMCRxCa5vRxGxF2Vg2pETGMO1XRlCRtg/view?usp=sharing)**



## Technical Decisions

- **Room Database:** Provides structured local persistence and preserves saved items across application restarts.
- **Jetpack Compose:** Provides a declarative approach to building the Android interface.
- **ViewModel and repository separation:** Separates UI state and application actions from data-access responsibilities.
- **Firebase Authentication:** Provides account-based authentication without requiring a custom authentication server.
- **Cloud Firestore:** Supports remote storage of user-specific items across devices.
- **Local-first data handling:** Keeps locally stored content available when remote synchronization fails.
- **Synchronization retry and conflict handling:** Improves resilience when cloud operations fail or local and remote versions differ.
- **Android share intents:** Allow supported content from other apps to be received and prefilled in the editor.

## Known Limitations

- Image access can depend on Android URI permissions and the continued availability of the referenced image.
- Cloud functionality requires correct Firebase configuration and deployed Firestore security rules.
- Conflict resolution may not cover every possible simultaneous-edit scenario.


