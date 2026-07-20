
## Commits

- `<type>` is required and must be one of the types listed below.
- `<scope>` is optional and should describe the part of the codebase affected (e.g., `core`, `api`, `config`, `ui`, `logging`).
- `<subject>` is a short, imperative description of the change (e.g., "Add validation for user input", "Fix null pointer in parser").

### Valid Types

| Type        | Description (from Techor specification with my variations)                                                                                             |
|-------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| `feat`      | Introducing a new feature.                                                                                                                             |
| `fix`       | Patching a bug.                                                                                                                                        |
| `perf`      | A code change that improves performance.                                                                                                               |
| `add`       | Adding new content or options (e.g., Add(CSS): Option '.preference' for default theme).                                                                |
| `update`    | Static content updates such as articles, news, about, profile, etc.                                                                                    |
| `improve`   | Improving existing functionality (e.g., Improve(Home): Swap the order of Feature and Pricing).                                                         |
| `deprecate` | Deprecating features, options, parameters, units, pages, etc.                                                                                          |
| `upgrade`   | Upgrading environment, system, dependencies, etc.                                                                                                      |
| `revert`    | Reverting a previous commit.                                                                                                                           |
| `docs`      | Documentation only changes (also comments except TODO/FIXME (see chore)).                                                                              |
| `style`     | Code formatting (e.g. white-space), etc.                                                                                                               |
|             | Also for adding or improving user messages and help text.                                                                                              |
| `example`   | Adding or updating usage examples.                                                                                                                     |
| `test`      | Adding missing tests or correcting existing tests.                                                                                                     |
| `refactor`  | Code change that neither fixes a bug nor adds a feature, yet improves its internal structure. This includes :                                          |
|             | - Internal consistency checks that throw on failure (scope : “consistency-checks”).                                                                    |
|             | - Moving logic into new interfaces' implementations (scope: “interfaces“).                                                                             |
|             | - Removing dead code.                                                                                                                                  |
|             | - Adding logging.                                                                                                                                      |
| `chore`     | Other changes that don't modify source (or are just TODO/FIXME comments), test files (filesystem), notes on things to do. E.g. .gitignore, file links. |
| `build`     | Changes to build system. F.ex. modifying dependencies (scope: “dependencies").                                                                         |
| `bump`      | For manually triggering a version bump (bump(patch), bump(minor), bump(major)).                                                                        |

### Known Scopes (mine)

| Scope                | Description                                                                            |
|----------------------|----------------------------------------------------------------------------------------|
| `comments`           | JavaDoc or regular comments (except TODO / FIXME). Type always docs.                   | 
| `debug`              | Code to facilitate debugging (except log which is a Scope in itself).                  |
| `exceptions`         |                                                                                        |
| `filesystem`         |                                                                                        |
| `future-changes`     | F.ex. TODO / FIXME (Type chore), notes for possible new implementations (Type update). |
| `interfaces`         | Type is always refactor.                                                               |
| `log`                | Adding / removing / modifying logging code and logged text.                            |
| `dependencies`       | Adding / removing / modifying dependencies.                                            |
| `consistency-checks` | F.ex. Assertions.                                                                      |
| `userIO`             | User messages, colors, anything about the style (not behavior) of the UI.              |


### Common examples of Type + Scope + possibly Subject

docs(comments) : Improve JavaDoc.

refactor(logging)

refactor(exceptions) : Remove spurious 'throws' clauses.

refactor(debug) : Add field to possibly keep temp files.

style(userIO) : Improve user messages.

style(format) : Whitespace.


### Breaking Changes

If your change introduces a breaking change, add `!` after the type/scope:

Feat(api)!: change response format from XML to JSON

The `!` indicates a breaking change and will trigger a major version bump.

---

## Development Workflow

1. Fork the repository.
2. Create a feature branch.
3. Make your changes.
4. Write or update tests as needed.
5. Ensure all tests pass.
6. Commit your changes using the commit message convention above.
7. Push your branch and open a pull request.

---

## Code Style

Please follow the existing code style of the project. If you are unsure, refer to the style guide or ask in the pull
request.

### IMPORTANT

The indentation of the Java code is tuned to IntelliJ and it works if the inlay hints are shown, with the setting "Use
editor font for inlay hints" true (Settings / Editor / General / Appearance) and the editor font "JetBrains Mono" size
13.0 (Settings / Editor / Font).

---

## Questions?

If you have any questions about contributing, feel free to open an issue or reach out to the maintainers.

---
