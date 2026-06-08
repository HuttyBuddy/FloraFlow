/**
 * 🌿 FloraFlow AI-Powered Marketing CRM & HITL Automation Engine
 * Place this script inside Extensions > Apps Script in your Google Sheet CRM.
 * 
 * Functions:
 * 1. generateAiMarketingDrafts() - Runs daily to check for new users/feedback and calls Gemini to draft emails.
 * 2. deliverApprovedMarketingCampaigns() - Runs daily to send approved drafts and log them in Sent History.
 * 3. onOpen() - Creates a custom "FloraFlow AI CRM" menu inside Google Sheets for quick execution.
 */

// --- ⚙️ CONFIGURATION ---
const GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"; // Replace with your actual Gemini API Key
const GEMINI_MODEL = "gemini-1.5-flash"; // Fully covered under Gemini free tier
const EMAIL_SENDER_NAME = "FloraFlow Care Team";

/**
 * Creates a custom menu inside your Google Sheet for easy access.
 */
function onOpen() {
  var ui = SpreadsheetApp.getUi();
  ui.createMenu('🌿 FloraFlow AI CRM')
      .addItem('🤖 Generate AI Marketing Drafts', 'generateAiMarketingDrafts')
      .addItem('🚀 Dispatch Approved Campaigns', 'deliverApprovedMarketingCampaigns')
      .addToUi();
}

/**
 * 1. AI COPYWRITING ENGINE
 * Scans 'CRM Database' for entries, calls Gemini API to draft personalized, segment-specific emails,
 * and appends them to the 'AI Approval Queue' with a PENDING_REVIEW state.
 */
function generateAiMarketingDrafts() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  
  // Get sheets or create if missing
  var dbSheet = ss.getSheetByName("CRM Database") || createSheetHelper(ss, "CRM Database", ["Timestamp", "Email", "Name", "Last Action / Segment", "Rating", "Comments"]);
  var queueSheet = ss.getSheetByName("AI Approval Queue") || createSheetHelper(ss, "AI Approval Queue", ["Queue ID", "Email", "Name", "Segment", "Rating", "AI Draft Subject", "AI Draft Body (HTML)", "HITL Status", "Approver Notes", "Created Date", "Action Type"]);
  
  var dbData = dbSheet.getDataRange().getValues();
  if (dbData.length <= 1) {
    Logger.log("No users found in CRM Database.");
    return;
  }
  
  // Index existing queue entries to prevent drafting duplicate emails for the same user
  var queueData = queueSheet.getDataRange().getValues();
  var draftedEmails = {};
  for (var q = 1; q < queueData.length; q++) {
    var email = queueData[q][1];
    var actionType = queueData[q][10];
    if (email) {
      draftedEmails[email + "_" + actionType] = true;
    }
  }
  
  // Loop through CRM users starting from row 2 (skip header)
  for (var i = 1; i < dbData.length; i++) {
    var timestamp = dbData[i][0];
    var email = dbData[i][1];
    var name = dbData[i][2] || "Grower";
    var segment = dbData[i][3] || "General Comment";
    var rating = parseInt(dbData[i][4]) || 5;
    var comments = dbData[i][5] || "";
    
    if (!email || email.indexOf("@") === -1) continue;
    
    // Check if we already have an active/sent draft for this email of type EMAIL
    var lookupKey = email + "_EMAIL";
    if (draftedEmails[lookupKey]) {
      Logger.log("Draft already exists for " + email + ". Skipping.");
      continue;
    }
    
    Logger.log("Generating AI draft for: " + email + " (Rating: " + rating + ")");
    
    // Call Gemini to generate hyper-personalized subject & HTML body
    var aiResult = callGeminiApiCopywriter(name, segment, rating, comments);
    
    if (aiResult && aiResult.subject && aiResult.htmlBody) {
      var queueId = "Q-" + Math.floor(Math.random() * 900000 + 100000);
      
      // Append row to AI Approval Queue
      queueSheet.appendRow([
        queueId,
        email,
        name,
        segment,
        rating,
        aiResult.subject,
        aiResult.htmlBody,
        "PENDING_REVIEW", // Strict HITL approval gate
        "", // Approver Notes placeholder
        new Date(),
        "EMAIL"
      ]);
      
      // Set cell data validation for the HITL Status column (Column 8) so the user gets a neat dropdown
      var cell = queueSheet.getRange(queueSheet.getLastRow(), 8);
      var rule = SpreadsheetApp.newDataValidation().requireValueInList(['PENDING_REVIEW', 'APPROVED', 'REJECTED'], true).build();
      cell.setDataValidation(rule);
      
      Logger.log("Draft created successfully for " + email);
    }
  }
}

/**
 * Calls Google Gemini Developer API to create a personalized copywriting payload.
 */
function callGeminiApiCopywriter(name, segment, rating, comments) {
  if (GEMINI_API_KEY === "YOUR_GEMINI_API_KEY_HERE") {
    Logger.log("Please set your GEMINI_API_KEY inside the script configuration.");
    return null;
  }
  
  // Tailor prompt context based on ratings (1-2 = Apology/Rescue, 3-4 = Nurture/Science, 5 = Affiliate/Promo)
  var toneDirective = "";
  var offerDirective = "";
  
  if (rating <= 2) {
    toneDirective = "sincere, highly empathetic, deeply apologetic, and comforting";
    offerDirective = "Apologize directly for the friction they faced ('" + comments + "'). To make things right, offer them an exclusive 1-on-1 garden layout consultation with our design specialist or a 1-month extension of their trial period completely free. Highlight that their peace is our highest priority.";
  } else if (rating <= 4) {
    toneDirective = "warm, educational, scientific, and encouraging";
    offerDirective = "Discuss the 'Companion Planting Spacing Mistake' (planting competing root crops stunting yield). Invite them to open FloraFlow's 2D grid planner to check automatic spacing companion alerts. Highlight how scientific garden layouts elevate garden success.";
  } else {
    toneDirective = "celebratory, joyful, vibrant, and highly premium";
    offerDirective = "Congratulate them on thriving! Invite them to join our FloraFlow Ambassador Circle. Provide them with an exclusive 30% lifetime discount on our PRO annual subscription (coupon code: THRIVE30) and ask them to share their AR garden weather screenshots on Pinterest or Instagram to inspire others.";
  }
  
  var systemInstruction = 
    "You are the head of customer experience and content marketing at FloraFlow: Dream Garden Designer. " +
    "Your voice is highly premium, warm, sophisticated, and deeply botanical. You avoid typical spammy sales language. " +
    "You write high-conversion emails in clean HTML. You must return your response STRICTLY as a valid JSON object " +
    "with exactly two fields: 'subject' (plain text string) and 'htmlBody' (complete, gorgeous responsive HTML email styled with modern dark botanical inline CSS, using #1E3A1E greens, #F4A261 sunset colors, and soft ivory backgrounds).";
    
  var prompt = 
    "Create a personalized marketing email for: \n" +
    "- User Name: " + name + "\n" +
    "- Segment Category: " + segment + "\n" +
    "- Experiential Rating: " + rating + "/5\n" +
    "- User Input/Feedback: \"" + comments + "\"\n\n" +
    "Requirements:\n" +
    "1. TONE: The tone must be " + toneDirective + ".\n" +
    "2. THEME & OFFER: " + offerDirective + "\n" +
    "3. FORMAT: Return STRICTLY a raw JSON object with no markdown codeblock wraps (no ```json). Format: {\"subject\": \"string\", \"htmlBody\": \"styled HTML\"}. Ensure all quotes in HTML are correctly escaped.";

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
      "temperature": 0.7
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
      
      // Parse the inner JSON generated by Gemini
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
 * 2. HITL DISPATCH ENGINE
 * Scans 'AI Approval Queue' for rows marked as 'APPROVED'. Sends them via Gmail and archives
 * the entries to 'Sent History' to keep the active queue clean and focused.
 */
function deliverApprovedMarketingCampaigns() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var queueSheet = ss.getSheetByName("AI Approval Queue");
  var historySheet = ss.getSheetByName("Sent History") || createSheetHelper(ss, "Sent History", ["Queue ID", "Email", "Name", "Subject", "Sent Date", "Action Type"]);
  
  if (!queueSheet) {
    Logger.log("No AI Approval Queue sheet exists.");
    return;
  }
  
  var data = queueSheet.getDataRange().getValues();
  if (data.length <= 1) {
    Logger.log("Queue is empty.");
    return;
  }
  
  // Process rows backwards to safely handle deletion/move operations during iteration
  var processedCount = 0;
  for (var i = data.length - 1; i >= 1; i--) {
    var queueId = data[i][0];
    var email = data[i][1];
    var name = data[i][2];
    var subject = data[i][5];
    var htmlBody = data[i][6];
    var hitlStatus = data[i][7];
    var actionType = data[i][10] || "EMAIL";
    
    if (hitlStatus === "APPROVED") {
      if (actionType === "EMAIL") {
        try {
          // Fire email using free Google/Gmail quotas
          MailApp.sendEmail({
            to: email,
            subject: subject,
            htmlBody: htmlBody,
            name: EMAIL_SENDER_NAME
          });
          
          Logger.log("Dispatched email successfully to " + email);
          
          // Archive in Sent History
          historySheet.appendRow([
            queueId,
            email,
            name,
            subject,
            new Date(),
            "EMAIL"
          ]);
          
          // Remove from active approval queue to keep it clean
          queueSheet.deleteRow(i + 1);
          processedCount++;
          
        } catch (mailErr) {
          Logger.log("Failed to send email to " + email + ": " + mailErr.toString());
          // Update status cell to flag error
          queueSheet.getRange(i + 1, 8).setValue("PENDING_REVIEW");
          queueSheet.getRange(i + 1, 9).setValue("Delivery Error: " + mailErr.toString());
        }
      }
    } else if (hitlStatus === "REJECTED") {
      // Quietly clean up rejected campaigns from the board
      Logger.log("Removing rejected draft " + queueId + " for " + email);
      queueSheet.deleteRow(i + 1);
    }
  }
  
  Logger.log("Processed and dispatched " + processedCount + " approved marketing campaigns.");
}

/**
 * Dynamic sheet creation helper to guarantee proper columns and keys exist.
 */
function createSheetHelper(ss, name, headers) {
  var sheet = ss.insertSheet(name);
  sheet.appendRow(headers);
  sheet.getRange(1, 1, 1, headers.length).setFontWeight("bold").setBackground("#E2EFE0").setFontColor("#1E3A1E");
  sheet.setFrozenRows(1);
  return sheet;
}
