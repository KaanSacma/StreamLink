# StreamLink

<div align="center">

<img src="images/logo.png" alt="StreamLink Logo" width="200"/>

**Integrate streaming platform chats directly into the game with real-time message synchronization.**

[![Version](https://img.shields.io/badge/version-1.2.0-9146FF)](https://github.com/KaanSacma/StreamLink)
[![Hytale](https://img.shields.io/badge/hytale-0.0.1+-00D166)](https://hytale.com)
[![Java](https://img.shields.io/badge/java-25-orange)](https://www.oracle.com/java/)
[![CurseForge](https://img.shields.io/badge/curseforge-download-F16436)](https://www.curseforge.com/hytale/mods/streamlink)
[![ModTale](https://img.shields.io/badge/modtale-download-3B82F6)](https://modtale.net/mod/streamlink-3698e5dc-636a-4565-a42b-fb0dc57d3714)

</div>

---

## Overview

StreamLink is a powerful server-side mod for Hytale that seamlessly integrates streaming platforms with in-game functionality. Turn your viewers into active participants by displaying live chat, triggering in-game events from stream activities, and creating an interactive streaming experience.

### Key Features

- **Real-time Twitch Chat Integration** - Display Twitch chat messages directly in-game with color-coded usernames and badge support
- **Live Event Notifications** - In-game notifications for follows, subscriptions, raids, cheers, and channel point redemptions
- **Interactive Dashboard** - Modern UI for managing credentials and connection status (accessible via `/streamlink ui`)
- **Secure Authentication** - User-controlled credentials with credential masking for security
- **Automatic Updates** - Built-in update checker notifies operators of new versions
- **Rich Badge System** - Display broadcaster, moderator, VIP, subscriber, and Prime badges with custom colors
- **Platform-Agnostic Design** - Architecture ready for future YouTube and Kick integration

---

## Requirements

- **Hytale Server** with plugin support (v0.0.1+)
- **Java 25** or higher
- **Twitch Account** (for integration)
- **Twitch Application** credentials (Client ID & Access Token)

---

## Installation

1. Download the latest release from [CurseForge](https://www.curseforge.com/hytale/mods/streamlink) or [ModTale](https://modtale.net/mod/streamlink-3698e5dc-636a-4565-a42b-fb0dc57d3714)
2. Place the `streamlink-1.1.1.jar` file in your server's `mods/` directory
3. Restart your server
4. Configure your Twitch credentials (see [Configuration](#-configuration))

---

## Configuration

### Getting Your Twitch Credentials

1. **Client ID**: Visit [Twitch Developer Console](https://dev.twitch.tv/console) and create a new application
2. **Access Token**: Generate at [Twitch Token Generator](https://twitchtokengenerator.com/quick/HvO1CktuVV)
    - Required scopes: `bits:read`, `channel:read:subscriptions`, `channel:read:redemptions`, `moderator:read:followers`

### Setup Methods

#### Method 1: Dashboard UI (Recommended)
```bash
/streamlink ui
```
Open the modern dashboard interface to configure all settings with visual feedback.

#### Method 2: Commands
```bash
# Twitch
# Set your credentials
/streamlink twitch setup <client_id> <access_token>

# Set your channel name
/streamlink twitch set <channel_name>

# Connect to Twitch
/streamlink twitch connect

# Disconnect
/streamlink twitch disconnect

# YouTube
# Set your credentials
/streamlink youtube setup <api_key>

# Set your channel ID
/streamlink youtube set <channel_id>

# Connect to YouTube
/streamlink youtube connect

# Disconnect
/streamlink youtube disconnect
```

---

## Usage

### Commands

| Command                                        | Description                       | Permission   |
|------------------------------------------------|-----------------------------------|--------------|
| `/streamlink ui`                               | Open the dashboard UI             | Not Required |
| `/streamlink twitch setup <client_id> <token>` | Configure Twitch credentials      | Not Required |
| `/streamlink twitch set <channel>`             | Set your Twitch channel           | Not Required |
| `/streamlink twitch connect`                   | Connect to Twitch chat and events | Not Required |
| `/streamlink twitch disconnect`                | Disconnect from Twitch            | Not Required |
| `/streamlink youtube setup <api_key>`          | Configure YouTube credentials     | Not Required |
| `/streamlink youtube set <channel_id>`         | Set your YouTube channel ID       | Not Required |
| `/streamlink youtube connect`                  | Connect to YouTube chat           | Not Required |
| `/streamlink youtube disconnect`               | Disconnect from YouTube           | Not Required |


**Aliases:** `/sl` can be used instead of `/streamlink`

### Supported Events

StreamLink automatically detects and displays the following Twitch events:

- 🟣 **Follows** - New follower notifications
- 💛 **Subscriptions** - New subscriber alerts (Tier 1/2/3)
- 💚 **Gift Subscriptions** - Mass gift sub notifications
- 🧡 **Resubscriptions** - Resub messages with month count
- ❤️ **Raids** - Incoming raid alerts with viewer count
- 💜 **Cheers** - Bit donation notifications
- 💙 **Channel Points** - Custom reward redemptions

---

## Features in Detail

### Dashboard UI
- **Status Indicator** - Real-time connection status with color-coded dots
- **Credential Management** - Secure input fields with automatic masking
- **Platform Tabs** - Future-ready interface for YouTube and Kick integration
- **Quick Links** - Direct access to credential generation

### Chat Integration
- **Color-Coded Usernames** - Displays chat messages with user-selected colors
- **Badge Support** - Shows broadcaster, moderator, VIP, subscriber, and Prime badges
- **Emote Detection** - Identifies and marks Twitch emotes in messages
- **Anonymous Mode** - Connects without requiring authentication for chat-only mode

### Event System
- **In-Game Notifications** - Custom notifications with item icons
- **EventSub WebSocket** - Real-time event delivery via Twitch's EventSub API
- **Automatic Reconnection** - Handles connection drops gracefully
- **Token Validation** - Verifies credentials before connection

---

## Building from Source

### Prerequisites
- Java 25+ JDK
- Gradle 8.10+
- `HytaleServer.jar` in project root

### Build Steps
```bash
# Clone the repository
git clone https://github.com/KaanSacma/StreamLink.git
cd StreamLink

# Place HytaleServer.jar in the project root
# (Used as compile-only dependency)

# Build the project
./gradlew build

# The compiled JAR will be in build/libs/
```

---

## Roadmap

### Planned Features
- [ ] Kick.com integration
- [ ] Stream overlay integration
- [ ] Automated in-game actions based on events
- [ ] Keyword-triggered chat commands
- [ ] Multi-streamer support
- [ ] Configurable event cooldowns

---

## Known Issues

- Emotes are displayed as text (`:emote_name:`) rather than images
- EventSub reconnection logic needs implementation for session migrations
- Large credential strings may overflow UI input fields on some resolutions

Report bugs on our [GitHub Issues](https://github.com/KaanSacma/StreamLink/issues) page.

---

## Documentation

For detailed Hytale plugin development documentation, visit:
- [Hytale Plugin Development Guide](https://hytale-docs.pages.dev/getting-started/introduction/)

---

## License

**All Rights Reserved**

Copyright © 2025 KentaTetsu

This software and associated documentation files (the "Software") are proprietary and confidential. Unauthorized copying, distribution, modification, or use of this Software, via any medium, is strictly prohibited without express written permission from the copyright holder.

---

## Author

**KentaTetsu**
- GitHub: [@KaanSacma](https://github.com/KaanSacma)
- CurseForge: [StreamLink](https://www.curseforge.com/hytale/mods/streamlink)
- ModTale: [ModTale](https://modtale.net/mod/streamlink-3698e5dc-636a-4565-a42b-fb0dc57d3714)

---

## Acknowledgments

- Twitch for the EventSub API and IRC chat interface
- [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket) library for WebSocket connectivity
- Hytale community for modding documentation and support
- [Twitch Token Generator](https://twitchtokengenerator.com/) for simplified token creation

---

## Support

Having issues? Here's how to get help:

1. Check the [Documentation](#-configuration) section above
2. Search [existing issues](https://github.com/KaanSacma/StreamLink/issues)
3. Create a [new issue](https://github.com/KaanSacma/StreamLink/issues/new) with details:
    - Your Hytale server version
    - StreamLink version
    - Full error logs
    - Steps to reproduce

---

<div align="center">

**Made with 💜 for the Hytale streaming community**

[Report Bug](https://github.com/KaanSacma/StreamLink/issues) · [Request Feature](https://github.com/KaanSacma/StreamLink/issues) · [View Roadmap](#-roadmap)

</div>