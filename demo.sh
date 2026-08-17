#!/usr/bin/env bash
#
# demo.sh — runs the Grant Fit Assessment EUC prototype end to end for a
# live demo: verifies LLM_API_KEY (via demo_setup.sh), builds and runs the
# offline test suite, then starts the web UI and prints a walkthrough
# checklist. See DEMO.md for the full narrative script.
#
# Usage: ./demo.sh
#
set -e
cd "$(dirname "$0")"

echo "== 1/3 Checking LLM_API_KEY =="
# shellcheck disable=SC1091
source demo_setup.sh

echo ""
echo "== 2/3 Building + running the offline test suite =="
mvn -q clean install
echo "Build OK."

echo ""
echo "== 3/3 Starting the web UI =="
PORT="${PORT:-8080}"
mvn -q exec:java -Dexec.mainClass="com.euc.grantfitassessment.web.WebServer" &
SERVER_PID=$!
trap 'echo; echo "Stopping demo server (pid $SERVER_PID)..."; kill "$SERVER_PID" 2>/dev/null' EXIT

echo "Waiting for the server to come up..."
for _ in $(seq 1 20); do
    if curl -sf -o /dev/null "http://localhost:$PORT/api/euc" 2>/dev/null; then
        break
    fi
    sleep 0.5
done

URL="http://localhost:$PORT"
echo "Web UI running at $URL"

if command -v open >/dev/null 2>&1; then
    open "$URL" 2>/dev/null || true
elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$URL" 2>/dev/null || true
fi

cat <<EOF

Demo checklist (see DEMO.md for the full script):
  1. Loaded EUC           -> goal/policies/pipeline read straight from the EUC JSON
  2. Try an Assessment     -> pick a sample case, run it, point out the eligibility halt
  3. Run Full Evaluation    -> scores all ground-truth cases against evaluationPipeline
  4. Run Drift Experiment    -> Week 5: baseline vs. prompt-variant reasoner

CLI equivalents (same code, no browser):
  mvn exec:java
  mvn exec:java -Dexec.mainClass="com.euc.grantfitassessment.eval.EvaluationRunner"
  mvn exec:java -Dexec.mainClass="com.euc.grantfitassessment.eval.DriftExperimentMain"

Press Ctrl+C to stop the server when done.
EOF

wait "$SERVER_PID"
