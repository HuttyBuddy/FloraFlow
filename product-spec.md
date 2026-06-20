# FloraFlow Product Specification

## Overview

FloraFlow is an advanced mobile application for the Android ecosystem that revolutionizes residential and urban landscape planning. By merging Augmented Reality (AR) visualization with an AI assistant grounded in biophilic design neuroscience, FloraFlow transforms garden planning from static blueprints into an immersive, emotionally grounding creative experience.

## Core Value Proposition

FloraFlow makes professional-grade landscape design accessible to everyday home gardeners. Users don't need design training or horticultural expertise — they point their phone at a space and FloraFlow helps them envision, plan, and bring a garden to life.

## Platform

- **Primary platform:** Android (mobile)
- **Target devices:** Android smartphones and tablets with ARCore support

## Core Features

### 1. AR Garden Visualization
Users point their camera at any outdoor or indoor space to see a real-time, overlaid preview of how plants, garden beds, and landscape elements would look in that environment. AR rendering accounts for:
- Existing spatial dimensions and obstacles
- Lighting conditions and sun exposure
- Scale and proportion relative to surroundings

### 2. AI Garden Planning Assistant
An intelligent conversational assistant that guides users through the planning process. Rooted in biophilic design principles, the AI:
- Asks about the user's goals, aesthetic preferences, and lifestyle
- Recommends plants based on climate zone, soil type, sun/shade, and maintenance tolerance
- Suggests layouts that maximize both visual appeal and ecological benefit
- Explains *why* certain combinations work — educating as it recommends

### 3. Biophilic Design Intelligence
FloraFlow's recommendations are informed by biophilic design neuroscience — the study of how natural environments affect human wellbeing. The app:
- Prioritizes plant selections and arrangements that reduce stress and increase cognitive restoration
- Surfaces the emotional and psychological benefits of design choices to users
- Encourages sensory variety: texture, color, scent, and seasonal change

### 4. Interactive Garden Planning Canvas
Beyond AR, users can plan gardens in a 2D/3D interactive canvas:
- Drag-and-drop plant placement
- View seasonal transitions — see what the garden looks like in spring vs. autumn
- Save, iterate, and share plans

### 5. Plant Library
A comprehensive, searchable database of plants suited to residential and urban gardens, including:
- Care requirements (water, sun, soil, pruning)
- Growth habits and mature dimensions
- Companion planting compatibility
- Native/pollinator-friendly tags

## What FloraFlow Is Not

- Not a general-purpose plant identification tool (that's a secondary use case, not the core)
- Not a professional landscape architecture platform
- Not a social/community gardening app (no public feed or UGC focus)
- Not a plant delivery or e-commerce service (though integrations are possible roadmap items)

## Key Differentiators

| Feature | FloraFlow | Typical garden apps |
|---|---|---|
| AR visualization | Real-time, spatially aware | Photo filters or static overlays |
| AI recommendations | Biophilic design–grounded | Generic plant databases |
| Emotional framing | Wellbeing-first | Utility-first |
| Platform depth | Immersive planning experience | Basic care reminders |

## Technical Notes

- Requires ARCore-compatible Android device
- Camera and location permissions required for full functionality
- Plant recommendations use USDA Hardiness Zones and Köppen climate classification
- AI assistant uses on-device + cloud hybrid processing
