## Contributing

To find our what this project is about, read the [README.md](README.md)

Contribute via [discussions](https://github.com/Zeckie/ProxyAuth/discussions), issues and pull requests

Significant changes / new features should be [discussed first](https://github.com/Zeckie/ProxyAuth/discussions) before
implementing.

## Building

The project is configured to use [Gradle Toolchains](https://docs.gradle.org/current/userguide/toolchains.html), so any
[supported Java version](https://docs.gradle.org/current/userguide/compatibility.html) will be sufficient to complete
the build.

To compile, execute tests, and generate the executable jar, run the gradle `build` task :

```bash
./gradlew build
```

This will create the jar file in `build/libs` (e.g. `build/libs/ProxyAuth-0.1.1.jar`)

If you are building in an environment where internet access is via a proxy, you will most likely need
to [configure Gradle's proxy settings](https://docs.gradle.org/current/userguide/build_environment.html#sec:accessing_the_web_via_a_proxy)

```
javac -sourcepath src\main\java -d bin src\main\java\proxyauth\*.java
```

To run, use:

```