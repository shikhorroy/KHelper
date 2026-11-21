# Releasing KHelper

Two GitHub Actions workflows build and publish the plugin.

| Workflow | Runs on | What it does |
|---|---|---|
| `.github/workflows/build.yml` | Push to `main` or `develop`, every pull request | Tests, builds and verifies the plugin, and keeps the zip as a build artifact |
| `.github/workflows/release.yml` | A published GitHub Release | Tests, verifies, signs and publishes the plugin to JetBrains Marketplace, and attaches the zip to the release |

## One-time setup

### 1. Upload the first version by hand

JetBrains Marketplace needs the first version of a plugin uploaded by hand. Later versions can come from the release
workflow. If KHelper is already on the Marketplace, skip this step.

1. Build the zip with `./gradlew buildPlugin`. It is in `build/distributions/`.
2. Sign in at https://plugins.jetbrains.com, choose **Upload plugin**, and upload the zip.

### 2. Create a Marketplace token

1. On https://plugins.jetbrains.com, open your profile and then **My Tokens**.
2. Create a token. Copy it, because it is shown only once.

### 3. Create a signing certificate

Marketplace expects signed plugins. Create a private key and a certificate once, on your own machine:

```bash
openssl genpkey -aes-256-cbc -algorithm RSA -out private_encrypted.pem -pkeyopt rsa_keygen_bits:4096
openssl rsa -in private_encrypted.pem -out private.pem
openssl req -key private.pem -new -x509 -days 365 -out chain.crt
```

The first command asks for a password. Remember it. Keep `private.pem` and `chain.crt` private and out of Git. The
certificate is valid for 365 days; create a new one before it ends.

### 4. Add the repository secrets

In GitHub, open the repository, then **Settings > Secrets and variables > Actions**, and add these secrets:

| Secret | Value |
|---|---|
| `PUBLISH_TOKEN` | The Marketplace token from step 2 |
| `CERTIFICATE_CHAIN` | The whole content of `chain.crt` |
| `PRIVATE_KEY` | The whole content of `private.pem` |
| `PRIVATE_KEY_PASSWORD` | The password from step 3 |

## Release a new version

1. Merge the work into `main`.
2. In GitHub, open **Releases > Draft a new release**.
3. Create a tag like `v2.0.0` on `main`. The tag sets the plugin version: `v2.0.0` publishes version `2.0.0`.
4. Write the release notes and click **Publish release**.
5. Watch the **Release** workflow in the **Actions** tab. When it is green, the new version is on the Marketplace (after
   JetBrains approves it) and the signed zip is attached to the GitHub Release.

The tag must look like `v1.2.3`. Other tags stop the workflow before anything is published.

## Local builds

Local builds do not need the secrets. Without `-PpluginVersion`, the version is `2.0.0-SNAPSHOT`.

```bash
./gradlew buildPlugin
```
