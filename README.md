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
* **Theme Support**: Toggle between light and dark themes effortlessly.

---

## Requirements & Compatibility

* **Kotlin & Java Support**: KHelper supports both Kotlin and Java. You can switch your preferred language and edit
  templates in **Settings > KHelper**.
* **Java 21 Required**: The plugin is built using Java 21. You **must** run your IDE with a runtime that supports this
  version (or newer). Using older Java versions (like Java 17) will result in version mismatch errors during debugging
  or execution.

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
