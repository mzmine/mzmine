#!/usr/bin/env python3
"""Integration checks for the generated Community package provenance resource."""

from __future__ import annotations

import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import zipfile


ROOT = Path(__file__).resolve().parents[2]
GRADLE = ROOT / ("gradlew.bat" if __import__("os").name == "nt" else "gradlew")
SCRIPT = ROOT / "gradle/community-build-info.gradle"


def run(*args: str, cwd: Path) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(args, cwd=cwd, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            check=False)
    if result.returncode:
        raise RuntimeError(f"Command failed: {' '.join(args)}\n{result.stdout}")
    return result


def gradle(project: Path, task: str) -> None:
    run(str(GRADLE), "-p", str(project), "--no-daemon", "--console=plain", task, cwd=ROOT)


def fixture(root: Path, name: str, *, git: bool) -> Path:
    project = root / name
    project.mkdir()
    project.joinpath("settings.gradle").write_text("rootProject.name = 'community-build-info-fixture'\n")
    project.joinpath("build.gradle").write_text(
        "plugins { id 'java' }\n"
        "version = '99.1.2-test'\n"
        f"apply from: '{SCRIPT.as_posix()}'\n"
    )
    if git:
        project.joinpath(".gitignore").write_text(".gradle/\nbuild/\n")
        run("git", "init", "--quiet", cwd=project)
        run("git", "config", "user.name", "Community Build Info Test", cwd=project)
        run("git", "config", "user.email", "community-build-info@example.test", cwd=project)
        project.joinpath("tracked.txt").write_text("first\n")
        run("git", "add", ".", cwd=project)
        run("git", "commit", "--quiet", "-m", "initial", cwd=project)
    return project


def build_info(project: Path) -> dict:
    return json.loads((project / "build/generated/community-build-info/META-INF/mzmine/community-build.json")
                      .read_text(encoding="utf-8"))


def main() -> None:
    if shutil.which("git") is None:
        raise RuntimeError("git is required for Community build-info tests")
    with tempfile.TemporaryDirectory(prefix="community-build-info-") as temporary:
        root = Path(temporary)
        project = fixture(root, "git-project", git=True)

        gradle(project, "generateCommunityBuildInfo")
        first = build_info(project)
        expected_commit = run("git", "rev-parse", "HEAD", cwd=project).stdout.strip()
        assert first == {
            "contract_version": 1,
            "package_version": "99.1.2-test",
            "commit": expected_commit,
        }

        project.joinpath("untracked.txt").write_text("dirty\n")
        gradle(project, "generateCommunityBuildInfo")
        assert build_info(project) == first

        run("git", "add", "untracked.txt", cwd=project)
        run("git", "commit", "--quiet", "-m", "second", cwd=project)
        gradle(project, "generateCommunityBuildInfo")
        second = build_info(project)
        assert second["commit"] == run("git", "rev-parse", "HEAD", cwd=project).stdout.strip()
        assert second["commit"] != first["commit"]

        jar_project = fixture(root, "jar-project", git=True)
        gradle(jar_project, "jar")
        jar = next((jar_project / "build/libs").glob("*.jar"))
        with zipfile.ZipFile(jar) as archive:
            assert json.loads(archive.read("META-INF/mzmine/community-build.json")) == {
                "contract_version": 1,
                "package_version": "99.1.2-test",
                "commit": run("git", "rev-parse", "HEAD", cwd=jar_project).stdout.strip(),
            }

        without_git = fixture(root, "no-git-project", git=False)
        gradle(without_git, "generateCommunityBuildInfo")
        assert build_info(without_git) == {
            "contract_version": 1,
            "package_version": "99.1.2-test",
            "commit": None,
        }


if __name__ == "__main__":
    main()
