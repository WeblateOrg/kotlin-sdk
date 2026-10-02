# Copyright © Michal Čihař <michal@weblate.org>
#
# SPDX-License-Identifier: Apache-2.0

"""Validate, collect, and publish the SDK's release artifacts."""

import argparse
import json
import os
import re
import shutil
import subprocess
from pathlib import Path

import tomllib


def version():
    with Path("gradle/libs.versions.toml").open("rb") as catalog:
        result = tomllib.load(catalog)["versions"]["weblate"]
    if not re.fullmatch(
        r"\d+\.\d+\.\d+(?:-[A-Za-z0-9.-]+)?(?:\+[A-Za-z0-9.-]+)?", result
    ):
        raise ValueError(f"Invalid release version: {result}")
    return result


def validate():
    release_version = version()
    if os.environ["GITHUB_REF_TYPE"] != "tag":
        raise ValueError(
            "Select an existing tag when running the release workflow manually."
        )
    if os.environ["GITHUB_REF_NAME"] != release_version:
        raise ValueError(
            f"Release tag must match the Gradle version {release_version}."
        )
    return release_version


def asset_names(release_version, module):
    name, extension = (
        ("plugin-android", "jar") if module == "plugin" else ("android", "aar")
    )
    return [
        f"{name}-{release_version}.{extension}",
        f"{name}-{release_version}-sources.jar",
        f"{name}-{release_version}-javadoc.jar",
    ]


def stage(module):
    release_version = version()
    if module == "plugin":
        patterns = [
            f"plugin-android/build/libs/{name}"
            for name in asset_names(release_version, module)
        ]
    else:
        patterns = [
            "weblate-android/build/outputs/aar/weblate-android-release.aar",
            "weblate-android/build/intermediates/source_jar/release/release-sources.jar",
            "weblate-android/build/intermediates/java_doc_jar/release/release-javadoc.jar",
        ]
    sources = []
    for pattern in patterns:
        matches = list(Path().glob(pattern))
        if (
            len(matches) != 1
            or not matches[0].is_file()
            or not matches[0].stat().st_size
        ):
            raise ValueError(
                f"Expected one nonempty release artifact matching {pattern}"
            )
        sources.append(matches[0])
    Path("dist").mkdir(exist_ok=True)
    for source, name in zip(sources, asset_names(release_version, module), strict=True):
        shutil.copyfile(source, Path("dist") / name)


def gh(*args):
    return subprocess.check_output(["gh", *args], text=True).strip()


def publish():
    release_version = validate()
    repository = os.environ["GITHUB_REPOSITORY"]
    pages = json.loads(
        gh("api", "--paginate", "--slurp", f"repos/{repository}/releases")
    )
    existing = next(
        (
            release
            for page in pages
            for release in page
            if release["tag_name"] == release_version
        ),
        None,
    )
    if existing and not existing["draft"]:
        print(f"Release {release_version} is already published; leaving it unchanged.")
        return

    names = asset_names(release_version, "plugin") + asset_names(
        release_version, "library"
    )
    assets = [Path("dist") / name for name in names]
    for asset in assets:
        if not asset.is_file() or not asset.stat().st_size:
            raise ValueError(f"Missing or empty release asset: {asset}")
    # A draft's tag remains mutable: check it still points to the checkout being released.
    remote_commit = gh(
        "api", f"repos/{repository}/commits/{release_version}", "--jq", ".sha"
    )
    local_commit = subprocess.check_output(
        ["git", "rev-parse", "HEAD"], text=True
    ).strip()
    if remote_commit != local_commit:
        raise ValueError("Release tag no longer points to the checked-out commit.")

    prerelease = "-" in release_version.split("+", 1)[0]
    flags = ["--prerelease", "--latest=false"] if prerelease else []
    if not existing:
        gh(
            "release",
            "create",
            release_version,
            "--repo",
            repository,
            "--draft",
            "--verify-tag",
            "--generate-notes",
            *flags,
        )
    # Replacing files is allowed only while the release is a draft.
    gh(
        "release",
        "upload",
        release_version,
        "--repo",
        repository,
        "--clobber",
        *(str(asset) for asset in assets),
    )
    uploaded = json.loads(
        gh("release", "view", release_version, "--repo", repository, "--json", "assets")
    )["assets"]
    if {asset["name"]: asset["size"] for asset in uploaded} != {
        asset.name: asset.stat().st_size for asset in assets
    }:
        raise ValueError("Draft assets do not match the complete release artifact set.")
    gh(
        "release",
        "edit",
        release_version,
        "--repo",
        repository,
        "--draft=false",
        "--verify-tag",
        f"--prerelease={str(prerelease).lower()}",
        *(["--latest=false"] if prerelease else []),
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("validate", "stage", "publish"))
    parser.add_argument("module", nargs="?", choices=("plugin", "library"))
    args = parser.parse_args()
    if args.command == "validate":
        print(f"Validated release {validate()}")
    elif args.command == "stage":
        if not args.module:
            parser.error("stage requires a module")
        stage(args.module)
    else:
        publish()


if __name__ == "__main__":
    main()
