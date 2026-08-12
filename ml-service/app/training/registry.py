"""Versioned model registry.

Before this, the service had exactly one artifact (`models/pipeline.joblib`) with no
identity and no history: retraining overwrote the deployed model in place, with no way to
compare versions or roll back a bad one. The registry gives every trained model a version,
keeps its metrics and corpus manifest beside it, and tracks which version is active.

Layout::

    models/
      registry.json                 {"active": "v3", "versions": [...]}
      versions/
        v1/{pipeline.joblib,metrics.json,corpus_manifest.json}
        v2/...
"""
from __future__ import annotations

import json
import shutil
import threading
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional

MODELS_DIR = Path(__file__).resolve().parent.parent.parent / "models"
VERSIONS_DIR = MODELS_DIR / "versions"
REGISTRY_PATH = MODELS_DIR / "registry.json"

# Pre-registry artifact. Still read as a fallback so an existing deployment keeps working
# after an upgrade without being forced to retrain first.
LEGACY_PIPELINE_PATH = MODELS_DIR / "pipeline.joblib"
LEGACY_METRICS_PATH = MODELS_DIR / "metrics.json"

_lock = threading.RLock()


def _empty_registry() -> Dict:
    return {"active": None, "versions": []}


def load_registry() -> Dict:
    with _lock:
        if not REGISTRY_PATH.exists():
            return _empty_registry()
        try:
            return json.loads(REGISTRY_PATH.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError):
            return _empty_registry()


def _save_registry(registry: Dict) -> None:
    with _lock:
        MODELS_DIR.mkdir(parents=True, exist_ok=True)
        REGISTRY_PATH.write_text(json.dumps(registry, indent=2), encoding="utf-8")


def next_version() -> str:
    registry = load_registry()
    numbers = []
    for entry in registry.get("versions", []):
        raw = str(entry.get("version", ""))
        if raw.startswith("v") and raw[1:].isdigit():
            numbers.append(int(raw[1:]))
    return f"v{max(numbers, default=0) + 1}"


def version_dir(version: str) -> Path:
    return VERSIONS_DIR / version


def register(version: str, metrics: Dict, corpus_manifest: Dict, gate: Dict) -> Dict:
    """Record a freshly trained version. Registering never activates it — see `promote`."""
    with _lock:
        registry = load_registry()
        entry = {
            "version": version,
            "created_at": datetime.now(timezone.utc).isoformat(),
            "metrics": metrics,
            "corpus": corpus_manifest,
            "gate": gate,
            "promoted": False,
        }
        registry["versions"] = [
            item for item in registry.get("versions", []) if item.get("version") != version
        ] + [entry]
        _save_registry(registry)
        return entry


def get_version(version: str) -> Optional[Dict]:
    return next(
        (item for item in load_registry().get("versions", []) if item.get("version") == version),
        None,
    )


def list_versions() -> List[Dict]:
    versions = load_registry().get("versions", [])
    return sorted(versions, key=lambda item: item.get("created_at", ""), reverse=True)


def active_version() -> Optional[str]:
    return load_registry().get("active")


def active_entry() -> Optional[Dict]:
    version = active_version()
    return get_version(version) if version else None


def promote(version: str) -> Dict:
    """Make `version` the model that serves predictions."""
    with _lock:
        registry = load_registry()
        entry = next(
            (item for item in registry.get("versions", []) if item.get("version") == version),
            None,
        )
        if entry is None:
            raise ValueError(f"Unknown version: {version}")
        if not (version_dir(version) / "pipeline.joblib").exists():
            raise ValueError(f"Version {version} has no artifact on disk")

        registry["active"] = version
        for item in registry["versions"]:
            item["promoted"] = item.get("version") == version
        entry["promoted_at"] = datetime.now(timezone.utc).isoformat()
        _save_registry(registry)
        return entry


def rollback(version: str) -> Dict:
    """Alias of promote, named for the operation an admin is actually performing."""
    return promote(version)


def active_pipeline_path() -> Optional[Path]:
    """Artifact serving predictions right now, or the legacy file before any registration."""
    version = active_version()
    if version:
        candidate = version_dir(version) / "pipeline.joblib"
        if candidate.exists():
            return candidate
    return LEGACY_PIPELINE_PATH if LEGACY_PIPELINE_PATH.exists() else None


def active_metrics_path() -> Optional[Path]:
    version = active_version()
    if version:
        candidate = version_dir(version) / "metrics.json"
        if candidate.exists():
            return candidate
    return LEGACY_METRICS_PATH if LEGACY_METRICS_PATH.exists() else None


def write_artifacts(version: str, pipeline, metrics: Dict, corpus_manifest: Dict) -> Path:
    import joblib

    target = version_dir(version)
    target.mkdir(parents=True, exist_ok=True)
    joblib.dump(pipeline, target / "pipeline.joblib")
    (target / "metrics.json").write_text(json.dumps(metrics, indent=2), encoding="utf-8")
    (target / "corpus_manifest.json").write_text(
        json.dumps(corpus_manifest, indent=2), encoding="utf-8"
    )
    return target


def delete_version(version: str) -> None:
    """Remove a non-active version's artifacts. Refuses to delete the live model."""
    with _lock:
        if active_version() == version:
            raise ValueError("Cannot delete the active version")
        shutil.rmtree(version_dir(version), ignore_errors=True)
        registry = load_registry()
        registry["versions"] = [
            item for item in registry.get("versions", []) if item.get("version") != version
        ]
        _save_registry(registry)
