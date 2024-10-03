// src/notifier/desktop.rs
use crate::Notifier;
use notify_rust::Notification;
use async_trait::async_trait;

pub struct DesktopNotifier;

#[async_trait]
impl Notifier for DesktopNotifier {
    async fn send(&self, message: &str) -> Result<(), Box<dyn std::error::Error>> {
        Notification::new()
            .summary("Command Completed")
            .body(message)
            .show()?;
        Ok(())
    }
}
