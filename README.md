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
automates
tedious setup, testing, and submission tasks, so you can focus purely on solving problems.

This plugin is designed, owned, and maintained by **Shikhor Kumer Roy**.
During development, **GitHub Copilot** was used as an AI-assisted coding tool to accelerate implementation and improve
developer productivity. All architectural decisions, feature design, and final code responsibility remain with the
author.

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
* **Test Management**: Add and manage custom test cases easily and conveniently.
* **Easy Submission**: Copy your solution source code with one click or select it directly for submission.
* **Theme Support**: Icons and panels follow the light and dark IDE themes.

---

## Requirements & Compatibility

* **Kotlin & Java Support**: KHelper supports both Kotlin and Java. You can switch your preferred language and edit
  templates in **Settings > KHelper**.
* **IDE Version**: IntelliJ IDEA 2025.3 or newer, with the bundled Kotlin and Java plugins enabled.
* **Project JDK 11 or newer**: Tests run in your project's JDK, so the project SDK must be Java 11 or newer.
* **Competitive Companion Port**: KHelper listens on `localhost:10043` by default. You can change the port in
  **Settings > Tools > KHelper**. Use the same port as a custom port in the Competitive Companion extension.

---

## Getting Started

1. **Install KHelper** in your IntelliJ IDE.
2. **Install Competitive Companion** in your browser (Chrome/Firefox).
3. **Configure Preferences**: Go to **Settings > KHelper** to select your preferred language (Kotlin/Java) and customize
   your templates.
4. **Open your project** in IntelliJ.
5. **Go to a problem** in your browser (e.g. on Codeforces).
6. **Click the Competitive Companion button**.
7. **Start Coding!** KHelper will create the file, open it, and set up the run configuration for you.

---

## Local Build from Source

To build and run the plugin locally:

```bash
./gradlew runIde
```

To build a distribution file:

```bash
./gradlew buildPlugin
```

The output file will be located in `build/distributions/`.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
