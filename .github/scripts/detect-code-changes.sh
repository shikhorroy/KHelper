#!/usr/bin/env bash
# Decides whether the build workflow must test and build: prints code=true or code=false to $GITHUB_OUTPUT.
#
# Input (environment):
#   EVENT  github.event_name (pull_request, push or workflow_dispatch)
#   BASE   commit to compare with: the PR base, or the commit before the push
#   HEAD   commit under test
#
# When in doubt (manual run, new branch, base not found) it answers true, so the build runs.
set -euo pipefail

#~ Files that change the plugin or its build. Anything else (README, docs, other workflows) skips the build.
CODE_PATHS='^(src/|gradle/|gradlew$|gradlew\.bat$|build\.gradle\.kts$|settings\.gradle\.kts$|gradle\.properties$|\.github/workflows/build\.yml$|\.github/scripts/detect-code-changes\.sh$)'

output="${GITHUB_OUTPUT:-/dev/stdout}"

answer() {
    echo "code=$1" >> "$output"
    echo "Build needed: $1 ($2)"
}

if [[ "${EVENT:-}" == "workflow_dispatch" ]]; then
    answer true "started by hand"
    exit 0
fi

if [[ -z "${BASE:-}" || "$BASE" =~ ^0+$ ]] || ! git cat-file -e "${BASE}^{commit}" 2>/dev/null; then
    answer true "no base commit to compare with"
    exit 0
fi

# Compare from the merge base, so commits that are only on the base branch do not count.
merge_base="$(git merge-base "$BASE" "$HEAD")"
changed="$(git diff --name-only "$merge_base" "$HEAD")"

echo "Changed files:"
echo "${changed:-  (none)}"

if grep -qE "$CODE_PATHS" <<< "$changed"; then
    answer true "code or build files changed"
else
    answer false "only files outside the code changed"
fi
