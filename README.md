# LensLink

**Professional photography bookings for Android.** LensLink connects clients with photographers and studios: clients browse packages, book a time slot, message their photographer, ask an assistant questions and download their finished photos. Photographers manage booking requests, publish packages, message clients and deliver photos straight from their phone's gallery.

---

## Table of contents

1. [Feature overview](#feature-overview)
2. [Roles and access control (RBAC)](#roles-and-access-control-rbac)
3. [Screens and user flows](#screens-and-user-flows)
4. [Tech stack](#tech-stack)
5. [Architecture](#architecture)
6. [Project structure](#project-structure)
7. [Data model](#data-model)
8. [Design system](#design-system)
9. [Backend: what is real and what is mocked](#backend-what-is-real-and-what-is-mocked)
10. [Photo delivery pipeline](#photo-delivery-pipeline)
11. [Getting started](#getting-started)
12. [Demo data](#demo-data)
13. [Known limitations](#known-limitations)
14. [Roadmap](#roadmap)

---

## Feature overview

### Clients
- Sign up and log in with email and password
- Home dashboard with quick actions and the next upcoming session
- Browse photographer packages (price, duration, what is included) and jump straight into booking one
- Book a session: choose photographer and package, pick a date on a calendar, pick a time slot, set a location
- Slot locking: a slot already held for a photographer on a date shows as Booked and cannot be selected
- My Bookings with colour-coded status (Pending, Confirmed, Completed, Cancelled), cancel with confirmation
- Download All: saves every photo for a shoot to the phone in original quality
- Chat with two modes: the LensLink AI Assistant and direct messages to the photographer
- Upcoming-session reminder from the bell icon (dialog plus notification)
- Profile with log out

### Photographers / studios
- Separate sign up and login as "Photographer / Studio"
- Dashboard with stats (pending requests, confirmed sessions, live packages), quick actions, requests needing a reply and the next session
- Bookings list with status filters (All, Pending, Confirmed, Completed, Cancelled)
- Accept or decline requests, mark sessions complete, cancel confirmed sessions (all with confirmation where it matters)
- Upload photos from the device gallery with multi-select and live progress
- Packages: publish new packages (title, price, hours, inclusions) and remove old ones; published packages appear to clients immediately
- Direct messaging with clients (no AI assistant in this role)
- Profile with log out

---

## Roles and access control (RBAC)

There are two roles, stored on the user record: `client` and `photographer`.

| Capability | Client | Photographer |
|---|:---:|:---:|
| Browse services | yes | no |
| Book a session | yes | no |
| View and cancel own bookings | yes | no |
| Download photos | yes | no |
| **AI assistant chat** | **yes** | **no** |
| Direct messaging | yes | yes |
| Manage bookings (accept, decline, complete) | no | yes |
| Upload photos | no | yes |
| Manage packages | no | yes |

RBAC is enforced in layers:

1. **Login check.** The account type chosen on the login screen must match the role stored on the account, otherwise the user is told which type the account is registered as and is signed out.
2. **Routing.** After login each role goes to its own Activity (`MainActivity` for clients, `PhotographerActivity` for photographers).
3. **Entry guard.** `RoleGuard.enforce()` runs first in each role Activity. It rejects anyone who is not signed in to Firebase or holds the wrong role and redirects them to the correct home or to login. Both role Activities are `exported="false"` in the manifest.
4. **Role-shaped navigation.** Each role is given its own bottom navigation items. Photographers have no Chat/AI tab.
5. **Feature permissions.** `Permissions.can(role, Feature)` is checked before sensitive actions (booking, status changes, uploads, downloads, package changes, AI replies). `ChatRepository` also refuses AI replies for non-clients as defence in depth.
6. **Server-side (Firebase rules).** `backend/storage.rules` and `backend/firestore.photos.rules` restrict photo uploads to photographer accounts. Client-side checks are a UX layer; the rules are the real enforcement.

---

## Screens and user flows

### Authentication (`LoginActivity`)
- Log In / Sign Up segmented toggle
- Account type selector (Client or Photographer / Studio); the name field becomes "Studio Name" for photographers
- Validation, loading state, already-signed-in users are routed straight to their home

### Client app (`MainActivity`) - bottom nav: Home, Bookings, Chat, Profile
- **Home:** greeting, four quick-action cards (Browse Services, Book a Session, My Bookings, Chat Assistant), upcoming session card, reminder bell
- **Browse Services:** package cards with a "Book This Photographer" button that preselects the photographer and package
- **Book a Session:** photographer and package dropdowns, calendar (past dates disabled), time slots (available, selected, booked), location, price summary, Confirm Booking
- **My Bookings:** cards with status chip, Download All and Cancel
- **Chat:** toggle between *LensLink AI Assistant* (suggestion chips plus free text, conversation kept while switching tabs) and *Message Photographer*
- **Profile:** avatar, name, email, role badge, log out

New bookings are created as **Pending** and hold their slot until the photographer accepts or declines.

### Photographer app (`PhotographerActivity`) - bottom nav: Home, Bookings, Packages, Messages, Profile
- **Home:** stats, quick actions (Manage Bookings, Add a Package, Deliver Photos, Messages), requests needing a reply with Accept/Decline, next confirmed session
- **Bookings:** filter chips and booking cards with context-aware actions
  - Pending: Accept, Decline
  - Confirmed: Upload Photos, Mark Complete, Cancel Booking
  - Completed: Upload Photos
- **Packages:** publish form with validation, list of own packages with remove
- **Messages:** conversation header with the client's name and their latest shoot, message thread, send box
- **Profile:** studio name, email, role badge, log out

---

## Tech stack

| Area | Technology |
|---|---|
| Platform | Native Android |
| Language | Java |
| UI toolkit | Android Views (XML layouts) with programmatic view building for lists |
| Components | Material Components (`MaterialButton`, `Theme.MaterialComponents.Light.NoActionBar`), AppCompat |
| Jetpack | `androidx.core` (insets, notifications), `androidx.activity` (Activity Result API, back dispatcher) |
| Authentication | Firebase Authentication (email and password) |
| Database | Cloud Firestore (users; photo metadata; other collections declared for the migration) |
| File storage | Firebase Cloud Storage (delivered photos) |
| Downloads | Android `DownloadManager` into `Pictures/LensLink/` |
| Local session | `SharedPreferences` (`SessionManager`) |
| Notifications | `NotificationCompat` with a notification channel |
| Icons | Vector drawables (Material icon paths) |
| Typography | Platform serif for brand and headings, default sans for body |

> The Gradle build files were not part of the uploaded project, so dependency versions are not pinned here. You need Firebase Auth, Firestore and **Storage**, Material Components, AppCompat and a reasonably current `androidx.core` / `androidx.activity`.

---

## Architecture

A light layered structure, with callbacks as the async boundary so the mocked and Firebase-backed repositories are interchangeable.

```
Activities (screens)        MainActivity, PhotographerActivity, LoginActivity
   |  use
UI helpers / components     BottomNavBar, UiUtils, SystemBars
   |  guarded by
Access control              SessionManager, RoleGuard, Permissions
   |  call
Repositories                Auth, Booking, Package, Gallery, Chat   (RepositoryCallback<T>)
   |  read/write
Models                      User, Booking, Package, Photo, Message, Service
   |
Firebase (Auth, Firestore, Storage)   and in-memory mock data
```

Key design points:
- **Repositories return results through `RepositoryCallback<T>`** (`onSuccess` / `onError`). Screens never know whether data came from memory or Firestore, so moving a repository to Firestore does not change the UI code.
- **`BottomNavBar`** is one reusable component. Each Activity supplies its own item list, which is how navigation differs per role.
- **`SystemBars`** applies edge-to-edge insets so content clears the status bar and gesture bar, and hides the bottom nav while the keyboard is open.
- **`SessionManager`** persists the signed-in user's id, name, email and role.

---

## Project structure

```
LensLink/
├── README.md
├── backend/
│   ├── README.md                 Firebase setup notes for photo delivery
│   ├── storage.rules             Storage rules (photographer-only uploads)
│   └── firestore.photos.rules    Firestore rules block for the Photos collection
└── app/src/main/
    ├── AndroidManifest.xml
    ├── java/com/example/lenslink/
    │   ├── LoginActivity.java            Log in / sign up, role selection, routing
    │   ├── MainActivity.java             Client experience
    │   ├── PhotographerActivity.java     Photographer experience
    │   ├── models/                       Booking, Message, Package, Photo, Service, User
    │   ├── repositories/                 AuthRepository, BookingRepository, PackageRepository,
    │   │                                 GalleryRepository, ChatRepository, RepositoryCallback
    │   ├── ui/BottomNavBar.java          Reusable icon + label bottom navigation
    │   └── utils/                        FirestoreConstants, Permissions, RoleGuard,
    │                                     SessionManager, SystemBars, UiUtils
    └── res/
        ├── layout/                       Screen layouts + shared pieces (profile, chat input, buttons, spinner rows)
        ├── drawable/                     Backgrounds (cards, inputs, pills, bubbles, slots) and vector icons
        └── values/                       colors, themes, styles, ids
```

---

## Data model

Collection names are declared in `FirestoreConstants`.

| Model | Key fields |
|---|---|
| `User` | `userId`, `fullName`, `email`, `phone`, `role` (`client` or `photographer`) |
| `Package` | `packageId`, `serviceId`, `photographerName`, `name`, `price`, `durationHours`, `inclusions` |
| `Booking` | `bookingId`, `clientId`, `clientName`, `photographerId`, `photographerName`, `serviceTitle`, `date` (`dd/MM/yyyy`), `timeSlot` (e.g. `02:00 PM`), `location`, `status`, `totalPrice` |
| `Photo` | `photoId`, `bookingId`, `galleryId`, `imageUrl`, `caption`, `fileName`, `uploadedBy`, `storagePath`, `uploadedAt` |
| `Message` | `messageId`, `chatId`, `senderId`, `senderRole`, `text`, `timestamp`, `isBot` |

Booking status values: `Pending`, `Confirmed`, `Completed`, `Cancelled`.

**Slot locking.** A slot is locked for a photographer on a date whenever a booking exists for it with any status other than Cancelled. Cancelling or declining frees the slot again. `createBooking` rejects a request for a locked slot.

---

## Design system

The look is warm, editorial and quiet so the photography does the talking.

- **Palette:** cream background `#F9F5F0`, white surfaces, near-black `#1C1C1E` for primary actions and text, warm grey for secondary text and toggles
- **Status colours:** green (Confirmed), amber (Pending), slate (Completed), red (Cancelled), each with a soft tinted background chip
- **Shape:** fully rounded pill inputs and buttons, 20dp cards, soft 1dp borders instead of heavy shadows
- **Typography:** serif for the LensLink wordmark and screen titles, sans-serif for body copy, small caps-style labels for form sections
- **Buttons:** dark pill (primary) and outlined pill (secondary), plus compact variants for use inside cards
- **Always light:** the theme extends `Theme.MaterialComponents.Light.NoActionBar` so system dark mode cannot turn widgets white on cream
- **Layout:** edge-to-edge with inset handling, bottom nav with an active pill behind the selected icon

Styles live in `res/values/styles.xml` and `themes.xml`; colours in `colors.xml`.

---

## Backend: what is real and what is mocked

| Feature | Status |
|---|---|
| Sign up / log in / log out | **Firebase Auth**; user profile and role stored in Firestore `Users` |
| Photo upload | **Firebase Storage** (files) + **Firestore** `Photos` (metadata) |
| Photo download | **Real** (`DownloadManager`) |
| Bookings | In-memory mock (resets when the app restarts) |
| Packages | In-memory mock (resets when the app restarts) |
| Direct messages | In-memory mock |
| AI assistant | Rule-based keyword replies (no external AI service) |

Because bookings and packages are mocked, **ownership is matched by name**: clients see bookings whose client name equals their account name, photographers see bookings and packages whose studio name equals their account name. When moving to Firestore, replace this with queries on user ids (for example `whereEqualTo("clientId", uid)`).

If a signup name is left blank, the app defaults to `Bee Lemme` (client) or `Barred Production Studios` (photographer) so the demo data lines up.

---

## Photo delivery pipeline

**Upload (photographer)**
1. Tap *Upload Photos* on a confirmed or completed booking
2. The system gallery picker opens (`GetMultipleContents`, `image/*`); select any number of photos. No storage permission is needed
3. Each original file is uploaded sequentially to `galleries/{bookingId}/{photoId}.ext` in Firebase Storage with its MIME type
4. The download URL is stored in a `Photos` document along with file name, uploader id, storage path and timestamp
5. A progress dialog shows "Uploading n of N"; the result reports uploaded and failed counts

**Download (client)**
1. Tap *Download All* on a booking; the app loads every photo for that booking
2. A confirmation shows the photo count and where files will be saved
3. All photos are queued with `DownloadManager` into `Pictures/LensLink/<shoot name>/` in original quality, with de-duplicated file names
4. A single grouped progress notification covers large batches; the photos then appear in the phone's gallery for the client to sort

Android 9 and below needs `WRITE_EXTERNAL_STORAGE` for this, which the manifest declares with `maxSdkVersion="28"`.

---

## Getting started

### Prerequisites
- Android Studio (current stable) and an Android device or emulator
- A Firebase project

### Setup
1. Clone or open the project in Android Studio.
2. In the [Firebase console](https://console.firebase.google.com/), create a project and add an Android app that uses this project's application id.
3. Download `google-services.json` and place it in `app/`.
4. Enable **Authentication** (Email/Password), **Cloud Firestore** and **Cloud Storage**. New projects may require the Blaze plan for Storage.
5. Make sure the app's `build.gradle` includes the Firebase Auth, Firestore and **Storage** dependencies (ideally via the Firebase BoM), Material Components and the Google services plugin.
6. Deploy `backend/storage.rules` and merge `backend/firestore.photos.rules` into your Firestore rules. See `backend/README.md`.
7. Build and run.

### Trying both roles
1. Sign up as **Client (User)** and as **Photographer / Studio**, using two different emails.
2. As the client, book a session with *Barred Production Studios* (leave the photographer's studio name as the default when signing up so the names match).
3. Log in as the photographer, accept the request, upload photos from the gallery.
4. Log back in as the client and tap *Download All*.

---

## Demo data

Seeded in memory for the mock phase:

- **Packages:** Graduation Standard (R1 500), Graduation Premium (R2 500), Wedding Full-Day (R8 500) from Barred Production Studios; Golden Hour Outdoor Session (R2 500) from Kagiso Visuals
- **Bookings** for the demo client "Bee Lemme": a confirmed Golden Hour session on 24/10/2026, a pending Graduation Standard session on 17/10/2026, and a confirmed Wedding Shoot on 24/05/2026
- **Photos:** sample images attached to two of the demo bookings so *Download All* can be tried before any upload

---

## Known limitations

- Bookings, packages and direct messages are not persisted yet; they reset on app restart
- Ownership is matched by name rather than user id (see above)
- Firebase rules can only check the user's role, not booking ownership, until bookings live in Firestore
- Photographer and client direct messages share a single demo thread rather than per-booking conversations
- Photos upload one at a time and while the app is open; very large batches on a slow connection take a while
- There is no in-app photo gallery viewer; clients download to the phone and view photos there
- The AI assistant is rule-based and answers a fixed set of topics (packages and prices, slot locking, downloading photos)
- Payments are not implemented; prices are shown for information
- No automated tests yet

---

## Roadmap

- Move bookings, packages and chats to Firestore with real-time updates and user-id based queries
- Tighten Firestore and Storage rules to booking ownership
- Per-booking conversations with unread indicators and push notifications (FCM)
- Photographer availability management (working hours, days off) instead of fixed slots
- In-app gallery with thumbnails, favourites and per-photo download, plus background upload with retry
- Real LLM-backed assistant for clients
- Payments and deposits, reviews and ratings
- Unit tests for repositories and permissions, UI tests for the booking flow

---

## Licence

No licence has been specified for this project yet.
