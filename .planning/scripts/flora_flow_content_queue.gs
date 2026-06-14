/**
 * 🌿 FloraFlow Content Pipeline — Sunday Planning Routine
 * Place this script inside Extensions > Apps Script in your Google Sheet
 * (can be the same spreadsheet as flora_flow_ai_automation.js, or a dedicated one).
 *
 * Functions:
 * 1. generateWeeklyContentBriefs() - Generates a batch of creative briefs via Gemini
 *    and appends them as QUEUED rows to the "Content Queue" sheet.
 * 2. onOpen() - Adds a "FloraFlow Content Pipeline" menu.
 *
 * The "Content Queue" sheet is the shared state with the local Monday Execution
 * routine (see .planning/scripts/flora_flow_execution_routine.js), which reads
 * QUEUED rows, shells out to the Higgsfield CLI, and writes back Status/Asset URL.
 * This script never calls Higgsfield directly — it only plans.
 */

// --- CONFIGURATION ---
const GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"; // Replace with your actual Gemini API Key
const GEMINI_MODEL = "gemini-1.5-flash";
const BRIEFS_PER_RUN = 7; // One week's worth by default

const CONTENT_QUEUE_HEADERS = [
  "Row ID", "Platform", "Objective Constraint", "Engine",
  "Prompt", "Reference Image", "Status", "Job ID", "Asset URL", "Error / Notes"
];

// Condensed routing matrix — mirrors the Smart Heuristic Router table in
// .claude/skills/floraflow-content-pipeline/SKILL.md. Keep in sync.
const ROUTING_MATRIX = [
  { platform: "TikTok", engine: "kling3_0", advantage: "Kinetic fluid stability & motion paths" },
  { platform: "Reels", engine: "seedance_2_0", advantage: "Audio-video syncing & talking avatars" },
  { platform: "Brand", engine: "sora2", advantage: "Absolute environmental physics & draping" },
  { platform: "Static", engine: "nano_banana_2", advantage: "Crisp typographic and label text rendering" }
];

// Condensed attention-vector framework — mirrors advertising-masterclass.md. Keep in sync.
const ATTENTION_VECTORS = [
  "Pattern Interrupt", "Curiosity Spike", "Stat Flash",
  "Before/After Proof", "Relatable Friction", "Social Proof / Community"
];

/**
 * Creates a custom menu inside your Google Sheet for easy access.
 */
function onOpen() {
  var ui = SpreadsheetApp.getUi();
  ui.createMenu('🌿 FloraFlow Content Pipeline')
      .addItem('🧠 Generate Weekly Content Briefs', 'generateWeeklyContentBriefs')
      .addToUi();
}

/**
 * 1. SUNDAY PLANNING ROUTINE
 * Generates BRIEFS_PER_RUN creative briefs via Gemini, anchored on the attention
 * vector framework and routing matrix, and appends them as QUEUED rows.
 */
function generateWeeklyContentBriefs() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var queueSheet = ss.getSheetByName("Content Queue") || createContentQueueSheet(ss);

  var briefs = callGeminiApiContentPlanner(BRIEFS_PER_RUN);
  if (!briefs || !briefs.length) {
    Logger.log("No briefs generated.");
    return;
  }

  var existingData = queueSheet.getDataRange().getValues();
  var nextId = existingData.length; // header row counts as 0

  briefs.forEach(function (brief) {
    nextId++;
    var rowId = "Q-" + ("000000" + nextId).slice(-6);

    queueSheet.appendRow([
      rowId,
      brief.platform || "",
      brief.objectiveConstraint || "",
      brief.engine || "",
      brief.prompt || "",
      brief.referenceImage || "",
      "QUEUED",
      "", // Job ID — populated by execution routine
      "", // Asset URL — populated by execution routine
      ""  // Error / Notes — populated only on NEEDS_REVIEW
    ]);

    // Keep the Status dropdown valid on the new row
    var statusCell = queueSheet.getRange(queueSheet.getLastRow(), 7);
    var rule = SpreadsheetApp.newDataValidation()
        .requireValueInList(['QUEUED', 'IN_PROGRESS', 'DONE', 'NEEDS_REVIEW'], true)
        .build();
    statusCell.setDataValidation(rule);
  });

  Logger.log("Appended " + briefs.length + " new QUEUED briefs to Content Queue.");
}

/**
 * Calls Gemini to generate `count` creative briefs as a strict JSON array.
 * Each brief: { platform, objectiveConstraint, engine, prompt, referenceImage }
 */
function callGeminiApiContentPlanner(count) {
  if (GEMINI_API_KEY === "YOUR_GEMINI_API_KEY_HERE") {
    Logger.log("Please set your GEMINI_API_KEY inside the script configuration.");
    return null;
  }

  var matrixDescription = ROUTING_MATRIX.map(function (m) {
    return "- " + m.platform + " -> engine '" + m.engine + "' (" + m.advantage + ")";
  }).join("\n");

  var vectorsDescription = ATTENTION_VECTORS.join(", ");

  var systemInstruction =
    "You are the content strategist for FloraFlow: Cultivating Calm through Mindful Gardening, a plant-care Android app. " +
    "You plan a weekly batch of short-form marketing video and static ad briefs for the FloraFlow content pipeline. " +
    "Every brief must map to exactly one platform from the routing matrix and use ONLY that platform's engine identifier. " +
    "You must return your response STRICTLY as a valid JSON array, with no markdown code fences.";

  var prompt =
    "Generate exactly " + count + " creative briefs for FloraFlow's content queue.\n\n" +
    "Routing matrix (platform -> engine):\n" + matrixDescription + "\n\n" +
    "Attention vector framework (pick one per brief): " + vectorsDescription + "\n\n" +
    "For each brief, return an object with these exact fields:\n" +
    "{\n" +
    "  \"platform\": one of \"TikTok\", \"Reels\", \"Brand\", \"Static\",\n" +
    "  \"engine\": the matrix engine identifier matching the platform exactly,\n" +
    "  \"objectiveConstraint\": short phrase describing the desired feel (e.g. \"kinetic, fast cuts\"),\n" +
    "  \"prompt\": a concrete, visual scene description suitable for a video/image generation model (no abstract marketing language),\n" +
    "  \"referenceImage\": \"\" (leave blank — filled in manually if a reference asset is needed)\n" +
    "}\n\n" +
    "Vary the attention vector and platform across the batch — do not repeat the same combination twice. " +
    "Return ONLY the raw JSON array.";

  var url = "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent?key=" + GEMINI_API_KEY;

  var payload = {
    "contents": [
      { "parts": [{ "text": prompt }] }
    ],
    "systemInstruction": {
      "parts": [{ "text": systemInstruction }]
    },
    "generationConfig": {
      "responseMimeType": "application/json",
      "temperature": 0.9
    }
  };

  var options = {
    "method": "post",
    "contentType": "application/json",
    "payload": JSON.stringify(payload),
    "muteHttpExceptions": true
  };

  try {
    var response = UrlFetchApp.fetch(url, options);
    var responseCode = response.getResponseCode();
    var responseText = response.getContentText();

    if (responseCode === 200) {
      var json = JSON.parse(responseText);
      var textResult = json.candidates[0].content.parts[0].text;
      return JSON.parse(textResult);
    } else {
      Logger.log("Gemini API Error (Code " + responseCode + "): " + responseText);
      return null;
    }
  } catch (e) {
    Logger.log("Exception calling Gemini: " + e.toString());
    return null;
  }
}

/**
 * Creates the Content Queue sheet with headers, formatting, and Status validation.
 */
function createContentQueueSheet(ss) {
  var sheet = ss.insertSheet("Content Queue");
  sheet.appendRow(CONTENT_QUEUE_HEADERS);
  sheet.getRange(1, 1, 1, CONTENT_QUEUE_HEADERS.length)
      .setFontWeight("bold")
      .setBackground("#E2EFE0")
      .setFontColor("#1E3A1E");
  sheet.setFrozenRows(1);
  return sheet;
}
