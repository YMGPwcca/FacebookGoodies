# Third-party components

## Xposed Framework API 82

`app/libs/xposed-api-82.jar` provides compile-time Xposed interfaces. The module declares it as `compileOnly`; the API classes are not embedded in the generated APK.

- Maven coordinate: `de.robv.android.xposed:api:82`
- Upstream API documentation: <https://api.xposed.info/>
- Upstream project and source: <https://github.com/rovo89/XposedBridge>
- License: Apache License 2.0
- JAR SHA-256: `f48c635f1c7469fdec0e00ad2ea0b7a6b2f5b55065784a35b7ca3a84615e8e25`

The complete Apache License 2.0 text is in [LICENSE](LICENSE).

## mp4parser 1.9.56

The download-only VP9 MP4 fallback uses `org.mp4parser:muxer` and `org.mp4parser:isoparser`, version 1.9.56. It remuxes container tracks without decoding or re-encoding them. Upstream: <https://github.com/sannies/mp4parser>. Copyright Sebastian Annies and CoreMedia AG; Apache License 2.0, included in [LICENSE](LICENSE).

Transitive runtime components: AspectJ runtime 1.9.7 (Eclipse Public License 2.0, <https://www.eclipse.org/legal/epl-2.0/>) and SLF4J API/NOP 1.8.0-beta4 (MIT license, <https://www.slf4j.org/license.html>). The NOP logger prevents the library from logging media details. JUnit is excluded from the runtime dependency.
