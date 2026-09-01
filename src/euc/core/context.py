"""The mutable shared state a pipeline run passes between stages."""

from __future__ import annotations

from typing import Any


class PipelineContext:
    def __init__(self) -> None:
        self._fields: dict[str, Any] = {}

    def put(self, field: str, value: Any) -> None:
        self._fields[field] = value

    def get(self, field: str) -> Any:
        if field not in self._fields or self._fields[field] is None:
            raise RuntimeError(
                f"Context field '{field}' was not set by any stage that ran before it was read"
            )
        return self._fields[field]

    def has(self, field: str) -> bool:
        return field in self._fields

    def __repr__(self) -> str:
        return f"PipelineContext{self._fields}"
