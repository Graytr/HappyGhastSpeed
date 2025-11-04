# HappyGhastSpeed (Paper plugin)

**Happy Ghasts Speed (1.21.x)**: This plugin targets the official `HappyGhast` entity in modern Paper. 
It also includes a fallback to recognize backports/datapacks that tag a vanilla ghast as a "happy" one.

- **Speed I** → **+40%** flight speed
- **Speed II** → **+80%** flight speed
- III+ scales +40%/level (linear), optional but handy for custom servers.

### How it works
We listen for changes to the **SPEED** potion effect on Happy Ghasts and set a single attribute modifier on
`GENERIC_FLYING_SPEED` (falling back to `GENERIC_MOVEMENT_SPEED` if needed). The modifier is removed when
the effect ends and on plugin disable.

### Compatibility
- Paper **1.21.x** (uses `org.bukkit.entity.HappyGhast`)
- Fallbacks for some backports/datapacks:
  - entity type is `GHAST` **and** it has scoreboard tag `happy_ghast` **or** custom name contains `Happy Ghast`

### Build
```bash
mvn -q -DskipTests package
```
Jar: `target/happyghast-speed-1.1.0.jar`

### Testing
- Get a Happy Ghast (dried ghast → ghastling → happy ghast; then harness).
- Splash/Lingering **Swiftness**. Watch the speed change immediately.
