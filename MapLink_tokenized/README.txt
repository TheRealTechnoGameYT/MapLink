MapLink - Plugin tokenized for BlueMap
-------------------------------------

Files in this project:
- build.gradle
- src/main/java/com/tonnom/maplink/MapLinkPlugin.java
- src/main/resources/plugin.yml
- src/main/resources/config.yml
- plugins/BlueMap/web/self-only-players-token.js

How to build:
1. Install JDK 17 and Gradle.
2. From project root run: ./gradlew build
3. The jar will be in build/libs/MapLink-1.0.jar

Installation on server:
1. Put the jar in plugins/ of your Paper/Spigot server.
2. Start server to generate config.
3. Edit plugins/MapLink/config.yml: set web-url and web-token-file (path inside BlueMap web folder).
4. Copy self-only-players-token.js to your BlueMap web folder (plugins/BlueMap/web/) if not already there.
5. Add the script to plugins/BlueMap/webapp.conf scripts: ["self-only-players-token.js"]
6. Restart and use /maplink token to get private link.

Notes:
- This project is built for BlueMap web integration.
- I included the JS file and configured token JSON path to plugins/BlueMap/web/maplink_tokens.json by default.
- I cannot compile the jar here, but you can build with Gradle using the commands above.
