# Contributing to Colour Droplet

Thank you for your interest in contributing to **Colour Droplet**! We welcome contributions from everyone, whether it's reporting a bug, proposing a feature, improving documentation, or submitting code changes.

---

## Code of Conduct

Please note that this project adheres to our [Code of Conduct](CODE_OF_CONDUCT.md). By participating in this project, you are expected to uphold this code.

---

## How Can I Contribute?

### Reporting Bugs

Before creating a bug report:
1. Check the [Issues](https://github.com/leekayden/colour-droplet/issues) page to see if the issue has already been reported.
2. If it hasn't, open a new issue using our **Bug Report** template.
3. Include clear steps to reproduce, your OS version, Java version, and display scaling settings if applicable.

### Suggesting Enhancements

Feature requests are very welcome:
1. Check existing issues to see if the idea has already been proposed.
2. Open an issue using our **Feature Request** template, explaining why this enhancement would be useful and how it should work.

### Submitting Pull Requests

1. **Fork the repository** and clone your fork locally:
   ```bash
   git clone https://github.com/<your-username>/colour-droplet.git
   cd colour-droplet
   ```
2. **Create a feature branch**:
   ```bash
   git checkout -b feature/my-new-feature
   ```
3. **Make your changes**:
   - Ensure your code adheres to standard Java conventions.
   - Keep dependencies minimal (the app aims to stay lightweight).
   - Test your changes locally on your platform.
4. **Compile and test locally**:
   - **Windows**: run `run.bat` or:
     ```cmd
     javac -encoding UTF-8 -cp "lib/*" -d out src/*.java
     java -cp "out;lib/*" ColorDropletApp
     ```
   - **macOS / Linux**: run `./run.sh` or:
     ```bash
     javac -encoding UTF-8 -cp "lib/*" -d out src/*.java
     java -cp "out:lib/*" ColorDropletApp
     ```
5. **Commit your changes**:
   - Write clear, concise commit messages.
6. **Push to your fork** and open a Pull Request against the `main` branch.

---

## Development Guidelines

- **Java Version Compatibility**: Target Java 8+ to keep the application as broadly compatible as possible. Avoid using features exclusive to newer Java versions unless necessary or properly handled.
- **DPI Awareness**: Colour picking must handle high-DPI scaling gracefully across multi-monitor setups.
- **Code Style**:
  - 4 spaces indentation.
  - Keep methods focused and avoid unnecessary external dependencies.
