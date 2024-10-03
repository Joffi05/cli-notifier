// src/notifier/webhook.rs
use crate::Notifier;
use async_trait::async_trait;
use reqwest::Client;

pub struct WebhookNotifier {
    url: String,
    secret: String,
    client: Client,
}

impl WebhookNotifier {
    pub fn new(url: String, secret: String) -> Self {
        Self {
            url,
            secret,
            client: Client::new(),
        }
    }
}

#[async_trait]
impl Notifier for WebhookNotifier {
    async fn send(&self, message: &str) -> Result<(), Box<dyn std::error::Error>> {
        let res = self
            .client
            .post(&self.url)
            .header("Authorization", format!("Bearer {}", self.secret))
            .header("Content-Type", "application/json")
            .json(&serde_json::json!({ "text": message.to_string() }))
            .send()
            .await?;

        if res.status().is_success() {
            Ok(())
        } else {
            Err(format!("Failed to send webhook notification: {}", res.status()).into())
        }
    }
}
