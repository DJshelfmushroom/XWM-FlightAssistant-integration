# Copilot Instructions for XWM-FlightAssistant-integration

## Repository Overview

This repository integrates **XWM** (X Window Manager) with a **Flight Assistant** system. The goal is to provide a seamless interface between the XWM environment and flight-assistant tooling, enabling flight-related data, controls, or overlays to be surfaced within the XWM desktop environment.

## Architecture

- The integration bridges the XWM windowing system with the Flight Assistant backend/API.
- Keep UI components and business logic clearly separated.
- Configuration and settings should be externalized (e.g., config files or environment variables) rather than hardcoded.

## Coding Conventions

- Follow the language-specific style guide for whatever language(s) are used in this project (e.g., PEP 8 for Python, Standard JS style for JavaScript/TypeScript).
- Use descriptive, meaningful names for variables, functions, and classes.
- Prefer small, focused functions with a single responsibility.
- Write self-documenting code; add comments only where the intent is non-obvious.
- Keep dependencies minimal and well-justified.

## Testing

- All new features and bug fixes should include appropriate tests.
- Run the full test suite before submitting a pull request.
- Tests should be deterministic and isolated (no shared mutable state between tests).

## Pull Requests

- Keep pull requests focused on a single concern.
- Provide a clear description of what the change does and why.
- Link any related issues in the PR description.
- Ensure CI passes before requesting a review.

## Getting Started

1. Clone the repository.
2. Install any required dependencies (see project-specific setup instructions as they are added).
3. Follow the README for environment setup details.
