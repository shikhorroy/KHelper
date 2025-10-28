# 🎯 Kotlin Competitive Programming Helper

A modern, feature-rich Kotlin web application designed to streamline competitive programming workflow. This tool
integrates with **Competitive Companion** browser extension, provides a web-based UI for test management, and includes
powerful features like output comparison, test case tracking, and batch execution.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-blue.svg)](https://kotlinlang.org/)
[![Ktor](https://img.shields.io/badge/Ktor-3.3.1-orange.svg)](https://ktor.io/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 📌 Core Files - Understanding Your Workflow

This project has **three main components**, each serving a specific purpose in your competitive programming workflow:

### 1️⃣ **CompetitiveCompanionListener.kt** - Problem Parser Server

**Purpose:** Listens for problems from Competitive Companion browser extension  
**When to run:** Start this FIRST before parsing any problems  
**Command:** `./gradlew run`

```kotlin
// Entry point: Starts the web server
fun main() {
    val server = ProblemParserServer()
    server.start()  // Runs on localhost:10045
}
```

**What it does:**

- Starts a web server on `http://127.0.0.1:10045`
- Receives problem data from Competitive Companion
- Saves test cases to `sample/input/` and `sample/output/`
- Provides web UI for test management and comparison

---

### 2️⃣ **LocalhostSolver.kt** - Local Test Runner

**Purpose:** Runs your solution against test cases locally  
**When to run:** After writing your solution, to test it  
**Command:** `./gradlew runSolver`

```kotlin
// Entry point: Runs batch tests locally
fun main() {
    // Automatically detects and runs all test files
    // Uses solve() function from Solver.kt
}
```

**What it does:**

- Reads test files from `sample/input/`
- Calls your `solve()` function from `Solver.kt`
- Generates outputs in `output/` folder
- Skips already-matched tests for efficiency
- Can run single test from `single-input.txt`

---

### 3️⃣ **Solver.kt** - Your Solution (For Submission)

**Purpose:** Contains your actual CP solution - THIS IS WHAT YOU SUBMIT  
**When to edit:** Write your solution logic here  
**When to submit:** Copy this entire file to the online judge

```kotlin
// Your problem-specific solver
fun solve(fs: FastScanner, out: FastWriter) {
    // ✍️ Write your solution here
    val tCases = fs.nextInt()
    for (cs in 1..tCases) {
        // Your code here
    }
}

fun main() {
    FastWriter(System.out).use { out ->
        solve(FastScanner(System.`in`), out)
    }
}

// Includes FastScanner and FastWriter classes
```

**What it contains:**

- `solve()` function - Your solution logic
- `main()` function - For online judge execution
- `FastScanner` class - Optimized input reading
- `FastWriter` class - Optimized output writing

**⚠️ Important:** This entire file gets submitted to the online judge. Don't modify the I/O classes unless necessary.

---

## 🔄 Complete Workflow

### Step-by-Step Process:

```
┌─────────────────────────────────────────────────────────────┐
│  1️⃣  START THE SERVER                                       │
│  ./gradlew run                                              │
│  (Runs CompetitiveCompanionListener.kt)                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  2️⃣  PARSE PROBLEM                                          │
│  • Open problem in browser                                  │
│  • Click Competitive Companion extension                    │
│  • Accept in web UI (http://127.0.0.1:10045)                │
│  → Test files saved to sample/input/ & sample/output/       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  3️⃣  WRITE SOLUTION                                         │
│  • Edit Solver.kt                                           │
│  • Implement logic in solve() function                      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  4️⃣  TEST LOCALLY                                           │
│  ./gradlew runSolver                                        │
│  (Runs LocalhostSolver.kt)                                  │
│  → Generates outputs in output/ folder                      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  5️⃣  COMPARE RESULTS                                        │
│  • Visit http://127.0.0.1:10045/compare                     │
│  • Check which tests pass/fail                              │
│  • See line-by-line differences                             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  6️⃣  FIX & RETEST                                           │
│  • Fix bugs in Solver.kt                                    │
│  • Run: ./gradlew runSolver                                 │
│  • Only failed tests run (smart tracking!)                  │
│  • Repeat until all tests pass ✅                           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  7️⃣  SUBMIT                                                 │
│  • Copy entire Solver.kt file                               │
│  • Paste to online judge                                    │
│  • Submit and get AC! 🎉                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## ✨ Features

### 🌐 Modern Web UI (Plain HTML/CSS/JS)

- **Easy to Customize** - Direct HTML/CSS/JS files, no Kotlin recompilation needed
- **Standard Web Stack** - Use any web development tools and browser DevTools
- **Real-time Updates** - Live problem data display with auto-refresh
- **Professional Design** - Clean, responsive interface with modern styling

### 📥 Competitive Companion Integration

- **Automatic Problem Import** - Receive problems directly from online judges
- **One-Click Accept/Reject** - Review and accept problems via web UI
- **Metadata Extraction** - Automatically extracts problem name, constraints, test cases

### 🧪 Test Case Management

- **Add Test Cases** - Create custom test cases via web form
- **Edit Test Cases** - Modify existing test cases inline
- **Delete Test Cases** - Remove unwanted tests with auto-renumbering
- **Visual Editor** - Large text areas for comfortable editing
- **Keyboard Shortcuts** - `Ctrl+Enter` to quickly add tests

### 🔍 Output Comparison

- **Automatic Comparison** - Compare expected vs actual outputs
- **Detailed Diff View** - Line-by-line difference highlighting
- **Visual Indicators** - Color-coded success/failure badges
- **Expandable Content** - Show/hide matched output content
- **Statistics Dashboard** - Summary cards showing match/fail counts

### 🎯 Smart Test Tracking

- **Matched Test Tracking** - Remembers which tests already pass
- **Auto-Skip** - Skip already-matched tests in batch runs
- **Persistent State** - Tracking survives across sessions
- **Force Re-run** - Easy reset by deleting `.matched-tests.txt` or by removing test from `.matched-tests.txt`

### ⚡ Batch Execution

- **Multi-Test Support** - Run all test cases in one go
- **Automatic File Management** - Creates output files automatically
- **Progress Tracking** - See which tests are running
- **Fast Execution** - Optimized I/O with custom FastScanner/FastWriter

### 🧹 Auto-Cleanup

- **New Problem Reset** - Automatically clears old data when accepting new problems
- **Directory Management** - Cleans `output/` and `.matched-tests.txt`
- **Fresh State** - Each problem starts with a clean slate

---

### Prerequisites

- **Java 21** or higher
- **Kotlin 2.2.20** (automatically managed by Gradle)
- **Competitive Companion** browser
  extension ([Chrome](https://chrome.google.com/webstore/detail/competitive-companion/cjnmckjndlpiamhfimnnjmnckgghkjbl) / [Firefox](https://addons.mozilla.org/en-US/firefox/addon/competitive-companion/))

### Installation

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd kotlin-cp
   ```

2. **Verify Java version:**
   ```bash
   java -version
   # Should be Java 21 or higher
   ```

### First Time Setup

1. **Install Competitive Companion** browser extension
2. **Configure the extension:**
    - Open extension settings
    - Set custom port: `10045`
    - Set URL: `http://127.0.0.1:10045`

### Daily Usage

```bash
# Terminal 1: Start the server (keep it running)
./gradlew run

# Terminal 2: Test your solutions
./gradlew runSolver
```

Then use your browser to parse problems and compare outputs!

---

### Batch vs Single Mode

**Batch Mode** (automatic detection):

- Activated when `sample/input/` folder exists
- Processes all `.txt` files
- Outputs to `output/` folder

**Single Mode** (fallback):

- Reads from `single-input.txt`
- Writes to `output.txt`
- Or uses stdin/stdout if file not found

---

## 🐛 Troubleshooting

### Server Won't Start

**Problem:** Port already in use

**Solution:**

```bash
# Find and kill the process
lsof -ti:10045 | xargs kill -9

# Or change the port in ServerConfig.kt
```

### Competitive Companion Not Connecting

**Problem:** Extension can't reach server

**Solutions:**

1. Ensure server is running: `./gradlew run`
2. Check port in extension settings matches `ServerConfig.PORT`
3. Try `localhost` instead of `127.0.0.1`
4. Check firewall settings

---

## 🤝 Contributing

Contributions are welcome! Feel free to:

- Report bugs
- Suggest features
- Submit pull requests
- Improve documentation

---

## 📝 License

This project is licensed under the MIT License.

---

## 🙏 Acknowledgments

- **Competitive Companion** - Browser extension for problem parsing
- **Ktor** - Modern Kotlin web framework
- **Kotlin** - Programming language
- Competitive programming community

---

## 🎉 Happy Coding!

Built with ❤️ for competitive programmers who value efficiency and automation.

**Star ⭐ this repo if you find it useful!**

