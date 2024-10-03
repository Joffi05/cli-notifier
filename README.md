# Webhook Notifier

**Webhook Notifier** is a small project to send a notification to all your devices when a bash command is done running.

### Usage
`.cli-notifier [Command]`

## Installation

### Android App

1. **Clone the Repository:**
    - `git clone https://github.com/Joffi05/cli-notifier.git`

2. **Open in Android Studio**
    - Open the android folder and build the apk
    - Install the app and run it

3. **Select a secret for authentication**
    - Input a secret and click save

### Rust Client

1. **Build the project**
    - `cd cli-notifier`
    - `cargo build --release`

### Configuration
## config.toml
Create a `config.toml` in `~/.config/cli-notifier/`
I could look like this:
```
[notifiers.desktop]

[notifiers.webhook]
url = "http://192.168.0.12:8000/webhook"
secret = "very-secret"
```

The `[notifiers.desktop]` specifies that the user also wants the notification on the desktop through the `notify-rust` crate.
The `[notifiers.webhook]` specifies that the user want the notification via webhook. `url` defines the target url. `secret` is... well

