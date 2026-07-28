# FloraFlow Product Specification

## Project Status
**Current Phase:** Production-Ready & Feature-Complete. The application is strictly tailored for **Indoor Houseplants & Indoor Biophilic Sanctuaries**, featuring native H.264 ambient Reels video generation, Plant Parent Personality Archetypes, Co-Care Duet AppWidgets, and camera-assisted AI Room Vibe Checks.

## Overview
FloraFlow is an advanced mobile application for the Android ecosystem that revolutionizes indoor biophilic sanctuary design and houseplant care. By merging immersive eco-acoustic binaural soundscapes, hardware-accelerated 15-second Reels video generation, Plant Parent Personality Archetypes, and a shared Co-Care Duet AppWidget with an AI assistant grounded in biophilic design neuroscience, FloraFlow transforms indoor room styling from static blueprints into an immersive, emotionally grounding creative experience.

## Core Value Proposition
FloraFlow makes professional biophilic indoor space design accessible to everyday houseplant lovers. Users answer a short Neural Load assessment and FloraFlow helps them envision, plan, and bring a restorative indoor plant sanctuary to life.

## Platform
- **Primary platform:** Android (mobile)
- **Target devices:** Android smartphones and tablets running Android 7.0+ (API 24+) supporting background audio services and hardware video encoding

## Core Features

### 1. 100% Indoor Biophilic Profile & Plant Parent Archetype System
- Scores user space across 10 biophilic categories (Nature Views, Living Plants, Window Light, Acoustic Calm, Natural Materials, Air & Ventilation, Organic Forms, Water Features, Sensory Richness, Seasonal Awareness).
- Calculates 5 Plant Parent Personality Archetypes: *Jungle Maximalist*, *Cactus Survivor*, *Serial Overwaterer*, *Cyberpunk Botanist*, *Sanctuary Master*.

### 2. 15-Second Ambient Reels Video Exporter
- On-device hardware-accelerated `MediaCodec` + `MediaMuxer` 1080x1920 30fps MP4 video generator; frames reach the encoder through an OpenGL ES input surface.
- Muxes an AAC track rendered offline from the shared generative soundscape engine, so the exported clip carries the user's real binaural frequency and ambient scene.
- Merges pulsing frequency soundwave visualizers with the archetype badge for instant sharing to TikTok & Instagram Reels.

### 3. Co-Care Duet Home Screen AppWidget & Shared Hub
- Jetpack Glance / AppWidgetProvider home screen widget showing shared blooming plant nodes, care streaks, and partner activity.

### 4. AI Room Vibe Check Screen
- Preset-based diagnostic screen covering window daylight intensity, indoor room foliage density, and air circulation balance, with a shareable story card.
- Scores derive from the chosen room preset. Photo-based analysis is not implemented; product and marketing copy must not describe this screen as camera-driven until it is.

### 5. Eco-Acoustics & Neural Restoration Journal
- Computes Neural Restoration Index (NRI) from indoor biophilic arrangements.
- Procedural binaural beats (Alpha 10Hz, Theta 6Hz, Gamma 40Hz) with layered ambient nature audio.

### 6. Interactive Indoor Sanctuary Planner
- 5x5 indoor grid canvas with light filters (*All Indoor*, *Bright Light*, *Low Light*) and custom indoor potting substrates.

### 7. Indoor Houseplant Library
- Catalog of 100% indoor houseplants (*Monstera*, *Pothos*, *Snake Plant*, *Peace Lily*, *ZZ Plant*, *Fiddle Leaf Fig*, *Calathea*, *Rubber Tree*, *Anthurium*, *Parlor Palm*, *Bonsai Ficus*).

## What FloraFlow Is Not
- Not an outdoor crop farming or agricultural tool (100% strictly indoor houseplants and biophilic sanctuaries)
- Not a professional landscape architecture platform
- Not a general social media feed app (shares via native Android share intents)
- Not a generic meditation app (audio and video generation are tied to the user's indoor sanctuary score)

## Technical Notes
- Requires device supporting background audio services, notifications, and hardware H.264 video encoding (`MediaCodec`)
- AI queries routed via Gemini API proxy service
- Real Play Billing integrated via Google Play Billing Library 9.0.0
