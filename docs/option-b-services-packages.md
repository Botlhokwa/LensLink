# Option B: Services, packages and add-ons

The Android app uses the existing LensLink Firebase project and creates these Firestore collections when the first offering is saved:

| Collection | Document fields |
| --- | --- |
| `services` | `photographerId`, `name`, `description`, `price` (ZAR number), `durationMinutes`, `inclusions` (string list), `addonIds` (document ID list), `active` |
| `packages` | `photographerId`, `category`, `name`, `description`, `price` (ZAR number), optional `twoDayPrice` and `partnerPrice`, `serviceIds`, `inclusions`, `addonIds`, `active` |
| `addons` | `photographerId`, `category`, `name`, `description`, `price` (ZAR number), `active` |

Each document's Firestore ID is the offering ID. `photographerId` should be the Firebase Authentication UID of the photographer who owns the offering. A package points to its included services and optional extras by document ID. The existing `Booking`, `daily_schedules`, and `users` collections are not changed by this feature.

## Security and account setup

The management screen accepts a photographer ID so it can be used with the app's current starter UI. The app does not yet implement Firebase Authentication or role-based Firestore rules. Before using this for real accounts, connect the signed-in Firebase Auth UID to the field and update the existing Firestore rules so photographers can only create, edit, and delete their own offerings. Do not allow the client app to choose a photographer ID as proof of ownership. Keep passwords in Firebase Authentication; never write them to Firestore.

The Firebase Console already has an `addons` reference on booking data (`addonIds`). Confirm with the booking teammate whether bookings should store `packageId`, `serviceId`, or a copied price/name snapshot as well. Existing bookings are not migrated by this work.

The supplied Barred Productionz Studios rate cards are transcribed in `barred-catalog-seed.json`. White Wedding and Traditional Wedding are separate categories; Bridal/Baby Shower is R4,900. The app includes a confirmation-gated **Load Barred rate card** action in photographer management. It writes 21 package and 19 add-on records under the entered photographer UID, using UID-scoped document IDs. Re-importing for the same UID replaces those catalog records. Use it only if these rates belong to that photographer. The JSON seed file is also kept for review.

## Manual verification checklist

1. In Android Studio, sync Gradle and run the app with the configured `google-services.json`.
2. Open **Photographer management**, enter the Firebase Auth UID for the photographer, and load the offerings.
3. If the supplied Barred rate cards belong to that photographer, choose **Load Barred rate card** and confirm. Check that the 21 packages and 19 add-ons appear in Firestore with that UID.
4. Open **Browse** and check that active services, packages and add-ons load from their respective collections.
5. Add one service with a price, duration, inclusions and add-on IDs; confirm the fields in Firestore.
6. Add a package linked to a service and add-on; confirm `serviceIds` and `addonIds` are lists of document IDs.
7. Edit the price/inclusions, then delete a test offering. Confirm each change in Firestore.
8. Check an inactive offering: it should remain visible in management and disappear from client browsing.
9. Verify Firestore rules reject writes when the signed-in UID does not own the offering. Do this in a test Firebase project or emulator before applying rule changes to the shared project.
