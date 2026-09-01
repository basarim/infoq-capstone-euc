"""Wraps Claude as DeepEval's judge model.

DeepEval defaults its LLM-as-judge metrics to OpenAI and auto-loads
OPENAI_API_KEY unless a custom model is supplied. This project only has
ANTHROPIC_API_KEY, so every GEval metric here must be given this model
explicitly rather than relying on DeepEval's default.
"""

from __future__ import annotations

import instructor
from anthropic import Anthropic
from deepeval.models import DeepEvalBaseLLM
from pydantic import BaseModel


class ClaudeJudgeModel(DeepEvalBaseLLM):
    def __init__(self, model_name: str) -> None:
        super().__init__(model_name)

    def load_model(self):
        return instructor.from_anthropic(Anthropic())

    def generate(self, prompt: str, schema: type[BaseModel]) -> BaseModel:
        return self.model.messages.create(
            model=self.get_model_name(),
            max_tokens=1024,
            messages=[{"role": "user", "content": prompt}],
            response_model=schema,
        )

    async def a_generate(self, prompt: str, schema: type[BaseModel]) -> BaseModel:
        return self.generate(prompt, schema)

    def get_model_name(self) -> str:
        return self.name
