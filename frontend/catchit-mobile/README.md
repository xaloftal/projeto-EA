
# CatchIt Mobile - README

## 🚀 How to Run the Project

### Prerequisites
- Node.js (v16+)
- Android Studio
- Java JDK 11+
- Android SDK

### Running the App

To run the project in development mode with Android emulator:

```bash
cd frontend/catchit-mobile
npx quasar dev -m capacitor -T android
```

This command will:
1. Start the Quasar development server
2. Build the Capacitor Android project
3. Launch the Android emulator
4. Install and run the app

### Opening in Android Studio

After running the command above, Android Studio will open automatically. If you need to open it manually:

1. Open Android Studio
2. Go to **File → Open**
3. Navigate to: `frontend/catchit-mobile/src-capacitor/android`
4. Click **OK**

To rebuild after native changes:
```bash
npx quasar build -m capacitor -T android
```

---

## 🏗️ Architecture

This project uses **Quasar Framework** with **Capacitor** for cross-platform mobile development.

### Project Structure

```
catchit-mobile/
├── public/              # Static public assets
├── src/
│   ├── assets/          # App assets and base styles
│   ├── boot/            # Quasar boot files (initialization)
│   ├── components/      # Reusable Vue components
│   ├── css/             # Global app CSS
│   ├── layouts/         # Main layout templates
│   ├── models/          # Domain and helper types
│   ├── pages/           # Application pages/routes
│   ├── router/          # Vue Router configuration
│   ├── services/        # API services (mock API)
│   ├── viewmodels/      # App state and business logic
│   ├── views/           # Screen-level views
│   └── App.vue          # Root component
├── src-capacitor/       # Native Capacitor project (Android)
├── src-pwa/             # PWA manifest and service worker
└── quasar.config.js     # Quasar configuration
```

### State Management
Uses shared reactive state in **src/viewmodels** with Vue refs/computed.

### Routing
Vue Router with lazy-loaded routes for optimal performance.

---

## 📱 Pages Overview

| Page | Description |
|------|-------------|
| **Home** | Main authenticated landing page |
| **Login** | User authentication |
| **Signup** | New user registration |
| **Search Tickets** | Route/ticket search flow |
| **Cards** | Travel cards browsing/purchase |
| **Checkout** | Payment and order confirmation flow |
| **Checkout Success** | Post-checkout confirmation |
| **Notifications** | User notifications view |
| **Profile** | User profile settings |
| **Error Not Found** | Fallback route for invalid paths |

---

## 🛠️ Technologies

- **Framework:** Quasar v2
- **Mobile:** Capacitor
- **State:** Vue reactivity via ViewModels
- **Router:** Vue Router 4
- **UI:** Quasar Components

---

## 📦 Build Commands

```bash
# Development
npx quasar dev -m capacitor -T android

# Production Build
npx quasar build -m capacitor -T android

# Add Platforms
npx quasar add capacitor android
```
