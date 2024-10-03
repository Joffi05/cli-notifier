// src/notifier/manager.rs
use crate::Notifier;
use std::sync::Arc;

pub struct NotifierManager {
    notifiers: Vec<Arc<dyn Notifier + Send + Sync>>,
}

impl NotifierManager {
    pub fn new() -> Self {
        Self {
            notifiers: Vec::new(),
        }
    }

    pub fn add_notifier<N: Notifier + Send + Sync + 'static>(&mut self, notifier: N) {
        self.notifiers.push(Arc::new(notifier));
    }

    pub async fn send_all(&self, message: &str) -> Result<(), Box<dyn std::error::Error>> {
        for notifier in &self.notifiers {
            notifier.send(message).await?;
        }
        Ok(())
    }
}
