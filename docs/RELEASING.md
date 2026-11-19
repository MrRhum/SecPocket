# Publishing SecPocket on GitHub

This guide takes the local project to a public GitHub repository and publishes the APK as a release asset. Choose **Option A** if you want to avoid the command line, or **Option B/C** if you have Git or the GitHub CLI installed.

## What goes where

| Item | Destination |
|---|---|
| `app/`, `content/`, `docs/`, `build.ps1`, `README.md`, `CHANGELOG.md`, `.gitignore` | Repository |
| `dist/SecPocket-1.0.apk` | Release asset |
| `dist/SHA256SUMS.txt` | Release asset |
| `build/`, `dist/`, `keystore/` | Never commit |

A GitHub release is attached to a Git tag. The tag gives you the **Source code (zip)** and **Source code (tar.gz)** archives automatically, so you do not need to attach a separate source ZIP: the repository holds the code and the release holds the built APK.

> **Never commit `keystore/`.** It contains the signing key. Anyone with it can publish updates Android will accept as SecPocket. `.gitignore` already excludes it.

---

## Before you start

- A GitHub account.
- The signed APK at `dist/SecPocket-1.0.apk`.
- The checksum file at `dist/SHA256SUMS.txt` (already generated next to the APK).
- Optional: **Git for Windows** — `winget install --id Git.Git -e --source winget`
- Optional: **GitHub CLI** — `winget install --id GitHub.cli -e --source winget`

After installing Git or the GitHub CLI, close and reopen the terminal so `git` and `gh` are on `PATH`.

---

## 1. Create the repository

1. Open <https://github.com/new>.
2. Repository name: `SecPocket`.
3. Visibility: **Public** (or Private if you prefer).
4. Do **not** add a README, `.gitignore` or license — the project already has them.
5. Click **Create repository**.

Copy the repository URL; it looks like `https://github.com/<USER>/SecPocket.git`.

Optional repository metadata:

- **Description:** `Offline Android field library of 77 cybersecurity frameworks, with crown jewels and client playbooks.`
- **Topics:** `android`, `cybersecurity`, `security-frameworks`, `nist`, `iso-27001`, `owasp`, `offline`, `field-guide`

---

## 2A. Upload with the GitHub website (no Git)

1. On the empty repository page, click **uploading an existing file**.
2. Open the `SecPocket` folder in File Explorer.
3. Select these items:
   - `.gitignore`
   - `README.md`
   - `CHANGELOG.md`
   - `build.ps1`
   - the `app`, `content` and `docs` folders
4. **Do not select** `build`, `dist` or `keystore`.
5. Drag the selected items into the browser. Wait for the file list to finish loading.
6. Commit message: `Initial commit: SecPocket 1.0`.
7. Click **Commit changes**.

Windows hides dotfiles by default. In File Explorer, enable **View → Show → Hidden items** and **File name extensions** so `.gitignore` is visible. If it was missed, use **Add file → Create new file**, name it `.gitignore`, and paste the contents of the local `.gitignore`.

The website uploader is fine for this project (well under GitHub's limits), but it cannot exclude files for you. Double-check that `keystore/`, `dist/` and `build/` are absent from the repository.

---

## 2B. Upload with Git

```powershell
cd "C:\Users\romai\Documents\Default Project\SecPocket"

git init
git add .
git commit -m "Initial commit: SecPocket 1.0"
git branch -M main
git remote add origin https://github.com/<USER>/SecPocket.git
git push -u origin main
```

Git Credential Manager opens a browser the first time; sign in there. If you prefer a token, create a fine-grained personal access token with **Contents: Read and write** for the repository. Never paste a token into a chat or commit it.

Before the first commit, confirm the ignored paths:

```powershell
git status --short
```

The output must not list `build/`, `dist/` or `keystore/`.

---

## 2C. Upload with the GitHub CLI

```powershell
cd "C:\Users\romai\Documents\Default Project\SecPocket"

gh auth login
gh repo create SecPocket --public --source . --remote origin --push
```

`gh auth login` is interactive: choose **GitHub.com → HTTPS → Login with a web browser**, then enter the one-time code it shows.

---

## 3. Publish the release

### Website

1. Open the repository on GitHub.
2. Click **Releases → Draft a new release**.
3. **Choose a tag:** type `v1.0.0`, then select **Create new tag: v1.0.0 on publish**.
4. **Release title:** `SecPocket 1.0`.
5. **Describe this release:** paste the contents of [`docs/release-notes/v1.0.0.md`](release-notes/v1.0.0.md).
6. Attach the binaries:
   - `dist/SecPocket-1.0.apk`
   - `dist/SHA256SUMS.txt`
7. Click **Publish release**.

GitHub creates the tag at the current `main` commit and generates the source archives for that tag.

### GitHub CLI

```powershell
gh release create v1.0.0 `
  ".\dist\SecPocket-1.0.apk" `
  ".\dist\SHA256SUMS.txt" `
  --title "SecPocket 1.0" `
  --notes-file ".\docs\release-notes\v1.0.0.md" `
  --target main
```

If you already pushed the tag, omit `--target main`.

The release assets must keep their exact filenames. The README's direct download link is:

```
/releases/latest/download/SecPocket-1.0.apk
```

---

## 4. Verify the published release

1. Open the release page in a private browser window.
2. Confirm both assets download:
   - `SecPocket-1.0.apk`
   - `SHA256SUMS.txt`
3. Check the hash:
   ```powershell
   Get-FileHash .\SecPocket-1.0.apk -Algorithm SHA256
   ```
   It must be:
   ```
   983193EA1A415647E1263BF1FBCB2B6AD875EFD0DC85A5EE3A81ED1753761947
   ```
4. Verify the signature:
   ```powershell
   & "$env:LOCALAPPDATA\SecPocket\sdk\build-tools\35.0.0\apksigner.bat" `
     verify --verbose --print-certs .\SecPocket-1.0.apk
   ```
5. Install the downloaded file on a device.
6. Confirm the README renders: screenshots visible, Download link resolves, catalog table intact.

---

## 5. Publishing future versions

1. Bump `versionCode` and `versionName` in `app/src/main/AndroidManifest.xml`.
2. Update the output filename/version in `build.ps1`.
3. Update the download filename and hash in `README.md`.
4. Add a new section to `CHANGELOG.md` and a new file under `docs/release-notes/`.
5. Build and verify:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\build.ps1
   ```
6. Commit, tag and push:
   ```powershell
   git add .
   git commit -m "Release 1.1.0"
   git tag -a v1.1.0 -m "SecPocket 1.1.0"
   git push origin main --tags
   ```
7. Create the release with the new APK and `SHA256SUMS.txt`.

Keep the same keystore for every update. A different key means Android refuses to install over the existing app; users must uninstall and lose their saved briefs.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| GitHub upload page does not show a folder | Drag the folder itself, not only the files inside it. Use Chrome or Edge. |
| `.gitignore` is missing from the upload | Create it with **Add file → Create new file** and paste the local contents. |
| `git` is not recognized | Close and reopen the terminal after installing Git. |
| `gh` is not recognized | Close and reopen the terminal after installing GitHub CLI. |
| Git asks for a password | Use Git Credential Manager's browser sign-in or a personal access token; GitHub no longer accepts account passwords. |
| Release download link returns 404 | The asset must be named exactly `SecPocket-1.0.apk` and attached to the latest release. |
| Tag is missing after a push | Push tags explicitly: `git push origin --tags`. |
| APK installs beside the old app | The new build was signed with a different key. Use the original `keystore/release.keystore`. |

---

## Security checklist

- [ ] `keystore/` is not in the repository or any release asset.
- [ ] `SHA256SUMS.txt` matches the APK being uploaded.
- [ ] The APK was built from the committed source.
- [ ] The signing certificate fingerprint matches `86:35:17:5E:...:85:07:08`.
- [ ] A backup of `keystore/release.keystore` and its credentials exists in a password manager.
- [ ] No tokens, `local.properties` or machine-specific paths are committed.
