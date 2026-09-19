# MossLib

A lightweight Fabric library providing a self-contained Bedrock model loader
for Minecraft Java Edition.

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).

## About the Bedrock model implementation

The Bedrock geometry parsing and conversion code in this library is an
**independent implementation** written from scratch, based on:

- The public Bedrock geometry format specification
  (https://wiki.bedrock.dev/visuals/entity-models.html)
- Minecraft's official client model API
  (`ModelData`, `ModelPartBuilder`, `ModelTransform`, `TexturedModelData`)
- Original coordinate-system derivation for translating between Bedrock and
  Java model spaces

No source code from AmbleKit, Cobblemon, or any other MPL/LGPL-licensed
project has been copied into this library.

### Design differences from other implementations

- Three-layer architecture: parser / converter / public API, instead of a
  single monolithic class
- Custom `Vec3` value type instead of boxed `List<Float>`
- Topological bone resolution, order-independent
- Per-model-part caching keyed by `Identifier`
- Multi-geometry support with identifier selection
- Explicit rejection of unsupported features (per-face UV) with descriptive
  error messages instead of silent fallback

### References consulted

- Bedrock Wiki: https://wiki.bedrock.dev/
- Minecraft Yarn mappings Javadoc
- AmbleKit's public API surface (read for interface design inspiration only;
  no code was copied)

## Contributing

By submitting a pull request, you agree to license your contribution under
GPL-3.0-or-later.