# 🏆 Google Play Console: Subscription Setup Guide

This guide details how to configure the subscriptions in the Google Play Console for **FloraFlow PRO Monthly** and **FloraFlow PRO Annual**.

---

## 🔑 1. Product Definitions & Identifiers

Ensure the product IDs match the configurations defined in `com.example.billing.BillingManager`:

| Plan Name | Product ID (SKU) | Price | Billing Period | Free Trial |
| :--- | :--- | :--- | :--- | :--- |
| **FloraFlow PRO Monthly** | `floraflow_premium_monthly` | `$4.99` | Monthly | 7-Day |
| **FloraFlow PRO Annual** | `floraflow_premium_yearly` | `$39.99` | Yearly | 14-Day |

---

## ⚙️ 2. Step-by-Step Play Console Configuration

### Step A: Locate Subscription Setup
1. Log into your [Google Play Console](https://play.google.com/console/).
2. Select **FloraFlow** from the app list.
3. In the left-hand navigation, scroll to the **Monetize** section.
4. Click **Products** ➡️ **Subscriptions**.

### Step B: Create Subscriptions
For each subscription (Monthly and Yearly):
1. Click **Create Subscription** in the top right corner.
2. Fill in the **Product ID** (e.g. `floraflow_premium_monthly`).
3. Set the **Name** (e.g. `FloraFlow PRO Monthly`).
4. Click **Create**.

### Step C: Configure Base Plans & Pricing
Subscriptions in Google Play Console require at least one **Base Plan**:
1. Under the subscription details page, scroll to **Base plans and offers**.
2. Click **Add base plan**.
3. Set the **Base plan ID** (e.g. `monthly-base-plan` or `yearly-base-plan`).
4. Choose **Auto-renewing** as the type.
5. Set the **Billing period** (e.g. `Monthly` or `Yearly`).
6. Set the **Price**:
   - Monthly: Select your target countries, set the price to **$4.99** USD (Play Console automatically handles local tax & currency conversion rules).
   - Yearly: Set the price to **$39.99** USD.
7. Click **Save** ➡️ **Activate**.

### Step D: Add Free Trial Offers
To configure free trials, you must create an **Offer** linked to the Base Plan:
1. In the **Base plans and offers** list, locate your newly activated base plan.
2. Click **Add offer**.
3. Set the **Offer ID** (e.g. `7-day-trial` or `14-day-trial`).
4. Set the **Eligibility Criteria** to **New Customer Acquisition** (only users who haven't subscribed before qualify).
5. Under **Phases**, click **Add phase**:
   - Choose **Free trial** type.
   - Set the duration: `7 Days` for Monthly, `14 Days` for Yearly.
6. Click **Save** ➡️ **Activate**.

---

## 📋 3. App Metadata & Benefits Checklists

Copy and paste these localized titles, descriptions, and benefit bullets directly into the Play Store submission fields:

### Monthly Plan Metadata
* **Subscription Title**: `FloraFlow PRO Monthly`
* **Short Description**: `Try premium features: full Neural Restoration Journal access, brainwave soundscapes, and AI queries.`
* **Benefits Bullet Points**:
  - Full Neural Restoration Journal access
  - Eco-acoustic binaural brainwave soundscapes (Alpha, Theta, Delta)
  - 3 free Gemini AI expert queries per month
  - Companion-planting space layout analyzer

### Yearly Plan Metadata (Best Value)
* **Subscription Title**: `FloraFlow PRO Annual`
* **Short Description**: `Save over 33%! Unlocks unlimited Gemini-powered AI advice, full soundscapes, and advanced layouts.`
* **Benefits Bullet Points**:
  - Unlimited Gemini-powered AI master botanist advice
  - Full Eco-acoustic binaural soundscapes & nature chimes
  - Unlimited space layout grids & design snapshots
  - Comprehensive therapeutic mental wellness logs & mood correlation statistics
  - 14-day free trial

---

## 🛡️ 4. Recommended Subscription Settings
To minimize churn and maximize customer lifetime value, enable these features in **Subscription settings**:
* **Grace Period**: Set to **7 days** for weekly/monthly plans, and **14 days** for annual plans. This allows users time to resolve declined credit cards without immediate losing premium access.
* **Account Hold**: Enable account hold for up to **30 days** to allow users to update payment information.
* **Subscription Pause**: Allow users to temporarily pause their subscription for up to 3 months rather than cancel.
