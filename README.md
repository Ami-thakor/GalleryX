# GalleryX

An offline Android gallery app project inspired by the Samsung One UI 4 Gallery layout.

## Build APK with GitHub Actions (no Android Studio required)

1. Create a new **public or private** repository on GitHub named `GalleryX`.
2. Upload the contents of this folder (the `app`, `.github`, and Gradle files) to the repository root. Do not upload the outer `GalleryX` folder as an extra nested folder.
3. Open the repository's **Actions** tab. Allow GitHub Actions if prompted.
4. The workflow **Build GalleryX APK** runs after a push to `main`. You can also select it and tap **Run workflow**.
5. Open the completed workflow run, scroll to **Artifacts**, and download `GalleryX-debug-apk`.
6. Extract the downloaded artifact ZIP and install `app-debug.apk` on your Android phone. You may need to allow installation from your browser or file manager.

This workflow creates a debug APK for testing. It is not a Play Store release/signing setup.
