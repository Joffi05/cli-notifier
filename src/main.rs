use clap::{Arg, Command};
use tokio::process::Command as TokioCommand;
use std::fs;
use log::{error, info};
use env_logger;
//use dirs;

mod config;
mod notifier;

use notifier::desktop::DesktopNotifier;
use notifier::webhook::WebhookNotifier;
use notifier::manager::NotifierManager;
use crate::config::Config;

use async_trait::async_trait;

#[async_trait]
pub trait Notifier {
    async fn send(&self, message: &str) -> Result<(), Box<dyn std::error::Error>>;
}

use std::error::Error;

pub fn load_config() -> Result<Config, Box<dyn Error>> {
    // Define the default config file name
    let config_file_name = "config.toml";

    // Attempt to load config from the home directory
    if let Some(home_dir) = dirs::home_dir() {
        let home_config_path = home_dir.join(".config/cli-notifier").join(config_file_name);
        if home_config_path.exists() {
            info!("Loading configuration from {}", home_config_path.display());
            let config_content = fs::read_to_string(&home_config_path)?;
            let config: Config = toml::from_str(&config_content)?;
            return Ok(config);
        }
    } else {
        info!("Could not find config file in .config/cli-notifier");
        // If not found in home directory, attempt to load from current directory
        let current_dir = std::env::current_dir()?;
        let current_config_path = current_dir.join(config_file_name);
        if current_config_path.exists() {
            info!("Loading configuration from {}", current_config_path.display());
            let config_content = fs::read_to_string(&current_config_path)?;
            let config: Config = toml::from_str(&config_content)?;
            return Ok(config);
        }
    }



    // If config file is not found in either location, return an error
    Err(format!(
        "Configuration file '{}' not found in home directory (~/.config/cli-notifier) or current directory.",
        config_file_name,
    )
    .into())
}


#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    env_logger::init();

    let matches = Command::new("li")
        .version("0.1.0")
        .about("Runs a command and sends a notification upon completion")
        .arg(
            Arg::new("command")
                .required(true)
                .num_args(1..)
                .allow_hyphen_values(true)
                .help("The command to execute"),
        )
        .get_matches();

    let command: Vec<&str> = matches
        .get_many::<String>("command")
        .unwrap()
        .map(|s| s.as_str())
        .collect();

    if command.is_empty() {
        eprintln!("No command provided.");
        std::process::exit(1);
    }

    // Load configuration
    let config = load_config()?;

    // Initialize NotifierManager
    let mut notifier_manager = NotifierManager::new();

    if let Some(_desktop) = config.notifiers.desktop {
        notifier_manager.add_notifier(DesktopNotifier);
    }

    if let Some(webhook) = config.notifiers.webhook {
        notifier_manager.add_notifier(WebhookNotifier::new(webhook.url, webhook.secret));
    }

    // Execute the command
    let mut child = TokioCommand::new(command[0])
        .args(&command[1..])
        .spawn()?;

    let status = child.wait().await?;

    // Prepare the notification message
    let message = format!(
        "Command `{}` completed with status: {}",
        command.join(" "),
        status
    );

    info!("Executing command: {:?}", command);

    if let Err(e) = notifier_manager.send_all(&message).await {
        error!("Failed to send notifications: {}", e);
    }

    Ok(())
}
