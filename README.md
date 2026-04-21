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

## Downloads

- [Latest release](https://github.com/OgRudolf/RotProxy-/releases/tag/v1.2.0)
- [Minecraft 1.21.10 jar](https://github.com/OgRudolf/RotProxy-/releases/download/v1.2.0/rotproxy-1.2.0%2Bmc1.21.10.jar)
- [Minecraft 1.21.11 jar](https://github.com/OgRudolf/RotProxy-/releases/download/v1.2.0/rotproxy-1.2.0%2Bmc1.21.11.jar)

## Compatibility

- `1.21.10` build: Fabric Loader `0.17.2+`, Fabric API `0.138.4+1.21.10`
- `1.21.11` build: Fabric Loader `0.19.1+`, Fabric API `0.141.3+1.21.11`
- Java `21`

## Install

1. Download the jar for your Minecraft version from the latest release.
2. Put the jar into your Minecraft `mods` folder.
3. Install Fabric Loader and Fabric API for the matching Minecraft version.
4. Launch Minecraft with the Fabric profile.
5. Open `Multiplayer` and click the `RotProxy` button.

## Build

```powershell
.\BUILD.ps1
```

## Privacy And Safety

- RotProxy stores proxy profiles locally in your Fabric config folder, not in this repository.
- Release builds ship with a blank default proxy profile.
- Never publish your live proxy IPs, usernames, passwords, or screenshots that show them.
- Never upload your local config file `rotproxy_profiles.json`.
- If you accidentally posted working proxy credentials anywhere, rotate them with your proxy provider.

## License

MIT. See `LICENSE`.
