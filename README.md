<p align="center">
  <img src="src/main/resources/META-INF/pluginIcon.svg" alt="KHelper Plugin Icon" width="200" />
</p>

<h1 align="center">KHelper</h1>

<p align="center">
  <strong>Competitive Programming Plugin for IntelliJ IDEA</strong>
</p>

<p align="center">
  Automates setup, testing, and submission - so you can focus purely on problem-solving.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Language-Kotlin-blueviolet" />
  <img src="https://img.shields.io/badge/Language-Java-red" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/IntelliJ%20IDEA-Compatible-blue" />
  <img src="https://img.shields.io/badge/License-MIT-green" />
</p>

---

## KHelper - A Kotlin (& Java 😉) CP Plugin for IntelliJ IDEA

**KHelper** is a powerful IntelliJ IDEA plugin designed to significantly enhance the competitive programming experience
for Kotlin/Java programmers. Inspired by the functionality and spirit of the classic **CHelper** plugin. KHelper
automates tedious setup, testing, and submission tasks, so you can focus purely on solving problems.

This plugin is designed, owned, and maintained by **Shikhor Kumer Roy**. During development, **GitHub Copilot** was used
as an AI-assisted coding tool to accelerate implementation and improve developer productivity. All architectural
decisions, feature design, and final code responsibility remain with the author.

---

## Key Features

* **One Click Problem Import**: Works seamlessly with the **Competitive Companion** browser extension to bring problems
  directly into your IDE.
* **Automatic Code Setup**: Instantly generates solution files with your preferred template, no more manual file
  creation or boilerplate copying and pasting.
* **Focus on Solving**: With just clicks, eliminate repetitive manual work and concentrate on what matters - solving
  problems.
* **Accident Proof**: Detects if a solution file already exists and asks for your permission before overwriting, keeping
  your hard work safe.
* **Instant Testing & Debugging**: Automatically sets up IntelliJ Run Configurations so you can run or debug against
  sample test cases with a single click.
* **Clear Verdicts**: Each test shows Accepted, Wrong Answer, Runtime Error or Time Limit Exceeded. Every test starts
  with fresh static state and a large stack, like on online judges.
* **Test Management**: Add, edit, duplicate and delete test cases, and turn single tests or all tests on and off.
* **Easy Submission**: After every run, KHelper writes a single `Main` file with your solution and the project code it
  uses. **Copy Submission** puts it on the clipboard at any time.
* **Quick Actions**: Open the source, open the problem in the browser, copy the submission, delete the problem and open
  settings, from the KHelper tool window title bar or from **Tools > KHelper**.
* **Theme Support**: Icons and panels follow the light and dark IDE themes.

---

## Requirements & Compatibility

* **Kotlin & Java Support**: KHelper supports both Kotlin and Java. You can switch your preferred language and edit
  templates in **Settings > Tools > KHelper**.
* **IDE Version**: IntelliJ IDEA 2025.3 or newer, with the bundled Kotlin and Java plugins enabled.
* **Project JDK 11 or newer**: Tests run in your project's JDK, so the project SDK must be Java 11 or newer.
* **Competitive Companion Port**: KHelper listens on `localhost:10043` by default, which Competitive Companion sends to
  without any setup. You can change the port in **Settings > Tools > KHelper**. A different port must also be added as a
  custom port in the Competitive Companion extension.

---

## Getting Started

1. **Install KHelper** in your IntelliJ IDE.
2. **Install Competitive Companion** in your browser (Chrome/Firefox).
3. **Configure Preferences**: Go to **Settings > Tools > KHelper** to select your preferred language (Kotlin/Java) and
   customize your templates.
4. **Open your project** in IntelliJ.
5. **Go to a problem** in your browser (e.g. on Codeforces).
6. **Click the Competitive Companion button**.
7. **Start Coding!** KHelper will create the file, open it, and set up the run configuration for you.

---

## Local Build from Source

Start a sandbox IDE with the plugin:

```bash
./gradlew runIde
```

The sandbox listens on port `10044` (`sandboxPort` in `gradle.properties`), so it does not clash with KHelper in your
main IDE. Add that port as a custom port in Competitive Companion. This port is for development only; real installations
use the port from Settings.

To load code changes into a running sandbox without a restart, run this in a second terminal:

```bash
./gradlew prepareSandbox
```

Run the tests:

```bash
./gradlew test
```

Build a distribution file. The output file is in `build/distributions/`:

```bash
./gradlew buildPlugin
```

Check compatibility with the plugin verifier (add `-PverifyAllIdes` to check all recommended IDE versions):

```bash
./gradlew verifyPlugin
```

Releases are built and published by GitHub Actions. See [docs/PIPELINES.md](docs/PIPELINES.md) for the pipelines and the
release steps.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
