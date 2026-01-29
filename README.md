# XyloGame

### A VoxelBased Block game made entirely from scratch in OpenGL!

---


## Prerequisites

* Java 21 or later (OpenJDK 21+). The project has ONLY been tested on Java 21.
* Gradle (the repository includes a Gradle wrapper, so a separate Gradle install is not required)
* A GPU with up-to-date OpenGL drivers (should support OpenGL 3.3)

---

## Quick setup

Clone the repository and run the project using the Gradle wrapper:

```bash
git clone https://X3r0Day/XyloGame.git && cd XyloGame

# Build
./gradlew build

# Run (uses Gradle application plugin)
./gradlew run
```

If the distribution includes native libraries (natives for LWJGL or similar), the Gradle run task will normally handle them. If you encounter issues with native libraries, ensure the native artifact classifier for your platform is available and that `java.library.path` is correctly set when running outside the Gradle wrapper.

---

## Discord Rich Presence integration

XyloGame optionally supports Discord Rich Presence. To enable it, set the `DISCORD_APP_ID` environment variable before running the game.

### Fish shell

```fish
set -x DISCORD_APP_ID <your_app_id>
./gradlew run
```

### Bash / Zsh

```bash
export DISCORD_APP_ID=<your_app_id>
./gradlew run
```

Notes:

* Use the application ID you created in the Discord Developer Portal.
* The application will attempt to initialize Discord RPC only if the environment variable is present.

---

## Configuration

Soon to be added

---

## Development

* Project layout follows standard Gradle conventions (`src/main/java`, `src/main/resources`).
* E.g., The renderer and shader code are located under `me.xeroday.engine`.
* For iteration, run `./gradlew run` and modify code; Gradle will compile updated classes on subsequent runs.


## Contributing

Contributions are welcome. Recommended workflow:

1. Fork the repository.
2. Create a feature branch: `git checkout -b feat/your-feature`.
3. Implement changes and add tests where applicable.
4. Open a pull request with a clear description and motivation.

Please follow the existing code style and keep commits focused and atomic.

---

## License & Attribution


You are free to use and modify this code, provided you give clear credit to **X3r0Day** and link back to this repository.

You are free to use, modify, and distribute this software, but **you must provide appropriate credit** to project *"X3r0Day"* and provide a link to this Repository!

If you modify this code or integrate it into your own project, you **must**:
- Include a link back to this repository in your documentation or README.

**Recommended Attribution Format:**
> *"Based on XyloGame by X3r0Day."*

---

## Contact / Attribution

Author: XeroDay (Project X3r0Day)

If you would like improvements to the build instructions, CI configuration, example screenshots, or packaging for distribution (native launchers), tell me which area to expand and I will update the README accordingly.
