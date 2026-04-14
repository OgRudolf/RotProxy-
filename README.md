# RotProxy

RotProxy is a client-side Fabric mod for Minecraft `1.21.10` that routes multiplayer connections through a configured proxy.

## Features

- Red and black in-game proxy manager UI
- Multiple saved proxy profiles
- `SOCKS5`, `HTTP`, and `HTTPS` proxy support
- Exit IP and proxy-check latency display
- Client-side only

## Requirements

- Minecraft `1.21.10`
- Fabric Loader `0.17.2` or newer
- Fabric API
- Java `21`

## Build

```powershell
.\gradlew.bat build
```

Built jar:

```text
build\libs\rotproxy-1.1.0.jar
```

## Install

1. Build the jar or download a release.
2. Put the jar into your Minecraft `mods` folder.
3. Install Fabric Loader and Fabric API for Minecraft `1.21.10`.
4. Launch Minecraft with the Fabric profile.
5. Open `Multiplayer` and click the `RotProxy` button.

## Privacy And Safety

- RotProxy stores proxy profiles locally in your Fabric config folder, not in this repository.
- Never publish your live proxy IPs, usernames, passwords, or screenshots that show them.
- Never upload your local config file `rotproxy_profiles.json`.
- If you accidentally posted working proxy credentials anywhere, rotate them with your proxy provider.


## License

MIT. See `LICENSE`.
