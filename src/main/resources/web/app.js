async function loadEuc() {
  const res = await fetch('/api/euc');
  const euc = await res.json();
  const el = document.getElementById('euc-summary-content');
  const stages = euc.executionPipeline
    .map(s => `${s.id} [${s.filter}] (${s.type}, onFailure=${s.onFailure})`)
    .join('<br>');
  const evalStages = euc.evaluationPipeline
    .map(s => `${s.id} [${s.filter}] evaluates ${s.evaluates.join(', ')}`)
    .join('<br>');
  el.innerHTML = `
    <p><strong>Goal:</strong> ${escapeHtml(euc.goal)}</p>
    <p><strong>Policies:</strong> ${euc.policies.map(escapeHtml).join('; ')}</p>
    <p><strong>Expected outcomes:</strong> ${euc.expectedOutcomes.map(escapeHtml).join(', ')}</p>
    <p><strong>Execution pipeline:</strong><br>${stages}</p>
    <p><strong>Evaluation pipeline:</strong><br>${evalStages}</p>
  `;
}

let dataset = [];
async function loadDataset() {
  const res = await fetch('/api/dataset');
  dataset = await res.json();
  const select = document.getElementById('sample-select');
  dataset.forEach((tc, i) => {
    const opt = document.createElement('option');
    opt.value = String(i);
    opt.textContent = `${tc.caseId} (expected: ${tc.expectedFitClassification})`;
    select.appendChild(opt);
  });
  select.addEventListener('change', () => {
    if (select.value === '') {
      return;
    }
    fillForm(dataset[Number(select.value)]);
  });
}

function fillForm(tc) {
  const form = document.getElementById('assess-form');
  form.elements['org.name'].value = tc.organization.name;
  form.elements['org.missionStatement'].value = tc.organization.missionStatement;
  form.elements['org.programs'].value = tc.organization.programs.join(', ');
  form.elements['org.operatingRegion'].value = tc.organization.operatingRegion;
  form.elements['org.isRegisteredNonprofit'].checked = tc.organization.isRegisteredNonprofit;
  form.elements['grant.funderName'].value = tc.grant.funderName;
  form.elements['grant.grantName'].value = tc.grant.grantName;
  form.elements['grant.fundingPriorities'].value = tc.grant.fundingPriorities.join(', ');
  form.elements['grant.eligibilityRequirements'].value = tc.grant.eligibilityRequirements.join(', ');
  form.elements['grant.allowedRegions'].value = tc.grant.allowedRegions.join(', ');
  form.elements['grant.requiresRegisteredNonprofit'].checked = tc.grant.requiresRegisteredNonprofit;
}

function splitList(value) {
  return value.split(',').map(s => s.trim()).filter(s => s.length > 0);
}

document.getElementById('assess-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.target;
  const resultEl = document.getElementById('assess-result');
  resultEl.innerHTML = '<p class="pending">Running…</p>';

  const body = {
    organization: {
      name: form.elements['org.name'].value,
      missionStatement: form.elements['org.missionStatement'].value,
      programs: splitList(form.elements['org.programs'].value),
      operatingRegion: form.elements['org.operatingRegion'].value,
      isRegisteredNonprofit: form.elements['org.isRegisteredNonprofit'].checked
    },
    grant: {
      funderName: form.elements['grant.funderName'].value,
      grantName: form.elements['grant.grantName'].value,
      fundingPriorities: splitList(form.elements['grant.fundingPriorities'].value),
      eligibilityRequirements: splitList(form.elements['grant.eligibilityRequirements'].value),
      allowedRegions: splitList(form.elements['grant.allowedRegions'].value),
      requiresRegisteredNonprofit: form.elements['grant.requiresRegisteredNonprofit'].checked
    }
  };

  try {
    const res = await fetch('/api/assess', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const data = await res.json();
    if (!res.ok) {
      resultEl.innerHTML = `<p class="error">Error: ${escapeHtml(data.error || res.statusText)}</p>`;
      return;
    }
    resultEl.innerHTML = renderAssessment(data);
  } catch (err) {
    resultEl.innerHTML = `<p class="error">Request failed: ${escapeHtml(String(err))}</p>`;
  }
});

function renderAssessment(result) {
  const cls = result.eligible ? 'eligible' : 'ineligible';
  const failedRules = result.failedEligibilityRules.length
    ? `<p><strong>Failed rules:</strong> ${result.failedEligibilityRules.map(escapeHtml).join('; ')}</p>`
    : '';
  const evidence = result.supportingEvidence.length
    ? `<p><strong>Supporting evidence:</strong></p><ul>${result.supportingEvidence.map(e => `<li>${escapeHtml(e)}</li>`).join('')}</ul>`
    : '';
  const uncertainty = result.identifiedUncertainty.length
    ? `<p><strong>Identified uncertainty:</strong></p><ul>${result.identifiedUncertainty.map(e => `<li>${escapeHtml(e)}</li>`).join('')}</ul>`
    : '';
  return `
    <div class="result-card ${cls}">
      <p><strong>Eligible:</strong> ${result.eligible ? 'Yes' : 'No'}</p>
      ${failedRules}
      <p><strong>Fit classification:</strong> <span class="badge">${escapeHtml(result.fitClassification)}</span></p>
      <p><strong>Explanation:</strong> ${escapeHtml(result.explanation)}</p>
      ${evidence}
      ${uncertainty}
    </div>
  `;
}

document.getElementById('evaluate-button').addEventListener('click', async () => {
  const resultEl = document.getElementById('evaluate-result');
  resultEl.innerHTML = '<p class="pending">Running full evaluation (one LLM call per eligible case)…</p>';
  try {
    const res = await fetch('/api/evaluate', { method: 'POST' });
    const data = await res.json();
    if (!res.ok) {
      resultEl.innerHTML = `<p class="error">Error: ${escapeHtml(data.error || res.statusText)}</p>`;
      return;
    }
    resultEl.innerHTML = renderEvaluation(data);
  } catch (err) {
    resultEl.innerHTML = `<p class="error">Request failed: ${escapeHtml(String(err))}</p>`;
  }
});

function renderEvaluation(data) {
  const rows = data.results.map(r => {
    if (r.error) {
      return `<tr><td>${escapeHtml(r.caseId)}</td><td colspan="4" class="error">${escapeHtml(r.error)}</td></tr>`;
    }
    return `<tr>
      <td>${escapeHtml(r.caseId)}</td>
      <td>${mark(r.eligibilityCorrectness)}</td>
      <td>${mark(r.programAlignment)}</td>
      <td>${mark(r.evidenceGrounding)}</td>
      <td>${mark(r.allPassed)}</td>
    </tr>`;
  }).join('');
  return `
    <p><strong>${data.passed} / ${data.total}</strong> test cases passed all criteria.</p>
    <table>
      <thead><tr><th>Case</th><th>Eligibility</th><th>Alignment</th><th>Grounding</th><th>All</th></tr></thead>
      <tbody>${rows}</tbody>
    </table>
  `;
}

document.getElementById('drift-button').addEventListener('click', async () => {
  const resultEl = document.getElementById('drift-result');
  resultEl.innerHTML = '<p class="pending">Running drift experiment (multiple LLM calls per variant)…</p>';
  try {
    const res = await fetch('/api/drift-experiment', { method: 'POST' });
    const data = await res.json();
    if (!res.ok) {
      resultEl.innerHTML = `<p class="error">Error: ${escapeHtml(data.error || res.statusText)}</p>`;
      return;
    }
    resultEl.innerHTML = renderDrift(data);
  } catch (err) {
    resultEl.innerHTML = `<p class="error">Request failed: ${escapeHtml(String(err))}</p>`;
  }
});

function pct(value) {
  return (value === null || value === undefined) ? 'n/a' : (value * 100).toFixed(1) + '%';
}

function renderDrift(data) {
  const rows = data.variants.map(v => `
    <tr>
      <td>${escapeHtml(v.label)}</td>
      <td>${v.expectedToAlterBehavior ? 'yes' : 'no'}</td>
      <td>${v.anyDriftFlagged ? 'yes' : 'no'}</td>
      <td>${v.deterministicRuleStable ? 'yes' : 'no'}</td>
      <td>${pct(v.evidenceGroundingRate)}</td>
    </tr>
  `).join('');
  return `
    <p><strong>Baseline:</strong> ${escapeHtml(data.baseline)}</p>
    <ul>
      <li>drift-detection-rate: ${pct(data.driftDetectionRate)}</li>
      <li>false-flag-rate: ${pct(data.falseFlagRate)}</li>
      <li>deterministic-rule-stability-rate: ${pct(data.deterministicRuleStabilityRate)}</li>
      <li>evidence-grounding-consistency-rate: ${pct(data.evidenceGroundingConsistencyRate)}</li>
    </ul>
    <table>
      <thead><tr><th>Variant</th><th>Expected to alter</th><th>Flagged</th><th>Rule stable</th><th>Grounding rate</th></tr></thead>
      <tbody>${rows}</tbody>
    </table>
  `;
}

function mark(bool) {
  return bool ? '✅' : '❌';
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

loadEuc();
loadDataset();
