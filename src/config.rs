// src/config.rs
use serde::Deserialize;

#[derive(Deserialize)]
pub struct Config {
    pub notifiers: Notifiers,
}

#[derive(Deserialize)]
pub struct Notifiers {
    pub desktop: Option<DesktopConfig>,
    pub pushbullet: Option<PushbulletConfig>,
    pub webhook: Option<WebhookConfig>,
}

#[derive(Deserialize)]
pub struct DesktopConfig {}

#[derive(Deserialize)]
pub struct PushbulletConfig {
    pub api_key: String,
}

#[derive(Deserialize)]
pub struct WebhookConfig {
    pub url: String,
    pub secret: String,
}
