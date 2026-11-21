# Graph Report - .  (2026-11-09)

## Corpus Check
- Corpus is ~101 words - fits in a single context window. You may not need a graph.

## Summary
- 3 nodes · 2 edges · 1 communities detected
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output
- Edge kinds: contains: 1 · method: 1


## Input Scope
- Requested: all
- Resolved: all (source: configured-default)
- Included files: 1 · Candidates: recursive
- Excluded: 0 untracked · 0 ignored · 0 sensitive · 0 missing committed
## God Nodes (most connected - your core abstractions)
1. `WalletServiceApplication` - 2 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities

### Community 0 - "Community 0"
Cohesion: 0.67
Nodes (1): WalletServiceApplication

## Knowledge Gaps
- **Thin community `Community 0`** (1 nodes): `WalletServiceApplication`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Not enough signal to generate questions. This usually means the corpus has no AMBIGUOUS edges, no bridge nodes, no INFERRED relationships, and all communities are tightly cohesive. Add more files or run with --mode deep to extract richer edges._