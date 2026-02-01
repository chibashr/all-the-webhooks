# Workflows

## Version and Release

Manually run to bump version, push, tag, build the JAR, and create a GitHub Release.

### Testing locally with act

[act](https://github.com/nektos/act) runs GitHub Actions locally using Docker.

1. **Install act** (requires Docker):
   - Windows: `choco install act` or download from [releases](https://github.com/nektos/act/releases)
   - macOS: `brew install act`
   - Or: `curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash`

2. **List workflows and jobs**:
   ```bash
   act -l
   ```

3. **Dry-run** (show what would run, no containers):
   ```bash
   act workflow_dispatch -n
   ```

4. **Run the Version and Release job** (use a test version; will try to push if remote is configured):
   ```bash
   act workflow_dispatch -e .github/workflows/event-dispatch.json -j version-tag-build-release
   ```

   To run without pushing/tagging (e.g. to validate checkout and build only), use a [custom event file](https://nektosact.com/usage/events/) or run specific steps manually.

5. **Event payload** for `workflow_dispatch` is in `.github/workflows/event-dispatch.json`. Edit the `version` input there before running.

**Note:** The Version and Release workflow uses `secrets.GITHUB_TOKEN` and pushes to the repo. Running it with act against a real repo will create commits and tags; use a test branch or fork.
