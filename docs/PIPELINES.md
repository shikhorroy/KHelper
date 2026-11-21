# Pipelines

KHelper has three GitHub Actions pipelines in `.github/workflows`.

| Pipeline | File | Starts | What it does |
|---|---|---|---|
| **Build** | `build.yml` | On its own: every pull request, every push to `main` or `develop`. Also by hand | Checks which files changed. When code or build files changed, it runs the tests, builds the plugin, runs the plugin verifier and keeps the zip. Docs-only changes skip these steps |
| **Release** | `release.yml` | By hand, with a version such as `2.0.0` | Tests `develop`, merges `develop` into `main`, tags `v2.0.0` and creates a GitHub Release with the zip and generated notes |
| **Publish** | `publish.yml` | By hand, with a tag such as `v2.0.0`. Also on its own when a person publishes a GitHub Release | Signs the plugin (when the signing secrets exist), publishes it to JetBrains Marketplace and attaches the zip to the GitHub Release |

## From a change to the Marketplace

| Step | You do | What runs |
|---|---|---|
| 1. Open a pull request into `develop` | Push your branch and open the pull request | **Build** runs on its own. Merge only when it is green (or skipped for docs-only changes) |
| 2. Merge into `develop` | Click **Merge** on the pull request. No pipeline does this step | **Build** runs on its own again, on `develop` |
| 3. Release | **Actions > Release > Run workflow**, enter the version, for example `2.0.0` | **Release** merges `develop` into `main`, tags `v2.0.0` and creates the GitHub Release |
| 4. Publish | **Actions > Publish > Run workflow**, enter the tag, for example `v2.0.0` | **Publish** sends the plugin to JetBrains Marketplace. The new version is live after JetBrains approves it |

Step 4 does not start on its own after step 3. GitHub does not let a release created by a workflow start another
workflow, so start Publish yourself.

## Good to know

- **Which code is published:** Publish checks out the tag, so it always publishes the tagged commit on `main`. The
  branch chosen under **Use workflow from** only decides which version of the workflow steps runs.
- **Run workflow button:** GitHub shows it only for workflows that exist on the default branch.
- **Versions:** the version must look like `1.2.3`, and its tag must not exist yet. Otherwise Release stops before it
  changes anything. A merge conflict between `develop` and `main` also stops it.
- **Protected `main`:** if `main` blocks direct pushes, Release cannot push the merge. Allow GitHub Actions to push to
  `main`, or merge by hand.
- **Secrets:** Build and Release need none. Publish needs the repository secret `PUBLISH_TOKEN`. The repository secrets
  `CERTIFICATE_CHAIN` and `PRIVATE_KEY` are optional; with them, Publish also signs the plugin with our own certificate.
