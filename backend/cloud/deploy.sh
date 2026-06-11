#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Configuration
PROJECT_ID="trustphone-dns"
BUCKET_NAME="trustphone-dns-blocklists"
REGION="us-central1"
FUNCTION_NAME="precompileBlocklists"
SCHEDULER_JOB_NAME="daily-blocklist-precompile"

echo "================================================================="
echo "TrustPhone DNS — Cloud Compiler Pipeline Deployer"
echo "================================================================="

# 1. Set the active project
echo "Setting GCP Project to: ${PROJECT_ID}..."
gcloud config set project "${PROJECT_ID}"

# 2. Enable Required APIs
echo "Enabling necessary Cloud APIs..."
gcloud services enable cloudfunctions.googleapis.com cloudbuild.googleapis.com cloudscheduler.googleapis.com

# 3. Deploy Cloud Function
echo "Deploying Cloud Function: ${FUNCTION_NAME} to ${REGION}..."
gcloud functions deploy "${FUNCTION_NAME}" \
    --runtime=nodejs18 \
    --trigger-http \
    --allow-unauthenticated \
    --set-env-vars BLOCKLIST_BUCKET="${BUCKET_NAME}" \
    --region="${REGION}" \
    --timeout=120s \
    --memory=512MB \
    --entry-point="${FUNCTION_NAME}"

# 4. Fetch the Function URL
echo "Retrieving Cloud Function trigger URL..."
FUNCTION_URL=$(gcloud functions describe "${FUNCTION_NAME}" --region="${REGION}" --format="value(httpsTrigger.url)")
echo "Function URL: ${FUNCTION_URL}"

# 5. Create Cloud Scheduler Trigger (Daily at 02:00 UTC)
echo "Creating/Updating Cloud Scheduler job: ${SCHEDULER_JOB_NAME}..."
if gcloud scheduler jobs describe "${SCHEDULER_JOB_NAME}" --location="${REGION}" >/dev/null 2>&1; then
    gcloud scheduler jobs update http "${SCHEDULER_JOB_NAME}" \
        --location="${REGION}" \
        --schedule="0 2 * * *" \
        --uri="${FUNCTION_URL}" \
        --http-method=POST \
        --time-zone="UTC" \
        --description="Daily trigger for TrustPhone DNS blocklist pre-compiler"
else
    gcloud scheduler jobs create http "${SCHEDULER_JOB_NAME}" \
        --location="${REGION}" \
        --schedule="0 2 * * *" \
        --uri="${FUNCTION_URL}" \
        --http-method=POST \
        --time-zone="UTC" \
        --description="Daily trigger for TrustPhone DNS blocklist pre-compiler"
fi

echo "================================================================="
echo "SUCCESS: Cloud Function & Scheduler Job deployed successfully!"
echo "================================================================="
