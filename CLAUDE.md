# CLAUDE.md --- Orbit App (Claude Code Agent Guide)

## Project Overview

**Orbit** is a personalized article and newsletter aggregation platform.

The system ingests newsletters and long‑form content from multiple
sources, normalizes them into a unified article schema, stores them in a
backend datastore, and presents them in a mobile reading app.

Orbit is designed to evolve from a simple aggregator into a **personal
reading assistant** that learns user interests and surfaces the most
relevant content automatically.

The project currently consists of three major subsystems:

1.  Content ingestion pipeline
2.  Storage / API layer
3.  Android frontend application

Claude agents working in this repository should prioritize
**maintainability, data consistency, and incremental feature
development.**

------------------------------------------------------------------------

# High Level Architecture

## 1. Content Ingestion Pipeline

Purpose: Convert unstructured newsletter emails into structured article
data.

Pipeline flow:

Gmail → Automation workflow → HTML extraction → Article parsing →
Normalized JSON → Database

Primary responsibilities:

• Retrieve newsletter emails • Extract HTML body • Parse article titles,
authors, links, and metadata • Convert extracted content into a unified
schema • Store parsed articles in storage

Design requirements:

• Parsers must tolerate inconsistent HTML structures • Extractors should
fail gracefully • Data normalization must enforce schema validity •
Duplicate article detection should be supported

------------------------------------------------------------------------

# Storage Layer

Current implementation uses a simple datastore for rapid development.

Future migration path:

• PostgreSQL • MySQL • Firebase • Custom backend service

The storage layer is the **source of truth for all articles.**

Data must be:

• normalized • queryable • extendable • version tolerant

------------------------------------------------------------------------

# Android Application

The frontend is an Android application built using:

• Kotlin • Jetpack Compose

Primary responsibilities:

• Fetch article data from backend • Render article feed • Provide
filtering and sorting • Display article detail view • Cache content
locally

------------------------------------------------------------------------

# Core Data Model

All sources must normalize to a single article schema.

Example:

{ "source": "Medium", "title": "Optimizing Android Startup Performance",
"author": "Author Name", "link": "https://example.com/article",
"summary": "Short description", "published": "2025-01-15", "category":
"technology", "read_time": "5 min", "image": "https://image.url" }

Schema design rules:

• Fields must be stable across sources • Optional fields must remain
nullable • Schema changes must be backward compatible

------------------------------------------------------------------------

# Feed Model

Orbit uses a **unified chronological feed**.

All articles from all sources appear in one feed.

Users can refine the feed using filters:

• source • category • topic • unread status • time range

Feed requirements:

• infinite scroll support • fast rendering • stable sorting •
predictable pagination

------------------------------------------------------------------------

# Personalization Strategy

Orbit gradually learns user preferences using behavioral signals.

Signals include:

• articles opened • reading duration • saved articles • source
preferences • topic filtering behavior

These signals will later support:

• recommendation ranking • topic prioritization • personalized digests •
intelligent notifications

------------------------------------------------------------------------

# Development Roadmap

## Phase 1 --- Aggregation Foundation

• newsletter ingestion • article parsing • normalized storage • feed
display

## Phase 2 --- Feed UX Improvements

• pull‑to‑refresh • loading placeholders • infinite scrolling • article
detail screen • feed filters

## Phase 3 --- Source Expansion

Add ingestion for:

• Substack newsletters • tech newsletters • blog feeds • educational
sources

AI extraction may assist with inconsistent structures.

## Phase 4 --- Personalization

Introduce behavior‑driven ranking and recommendations.

Goals:

• relevance scoring • personalized feeds • daily digests • reading
insights

------------------------------------------------------------------------

# Design Principles

Orbit development follows these principles.

## Simplicity

The reading experience must remain clean and distraction‑free.

## Structured Data

All ingested content must conform to the unified schema.

## Source Agnostic

The system should ingest new content sources easily.

## Incremental Intelligence

Personalization should evolve gradually using behavior signals.

## Privacy Respect

User data should only be used to improve their experience.

------------------------------------------------------------------------

# Claude Agent Development Guidelines

Claude Code agents working in this repository should follow these rules.

## 1. Prefer Small Iterations

Implement features in small testable increments.

Avoid large architectural rewrites unless explicitly requested.

## 2. Preserve Data Contracts

Never change the article schema without updating:

• ingestion pipeline • storage layer • frontend data models

## 3. Avoid Hidden Logic

Prefer explicit transformation steps over implicit behavior.

## 4. Handle Parsing Failures

Parsing logic must tolerate malformed input.

## 5. Prioritize Readability

Code should be understandable by future contributors.

## 6. Maintain Backward Compatibility

Existing stored article data must remain valid.

## 7. Limit Comments

Do not add comments to describe changes that were made. The code should be able to explain what it does.
Only add comments if the action that was performed was a guess or needs a developers attention.

## 9. Compose UI

* Compose code should have very limited buisness logic. UI models that are supplied to the composables
  should be in there final form if possible before using it inside the composable.
* Use nullability on a field in the ui model to determine visiblity of a compsable. If a composable should
  not be showm, than it's data would be null. 

  Example:

  uiModel.title?.let {
    Text(text = it)
  }

------------------------------------------------------------------------

# Typical Claude Agent Tasks

Agents working on Orbit may be asked to:

### Ingestion Tasks

• create HTML parsers • normalize newsletter content • detect duplicate
articles • implement ingestion workflows

### Backend Tasks

• improve storage schema • implement APIs • support pagination

### Android Tasks

• implement feed UI • add filtering logic • improve caching • build
article detail views

------------------------------------------------------------------------

# Long Term Vision

Orbit should become a **personalized knowledge feed** that understands
each reader's interests and continuously improves the reading
experience.

The system evolves from:

simple aggregation → structured feeds → personalized reading assistant
