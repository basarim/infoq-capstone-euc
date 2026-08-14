#!/usr/bin/env bash
#
# demo_setup.sh — verifies LLM_API_KEY is available before running the
# Grant Fit Assessment demo (GrantFitApplication, EvaluationRunner,
# DriftExperimentMain). Never prints the key itself.
#
# Run this with `source` (not as a subprocess) so it can see and reuse
# whatever's already exported in your current shell:
#
#   source demo_setup.sh
#   . demo_setup.sh
#
# If LLM_API_KEY isn't already exported in this shell, it falls back to a
# local .env file (see .env.example) — .env is gitignored, so that's a
# safe place to keep it outside your shell profile if you prefer.

if [ -z "$LLM_API_KEY" ] && [ -f .env ]; then
    echo "LLM_API_KEY not set in this shell — loading from local .env"
    set -a
    # shellcheck disable=SC1091
    source .env
    set +a
fi

if [ -z "$LLM_API_KEY" ]; then
    echo "LLM_API_KEY is NOT set."
    echo "Set it in this shell before running the demo, e.g.:"
    echo "  export LLM_API_KEY=\"sk-ant-...\""
    echo "or put it in a local .env file (already gitignored) and re-run: source demo_setup.sh"
    return 1 2>/dev/null || exit 1
fi

export LLM_MODEL="${LLM_MODEL:-claude-sonnet-4-6}"

echo "LLM_API_KEY is set."
echo "LLM_MODEL=$LLM_MODEL"
echo ""
echo "Ready to run:"
echo "  mvn exec:java"
echo "  mvn exec:java -Dexec.mainClass=\"com.euc.grantfitassessment.eval.EvaluationRunner\""
echo "  mvn exec:java -Dexec.mainClass=\"com.euc.grantfitassessment.eval.DriftExperimentMain\""
