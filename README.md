# ekw: learning skeleton

The project you build while following the [development guide](../README.md). Start with the guide, not here.

- Everything compiles from day one; classes you write throw `Todo` ("TODO (development guide, chapter N)").
- Each chapter's tests are tagged `chNN`. They fail until you write the code.
- `./mvnw verify` runs the chapters listed in `<ekw.chapters>` in `pom.xml` (at first only `skeleton`).
- `./mvnw verify -Dekw.chapters=ch02` runs one chapter. Add `-Pnetwork` for tests that clone repositories.
- No test calls a paid model API.
