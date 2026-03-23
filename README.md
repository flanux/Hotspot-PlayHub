# Hotspot PlayHub

A local multiplayer platform for Android that runs over WiFi hotspot. Connect multiple phones and play games, chat, and share content without internet.

## Features

V1 includes:

- Host/Join sessions over WiFi hotspot
- Chat module for text messaging
- Chess game (turn-based, 2 players)
- Clipboard sharing
- Phone finder (ring lost phones)
- Binary packet protocol for efficient networking
- Modular architecture for easy feature additions

## Architecture

### Core Engine

- Server: Host-authoritative networking on the phone that creates the hotspot
- Client: Connects to host server (joins the hotspot)
- PacketProtocol: Binary packet serialization for all messages
- MessageRouter: Dispatches packets to appropriate modules
- SessionManager: Handles lobby and player tracking
- TickLoop: Fixed-rate update loop for games

### Modules

All features are implemented as pluggable modules:

- ChatModule: Text messaging
- ChessModule: Turn-based chess game
- ClipboardModule: Text sharing between devices
- PhoneFinderModule: Ring phones on the network

## How It Works

1. One phone creates a WiFi hotspot (becomes host)
2. Other phones connect to that hotspot
3. Host runs server on 192.168.43.1:8888
4. Clients connect and join the session
5. All communication uses binary packets
6. Host validates and broadcasts game state

## Network Protocol

Packet structure:
```
| Type (1 byte) | PlayerID (1 byte) | Payload length (2 bytes) | Payload (variable) |
```

Packet types:
- 0x01: JOIN
- 0x02: LEAVE
- 0x03: CHAT
- 0x04: GAME_INPUT
- 0x05: GAME_STATE
- 0x06: CLIPBOARD
- 0x07: PHONE_FIND
- 0x08: LOBBY_UPDATE
- 0x09: MODULE_SWITCH
- 0x0A: HEARTBEAT

## Building

This project uses GitHub Actions for automated builds.

Just push to main branch and download the APK from Releases.

### Local Build (optional)

```bash
gradle assembleDebug
```

## Requirements

- Android 7.0 (API 24) or higher
- WiFi capability
- Permissions: INTERNET, ACCESS_WIFI_STATE, CHANGE_WIFI_STATE

## Usage

1. Host: Open app and tap "Host Session"
2. Host: Turn on mobile hotspot in phone settings
3. Clients: Connect to host's WiFi hotspot
4. Clients: Open app and tap "Join Session"
5. All players see lobby with connected players
6. Host selects a module (Chat, Chess, etc.)
7. Everyone switches to that module automatically

## Future Modules

- Ludo (turn-based, 2-4 players)
- Walkie Talkie (voice chat)
- Camera viewer (live video streaming)
- Whiteboard (collaborative drawing)
- Real-time multiplayer games (Snake, Tank Battle)
- Party games

## Technical Details

- Language: Kotlin
- Min SDK: 24
- Target SDK: 34
- Networking: Raw sockets (TCP)
- Architecture: Host-authoritative server model
- Update rate: 20 ticks per second for games

## License

MIT License
