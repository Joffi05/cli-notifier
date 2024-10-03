# webhook_server.py

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
import logging

app = FastAPI()

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class WebhookPayload(BaseModel):
    text: str

@app.post("/webhook")
async def receive_webhook(payload: WebhookPayload, request: Request):
    logger.info(f"Received webhook payload: {payload.text}")
    print(f"Received webhook payload: {payload.text}")
    return {"status": "success"}
