# GitHub Workflow

## Purpose

This document defines the Git and GitHub workflow for TicketFlow.

The goal is to keep the project history clean, professional, and easy to review.

## Branch Strategy

Use one feature branch per phase.

Current branch:

- feature/project-planning

Future branches:

- feature/backend-foundation
- feature/event-management
- feature/reservation-transaction
- feature/transactional-outbox
- feature/kafka-outbox-publisher
- feature/payment-simulator
- feature/payment-result-consumer
- feature/retry-dlq
- feature/dashboard-api
- feature/frontend-dashboard
- feature/devops-compose
- feature/final-polish

## Commit Message Rules

Use clear commit messages.

Recommended prefixes:

- docs
- feat
- fix
- test
- refactor
- chore

Examples:

- docs: add initial architecture notes
- docs: define ticketflow mvp scope
- docs: add oop and solid guidelines
- docs: add clean code guidelines
- feat: add event management api
- feat: add transactional reservation flow
- feat: persist reservation created outbox event
- test: add reservation consistency tests
- refactor: standardize api error responses
- chore: improve docker compose setup

## Phase 0 Commits

Current Phase 0 commits:

- docs: add initial project overview and mvp scope
- docs: add architecture and engineering guidelines
- docs: define database api and kafka event flow
- docs: add devops testing github and interview notes

## Before Commit Checklist

Before each commit:

- check git status
- verify changed files
- check file contents
- make sure empty files are not accidentally committed
- use meaningful commit message

Useful commands:

git status
git diff
git log --oneline

## GitHub Repository Setup

Repository name:

- ticketflow

Recommended visibility:

- Public if this will be used as portfolio project
- Private if still experimental

When creating the repository on GitHub:

- do not add README
- do not add .gitignore
- do not add license

Reason:

These files already exist locally.

## Remote Setup

After creating the GitHub repository, connect local repository to remote:

git remote add origin https://github.com/kayahanguneri8/ticketflow.git

Then push the branch:

git push -u origin feature/project-planning

## Pull Request Strategy

Open a PR from:

- feature/project-planning

Into:

- main

PR title:

docs: complete phase 0 project planning

PR description should include:

- what was added
- why it was added
- files changed
- checklist
- review notes

## Phase 0 PR Description Draft

Summary:

- Added initial project overview and MVP scope.
- Added architecture documentation.
- Added OOP, SOLID, and Clean Code guidelines.
- Added database, API, and Kafka event flow design.
- Added Docker Compose planning notes.
- Added testing strategy.
- Added GitHub workflow documentation.
- Added interview notes for explaining Phase 0.

Checklist:

- README.md added
- MVP scope documented
- architecture documented
- database design documented
- API design documented
- Kafka event flow documented
- Docker Compose plan documented
- testing strategy documented
- GitHub workflow documented
- interview notes documented

## What To Review Before Merge

Review these files:

- README.md
- docs/architecture.md
- docs/mvp-scope.md
- docs/database-design.md
- docs/api-design.md
- docs/kafka-event-flow.md
- docs/docker-compose-plan.md
- docs/testing-strategy.md
- docs/github-workflow.md
- docs/interview-notes.md
- docs/clean-code-guidelines.md
- docs/oop-solid-notes.md

## Interview Explanation

In TicketFlow, I use a phase-based Git workflow. Each major development phase gets its own feature branch, commits are small and meaningful, and each phase is reviewed through a pull request. This makes the project easier to review and closer to a professional team workflow.
