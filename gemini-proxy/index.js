const crypto = require('crypto');
const express = require('express');
const axios = require('axios');
const dotenv = require('dotenv');
const rateLimit = require('express-rate-limit');

dotenv.config();

const app = express();
app.use(express.json());

const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
// Shared secret embedded in the app's BuildConfig (see APP_SHARED_SECRET in
// build.gradle.kts) and sent as the X-App-Secret header. This is NOT a
// substitute for real attestation (Play Integrity / Firebase App Check) --
// a determined attacker can still extract it from the APK -- but it stops
// casual scraping and drive-by abuse of an otherwise fully open endpoint.
// Replace with Play Integrity verification before this proxy handles
// meaningful production traffic.
const APP_SHARED_SECRET = process.env.APP_SHARED_SECRET;

if (!APP_SHARED_SECRET) {
    console.error('ERROR: APP_SHARED_SECRET is not set. The proxy will reject all requests.');
}

// Coarse per-IP rate limit. Tune based on real free-tier usage (3 AI
// queries/day/user client-side) plus reasonable headroom for shared NATs.
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 60,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Too many requests. Please try again later.' }
});
app.use(limiter);

function requireAppSecret(req, res, next) {
    if (!APP_SHARED_SECRET) {
        // Fail closed when no secret is configured to prevent authentication bypass.
        return res.status(500).json({ error: 'Server configuration error: APP_SHARED_SECRET not set' });
    }
    const provided = req.header('X-App-Secret');
    const providedStr = typeof provided === 'string' ? provided : '';
    const expectedBuffer = Buffer.from(APP_SHARED_SECRET);
    const providedBuffer = Buffer.from(providedStr);

    let isMatch = true;
    if (providedBuffer.length !== expectedBuffer.length) {
        isMatch = false;
    }

    const compareBuffer = isMatch ? providedBuffer : expectedBuffer;

    if (!crypto.timingSafeEqual(compareBuffer, expectedBuffer) || !isMatch) {
        return res.status(401).json({ error: 'Unauthorized' });
    }
    next();
}

app.post('/v1beta/models/gemini-2.5-flash:generateContent', requireAppSecret, async (req, res) => {
    try {
        if (!GEMINI_API_KEY) {
            return res.status(500).json({ error: 'Server configuration error: API key not set' });
        }
        const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${GEMINI_API_KEY}`;
        const response = await axios.post(url, req.body, {
            headers: {
                'Content-Type': 'application/json'
            }
        });
        res.status(response.status).json(response.data);
    } catch (error) {
        console.error('Error forwarding request to Gemini:', error.message);
        if (error.response) {
            res.status(error.response.status).json(error.response.data);
        } else {
            res.status(500).json({ error: 'Internal Server Error' });
        }
    }
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
    console.log(`Gemini API Proxy listening on port ${PORT}`);
});
