# Fastlane

Fastlane owns the Android release pipeline and the Play Store metadata in this directory.

## One-time local setup

1. Install Ruby 3.3 or newer with your preferred Ruby version manager.
2. Run `bundle install` from the repository root.
3. Save the Google Play service-account JSON outside the repository and export:

   ```sh
   export PLAY_SERVICE_ACCOUNT_JSON_PATH=/absolute/path/to/play-account.json
   ```

4. Export the existing Android signing variables:

   ```sh
   export ANDROID_RELEASE_KEYSTORE_PATH=/absolute/path/to/release.jks
   export ANDROID_RELEASE_STORE_PASSWORD=...
   export ANDROID_RELEASE_KEY_ALIAS=...
   export ANDROID_RELEASE_KEY_PASSWORD=...
   ```

The service account must have permission to manage releases for `com.scrolless.app`. Validate it with:

```sh
bundle exec fastlane run validate_play_store_json_key json_key:"$PLAY_SERVICE_ACCOUNT_JSON_PATH"
```

## Release notes

Google Play release notes live at:

```text
fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt
```

Before bumping the app version, add at least the `en-US` changelog for the new `versionCode`. The release lane uploads every locale that has a matching file.

## Lanes

```sh
bundle exec fastlane android test
bundle exec fastlane android release_status
bundle exec fastlane android quality
bundle exec fastlane android build_release
bundle exec fastlane android publish_internal
bundle exec fastlane android internal
bundle exec fastlane android metadata track:internal
bundle exec fastlane android promote to:production
bundle exec fastlane android publish_release
bundle exec fastlane android release
```

- `quality` runs formatting, all local tests, and both release-flavor lint tasks.
- `build_release` builds and validates the signed Play APK/AAB and OSS APK.
- `release_status` checks whether the GitHub release and both non-empty APK assets are complete. CI uses this instead of checking only whether a tag exists.
- `publish_internal` publishes already-built artifacts and localized notes. It skips the AAB when its version code is already on the selected Play track. Play display names are editable and are not used as bundle identity.
- `internal` runs `quality`, `build_release`, and `publish_internal` together.
- `metadata` uploads store text and changelogs without uploading a binary.
- `promote` promotes the existing Internal release to another track; it defaults to Production.
- `publish_release` publishes already-built artifacts to Internal, creates a GitHub release if needed, and uploads any missing APK assets. A retry preserves successfully uploaded assets and replaces only failed/empty ones.
- `release` runs the full local quality, build, Play, and GitHub release sequence.

## GitHub Actions deployment

The Release workflow waits for the Build workflow to pass on `main`, then publishes or repairs the current source-controlled release unless the GitHub release and both uploaded APK assets are already complete. A tag alone does not mark completion. A running Play deployment is never interrupted; if several newer releases become pending, GitHub keeps only the newest pending run.

For a normal release:

1. Add localized changelogs for the new `versionCode`.
2. Bump `versionCode` and `versionName` in `app/build.gradle.kts` in the same change.
3. Merge or push the change to `main`.
4. After Build succeeds, GitHub Actions builds signed artifacts, stores them for seven days, uploads the AAB and changelogs to Play Internal, and creates the GitHub release.

Do not create the version tag manually. Fastlane creates `v<versionName>` on the verified commit after the Play upload succeeds. Play uses the localized changelog files; GitHub generates its release notes from merged changes.

Use **Run workflow → Production** to promote the current Internal release to Production without rebuilding it. **Run workflow → Internal** manually runs the quality gate before building and publishing an incomplete release.

If a release fails after Play accepts the AAB or GitHub creates the tag, rerun the original failed workflow. It skips the already-uploaded Play version and repairs missing GitHub APKs. The checkout must match the existing tag's commit when repairing assets; publishing from a newer commit with the same version fails rather than attaching different binaries to the old release. GitHub API errors also fail the run instead of being mistaken for a missing release.

The workflow records deployments in GitHub's `internal` and `production` environments. To require confirmation before a Production promotion, add required reviewers to the `production` environment in **Repository settings → Environments**.

The existing repository secrets remain valid; no new secret names are required.
