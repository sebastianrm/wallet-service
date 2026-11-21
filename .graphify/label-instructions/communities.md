# Community Labeling

Graphify is running in assistant/skill mode (no API key). You are the host
assistant (Claude Code / Codex / Gemini CLI). Read the community listing below
and write 2-5 word plain-language names for each.

## Language

Write every name in English (en). Do not switch languages.

## Communities

Community 0: WalletServiceApplication, WalletServiceApplication.java, .main(

## Instructions

Write a single JSON object mapping each community id (as a string) to its
2-5 word name to: c:\Users\semorale\proyectos\yanki-accounts-service\wallet-service\.graphify\label-instructions\communities.json

Example:
```json
{
  "0": "Authentication Flow"
}
```

Then re-run `graphify update` (or `graphify label`) to ingest the names.
