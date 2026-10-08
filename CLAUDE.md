## Start here
- Read `context.md` first: it holds the project summary, status and TODOs.
- **Required after every change:** whenever you add, remove or modify anything, (1) update `context.md` to match and keep it short, and (2) refresh the graph. Use `graphify update .` for code changes (free). If any doc (.md/.yaml) changed, also run `/graphify . --update`.

## Token budget (user wants minimal token use)
- Prefer `graphify query/path/explain` over reading or grepping source. Open a file only when you're about to edit it, and read just the lines you need.
- Don't re-read files after editing them. Don't read all of `GRAPH_REPORT.md` unless it's really needed.
- Don't spawn subagents unless required. Keep replies brief.
- Avoid full `/graphify` rebuilds. Use `graphify update .` after code changes (free) and `/graphify . --update` only when docs change.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
