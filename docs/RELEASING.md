# Releasing KHelper

Three GitHub Actions workflows build, release and publish the plugin.

| Workflow | Starts | What it does |
|---|---|---|
| `build.yml` (Build) | Push to `main` or `develop`, every pull request | Tests, builds and verifies the plugin, and keeps the zip as a build artifact |
| `release.yml` (Release) | By hand, with a version like `2.0.0` | Tests `develop`, merges it into `main`, tags `v2.0.0` and creates a GitHub Release with the zip |
| `publish.yml` (Publish) | By hand, with a tag like `v2.0.0`; also when a person publishes a release by hand | Signs the plugin, publishes it to JetBrains Marketplace and attaches the signed zip to the release |

Publish is separate because GitHub does not let a release made by a workflow start another workflow. So after Release
finishes, start Publish yourself.

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

1. Merge the finished work into `develop`.
2. In GitHub, open **Actions > Release > Run workflow**, enter the version (for example `2.0.0`) and run it.
3. When it is green, `main` holds `develop`, the tag `v2.0.0` exists, and the GitHub Release has the zip and generated
   release notes. Edit the notes in GitHub if you like.
4. Open **Actions > Publish > Run workflow**, enter the tag `v2.0.0` and run it.
5. When it is green, the new version is on the Marketplace (after JetBrains approves it) and the signed zip is on the
   GitHub Release.

The version must look like `1.2.3`, and its tag must not exist yet. Otherwise the Release workflow stops before it
changes anything. A merge conflict between `develop` and `main` also stops it; then merge by hand and run it again.

If `main` is a protected branch that blocks direct pushes, the Release workflow cannot push the merge. Then allow GitHub
Actions to push to `main`, or merge `develop` into `main` with a pull request and create the release by hand.

## Local builds

Local builds do not need the secrets. Without `-PpluginVersion`, the version is `2.0.0-SNAPSHOT`.

```bash
./gradlew buildPlugin
```
