# Copyright © Michal Čihař <michal@weblate.org>
#
# SPDX-License-Identifier: Apache-2.0

"""Exercise release safety and recovery without publishing to GitHub."""

import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import release


class ReleaseTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        original = Path.cwd()
        self.addCleanup(os.chdir, original)
        os.chdir(temporary.name)
        Path("gradle").mkdir()
        self.set_version("1.0.0-alpha01")
        environment = patch.dict(
            os.environ,
            {
                "GITHUB_REF_TYPE": "tag",
                "GITHUB_REF_NAME": "1.0.0-alpha01",
                "GITHUB_REPOSITORY": "WeblateOrg/kotlin-sdk",
            },
        )
        environment.start()
        self.addCleanup(environment.stop)
        self.calls = []
        self.existing = []
        self.remote_commit = "abc123"
        self.upload_error = False
        self.incomplete_upload = False

    def set_version(self, value):
        Path("gradle/libs.versions.toml").write_text(
            f'[versions]\nweblate = "{value}"\n'
        )

    def make_assets(self):
        Path("dist").mkdir()
        for module in ("plugin", "library"):
            for name in release.asset_names(release.version(), module):
                (Path("dist") / name).write_bytes(b"built artifact")

    def fake_gh(self, *args):
        self.calls.append(args)
        if args[:4] == (
            "api",
            "--paginate",
            "--slurp",
            "repos/WeblateOrg/kotlin-sdk/releases",
        ):
            return json.dumps([self.existing])
        if args[0] == "api":
            return self.remote_commit
        if args[:2] == ("release", "view"):
            assets = [
                {"name": path.name, "size": path.stat().st_size}
                for path in Path("dist").iterdir()
            ]
            return json.dumps(
                {"assets": assets[:-1] if self.incomplete_upload else assets}
            )
        if args[:2] == ("release", "upload") and self.upload_error:
            raise subprocess.CalledProcessError(1, ["gh", *args])
        return ""

    def publish(self):
        with (
            patch.object(release, "gh", side_effect=self.fake_gh),
            patch.object(release.subprocess, "check_output", return_value="abc123\n"),
        ):
            release.publish()

    def commands(self, command):
        return [args for args in self.calls if args[:2] == ("release", command)]

    def test_validate_rejects_branch(self):
        os.environ["GITHUB_REF_TYPE"] = "branch"
        with self.assertRaisesRegex(ValueError, "existing tag"):
            release.validate()

    def test_validate_rejects_mismatched_version(self):
        os.environ["GITHUB_REF_NAME"] = "1.0.0"
        with self.assertRaisesRegex(ValueError, "match the Gradle version"):
            release.validate()

    def test_validate_rejects_unsafe_version(self):
        self.set_version("../../artifact")
        with self.assertRaisesRegex(ValueError, "Invalid release version"):
            release.validate()

    def test_stage_preserves_bytes_and_versions_library_names(self):
        sources = {
            "weblate-android/build/outputs/aar/weblate-android-release.aar": b"library",
            "weblate-android/build/intermediates/source_jar/release/release-sources.jar": b"sources",
            "weblate-android/build/intermediates/java_doc_jar/release/release-javadoc.jar": b"documentation",
        }
        for filename, content in sources.items():
            path = Path(filename)
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content)
        release.stage("library")
        for name, content in zip(
            release.asset_names(release.version(), "library"),
            sources.values(),
            strict=True,
        ):
            self.assertEqual((Path("dist") / name).read_bytes(), content)

    def test_stage_missing_file_does_not_create_partial_set(self):
        with self.assertRaisesRegex(ValueError, "Expected one nonempty"):
            release.stage("plugin")
        self.assertFalse(Path("dist").exists())

    def test_stage_plugin_preserves_all_three_published_files(self):
        names = release.asset_names(release.version(), "plugin")
        sources = Path("plugin-android/build/libs")
        sources.mkdir(parents=True)
        for name in names:
            (sources / name).write_bytes(name.encode())
        release.stage("plugin")
        for name in names:
            self.assertEqual(
                (Path("dist") / name).read_bytes(), (sources / name).read_bytes()
            )

    def test_prerelease_is_uploaded_and_checked_before_publication(self):
        self.make_assets()
        self.publish()
        self.assertIn("--generate-notes", self.commands("create")[0])
        self.assertIn("--draft", self.commands("create")[0])
        self.assertIn("--prerelease", self.commands("create")[0])
        self.assertIn("--latest=false", self.commands("edit")[0])
        self.assertLess(
            self.calls.index(self.commands("upload")[0]),
            self.calls.index(self.commands("view")[0]),
        )
        self.assertLess(
            self.calls.index(self.commands("view")[0]),
            self.calls.index(self.commands("edit")[0]),
        )

    def test_stable_release_including_build_metadata(self):
        self.set_version("1.0.0+build-1")
        os.environ["GITHUB_REF_NAME"] = "1.0.0+build-1"
        self.make_assets()
        self.publish()
        self.assertNotIn("--prerelease", self.commands("create")[0])
        self.assertIn("--prerelease=false", self.commands("edit")[0])
        self.assertNotIn("--latest=false", self.commands("edit")[0])

    def test_existing_draft_is_completed(self):
        self.existing = [{"tag_name": release.version(), "draft": True}]
        self.make_assets()
        self.publish()
        self.assertFalse(self.commands("create"))
        self.assertIn("--clobber", self.commands("upload")[0])
        self.assertIn("--draft=false", self.commands("edit")[0])

    def test_published_release_is_untouched_even_without_local_assets(self):
        self.existing = [{"tag_name": release.version(), "draft": False}]
        self.publish()
        self.assertEqual(len(self.calls), 1)

    def test_missing_asset_prevents_draft_creation(self):
        self.make_assets()
        next(Path("dist").iterdir()).unlink()
        with self.assertRaisesRegex(ValueError, "Missing or empty"):
            self.publish()
        self.assertFalse(self.commands("create"))

    def test_moved_tag_prevents_creation_and_upload(self):
        self.make_assets()
        self.remote_commit = "changed"
        with self.assertRaisesRegex(ValueError, "no longer points"):
            self.publish()
        self.assertFalse(self.commands("create"))
        self.assertFalse(self.commands("upload"))

    def test_upload_failure_leaves_draft_unpublished(self):
        self.make_assets()
        self.upload_error = True
        with self.assertRaises(subprocess.CalledProcessError):
            self.publish()
        self.assertTrue(self.commands("create"))
        self.assertFalse(self.commands("edit"))

    def test_incomplete_upload_prevents_publication(self):
        self.make_assets()
        self.incomplete_upload = True
        with self.assertRaisesRegex(ValueError, "complete release artifact set"):
            self.publish()
        self.assertFalse(self.commands("edit"))


if __name__ == "__main__":
    unittest.main()
