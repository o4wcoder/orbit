# 🛰️ Orbit

**Orbit** is a personal news and newsletter aggregation app for Android.  
It brings newsletters, long-form articles, and curated content into a single, fast, modern reading experience — built with Jetpack Compose and Material 3.

The app is backed by an automated ingestion pipeline that parses newsletters and normalizes them into a unified feed.

---

## ✨ Features

- 📬 **Automated newsletter ingestion**
  - Newsletters are parsed and structured by a custom **n8n workflow** running on a **Hostinger VPS**
  - Designed to handle multiple sources and inconsistent newsletter formats
  - Produces clean, normalized article data for the app

- 📱 **Modern Android UI**
  - Built entirely with **Jetpack Compose**
  - Uses the **latest Material 3** components, theming, and layout patterns
  - Adaptive layouts for different screen sizes

- 📰 **Unified reading feed**
  - All content appears in a single, scrollable feed
  - Designed for fast scanning, saving, and deep reading
  - Clean separation between data ingestion and presentation

- ⚡ **Scalable architecture**
  - Backend ingestion is fully decoupled from the Android app
  - Easy to extend with new sources or processing steps
  - Designed with future growth in mind

---

## 🧱 Architecture Overview

### Backend
- **n8n** automation workflows
- Hosted on a **Hostinger VPS**
- Responsible for:
  - Fetching newsletter content
  - Parsing and normalizing article data
  - Preparing content for consumption by the app

### Android App
- **Kotlin**
- **Jetpack Compose**
- **Material 3**
- Fetches pre-processed article data from the backend
- Focuses purely on presentation, interaction, and user experience

---

## 🖼️ Screenshots & Images

> Screenshots, UI previews, and diagrams will be added here.

<!--
Example:
![Feed Screen](images/feed.png)
![Article Detail](images/article_detail.png)
-->

---

## 🚧 Project Status

Orbit is an active, evolving project focused on:
- Improving ingestion reliability
- Refining the reading experience
- Exploring smarter personalization over time

The app is currently optimized for personal use, with architecture choices that allow future scaling.

---

## 📄 License

This project is currently not licensed for redistribution.  
All rights reserved unless otherwise stated.
