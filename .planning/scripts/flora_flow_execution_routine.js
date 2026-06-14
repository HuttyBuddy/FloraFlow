#!/usr/bin/env node
/**
 * FloraFlow Content Pipeline — Monday Execution Routine
 *
 * Reads QUEUED rows from the "Content Queue" Google Sheet (populated by the
 * Sunday Planning routine in flora_flow_content_queue.gs), maps each row to a
 * Higgsfield CLI invocation via the Smart Heuristic Router, and writes results
 * back to the sheet. See .claude/skills/floraflow-content-pipeline/SKILL.md
 * for the full protocol and the Hard Rules this script implements.
 *
 * ---------------------------------------------------------------------------
 * ONE-TIME SETUP
 * ---------------------------------------------------------------------------
 * 1. In Google Cloud Console, create a Service Account and enable the
 *    "Google Sheets API" for its project.
 * 2. Create a JSON key for the service account and save it locally, e.g.
 *    .planning/scripts/service-account.json (this path is gitignored — do
 *    NOT commit credentials).
 * 3. Open your Content Queue spreadsheet and "Share" it with the service
 *    account's email address (found in the JSON key as `client_email`),
 *    giving it Editor access.
 * 4. Install dependencies: `npm install` (from .planning/scripts).
 * 5. Set environment variables before running:
 *      GOOGLE_APPLICATION_CREDENTIALS=./service-account.json
 *      FLORAFLOW_SHEET_ID=<spreadsheet ID from its URL>
 * 6. Ensure the `higgsfield` CLI is installed and authenticated separately.
 * 7. Run: `npm run execute-queue`
 * ---------------------------------------------------------------------------
 */

const { google } = require('googleapis');
const { execFileSync } = require('child_process');

const SHEET_NAME = 'Content Queue';
const SHEET_ID = process.env.FLORAFLOW_SHEET_ID;

// Column order must match CONTENT_QUEUE_HEADERS in flora_flow_content_queue.gs
const COLUMNS = [
  'rowId', 'platform', 'objectiveConstraint', 'engine',
  'prompt', 'referenceImage', 'status', 'jobId', 'assetUrl', 'errorNotes'
];
const STATUS_COL = COLUMNS.indexOf('status'); // 0-indexed
const JOB_ID_COL = COLUMNS.indexOf('jobId');
const ASSET_URL_COL = COLUMNS.indexOf('assetUrl');
const ERROR_COL = COLUMNS.indexOf('errorNotes');

async function getSheetsClient() {
  const auth = new google.auth.GoogleAuth({
    scopes: ['https://www.googleapis.com/auth/spreadsheets'],
  });
  const client = await auth.getClient();
  return google.sheets({ version: 'v4', auth: client });
}

async function readQueueRows(sheets) {
  const res = await sheets.spreadsheets.values.get({
    spreadsheetId: SHEET_ID,
    range: `${SHEET_NAME}!A2:J`,
  });
  return res.data.values || [];
}

async function writeRowCell(sheets, rowIndex, colIndex, value) {
  // rowIndex is 0-based into the data (excluding header), so sheet row = rowIndex + 2
  const sheetRow = rowIndex + 2;
  const colLetter = String.fromCharCode('A'.charCodeAt(0) + colIndex);
  await sheets.spreadsheets.values.update({
    spreadsheetId: SHEET_ID,
    range: `${SHEET_NAME}!${colLetter}${sheetRow}`,
    valueInputOption: 'RAW',
    requestBody: { values: [[value]] },
  });
}

/**
 * Runs the Higgsfield CLI for a single queue row and returns { assetUrl, jobId }
 * or throws an Error whose `.status` is 'failed' | 'nsfw' | 'connection_dropped'.
 */
function runHiggsfieldForRow(row) {
  const [, platform, , engine, prompt, referenceImage] = row;

  const args = ['generate', 'create', '--model', engine, '--wait', '--json'];

  // Reference Drift Barrier (Hard Rule 1): always attach the image directly
  // and pin the literal fidelity constraint when a reference image is present.
  let finalPrompt = prompt;
  if (referenceImage) {
    args.push('--image', referenceImage);
    finalPrompt =
      `${prompt} The app UI and hardware configuration must appear EXACTLY as ` +
      `shown in this reference image. Same dimensions, exact branding text, ` +
      `zero structural variations or hallucinated placeholder blocks.`;
  }

  args.push('--prompt', finalPrompt);

  let stdout;
  try {
    stdout = execFileSync('higgsfield', args, { encoding: 'utf-8' });
  } catch (err) {
    // Distinguish a dropped connection (CLI process killed/timed out) from a
    // clean error response on stdout.
    if (err.stdout) {
      stdout = err.stdout.toString();
    } else {
      const dropped = new Error('Connection dropped before completion');
      dropped.status = 'connection_dropped';
      throw dropped;
    }
  }

  const result = JSON.parse(stdout);

  if (result.status === 'failed' || result.status === 'nsfw') {
    const flagged = new Error(result.error || `Generation status: ${result.status}`);
    flagged.status = result.status;
    flagged.jobId = result.job_set_id;
    throw flagged;
  }

  return { assetUrl: result.asset_url, jobId: result.job_set_id };
}

/**
 * Hard Rule 2 — Async Interruption Vault.
 * On a dropped connection, list in-flight jobs and find the one matching this
 * row's previously recorded jobId (or, if none recorded yet, the most recent
 * job whose prompt matches), then poll it to completion instead of resubmitting.
 */
function resumeViaJobList(existingJobId) {
  const stdout = execFileSync('higgsfield', ['generate', 'list', '--json'], { encoding: 'utf-8' });
  const jobs = JSON.parse(stdout);

  const match = existingJobId
    ? jobs.find((j) => j.job_set_id === existingJobId)
    : jobs.find((j) => j.status === 'in_progress' || j.status === 'queued');

  if (!match) return null;

  if (match.status === 'completed') {
    return { assetUrl: match.asset_url, jobId: match.job_set_id };
  }

  // Still in progress server-side — caller should re-check on the next run.
  return { jobId: match.job_set_id, inProgress: true };
}

async function main() {
  if (!SHEET_ID) {
    console.error('FLORAFLOW_SHEET_ID is not set.');
    process.exit(1);
  }

  const sheets = await getSheetsClient();
  const rows = await readQueueRows(sheets);

  for (let i = 0; i < rows.length; i++) {
    const row = rows[i];
    const status = row[STATUS_COL];

    if (status !== 'QUEUED') continue;

    await writeRowCell(sheets, i, STATUS_COL, 'IN_PROGRESS');

    try {
      const { assetUrl, jobId } = runHiggsfieldForRow(row);
      await writeRowCell(sheets, i, ASSET_URL_COL, assetUrl);
      await writeRowCell(sheets, i, JOB_ID_COL, jobId || '');
      await writeRowCell(sheets, i, STATUS_COL, 'DONE');
      console.log(`Row ${row[0]}: DONE (${assetUrl})`);
    } catch (err) {
      if (err.status === 'connection_dropped') {
        // Hard Rule 2: try to resume from the server-side job list rather than resubmitting.
        const resumed = resumeViaJobList(row[JOB_ID_COL]);
        if (resumed && resumed.assetUrl) {
          await writeRowCell(sheets, i, ASSET_URL_COL, resumed.assetUrl);
          await writeRowCell(sheets, i, JOB_ID_COL, resumed.jobId || '');
          await writeRowCell(sheets, i, STATUS_COL, 'DONE');
          console.log(`Row ${row[0]}: DONE after resume (${resumed.assetUrl})`);
        } else {
          // Leave as IN_PROGRESS with the jobId recorded so the next run can resume.
          if (resumed && resumed.jobId) {
            await writeRowCell(sheets, i, JOB_ID_COL, resumed.jobId);
          }
          console.log(`Row ${row[0]}: still in progress server-side, will resume next run.`);
        }
        continue;
      }

      // Hard Rule 3: moderation/other failures are flagged for human review,
      // never auto-reworded or auto-resubmitted. The batch continues.
      await writeRowCell(sheets, i, STATUS_COL, 'NEEDS_REVIEW');
      await writeRowCell(sheets, i, ERROR_COL, `[${err.status || 'error'}] ${err.message}`);
      console.warn(`Row ${row[0]}: NEEDS_REVIEW (${err.status || 'error'}) - ${err.message}`);
    }
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
