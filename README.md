# RotProxy

RotProxy is a client-side Fabric mod for Minecraft `1.21.10` and `1.21.11` that routes multiplayer connections through a configured proxy.

## Features

- Red and black in-game proxy manager UI
- Multiple saved proxy profiles
- `SOCKS5`, `HTTP`, and `HTTPS` proxy support
- Multiplayer screen quick-access button
- Built-in proxy test button inside the config screen
- Kill switch that blocks joins when the proxy is not healthy
- Automatic proxy heartbeat every `15` seconds
- Exit IP and proxy-check latency display
- Client-side only

## Requirements

- Minecraft `1.21.10` or `1.21.11`
- Fabric Loader `0.17.2` or newer
- Fabric API
- Java `21`

## Build

```powershell
.\BUILD.ps1
```

Built jars:

```text
build\libs\rotproxy-1.2.0+mc1.21.10.jar
build\libs\rotproxy-1.2.0+mc1.21.11.jar
```

## Downloads

GitHub-downloadable builds are stored in this repository:

- `downloads/rotproxy-1.2.0+mc1.21.10.jar`
- `downloads/rotproxy-1.2.0+mc1.21.11.jar`

Compatibility and release details:

- `1.21.10` build: Fabric Loader `0.17.2+`, Fabric API `0.138.4+1.21.10`
- `1.21.11` build: Fabric Loader `0.19.1+`, Fabric API `0.141.3+1.21.11`
- Features: SOCKS5 / HTTP / HTTPS support, Multiplayer quick-access button, in-game proxy test, kill switch, and 15-second heartbeat monitoring
- Privacy: release jars do not ship with live proxy host, IP, port, username, or password data

Checksums:

- `rotproxy-1.2.0+mc1.21.10.jar`: `921B89E549F66CCC99C642800170731EE88114DDE8527846C712B510DC43D5F4`
- `rotproxy-1.2.0+mc1.21.11.jar`: `DF781BA357A9959F33B14CCC309D1AFBFFAB0FC9AFF65F2A6F0E35D86F3328E9`

## Install

1. Build the jar or download a release.
2. Put the jar into your Minecraft `mods` folder.
3. Install Fabric Loader and Fabric API for the matching Minecraft version.
4. Launch Minecraft with the Fabric profile.
5. Open `Multiplayer` and click the `RotProxy` button.

## Privacy And Safety

- RotProxy stores proxy profiles locally in your Fabric config folder, not in this repository.
- Never publish your live proxy IPs, usernames, passwords, or screenshots that show them.
- Never upload your local config file `rotproxy_profiles.json`.
- If you accidentally posted working proxy credentials anywhere, rotate them with your proxy provider.

## License

MIT. See `LICENSE`.
