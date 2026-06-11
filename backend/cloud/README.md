# Shield DNS Cloud Pre-compiler Pipeline

This directory contains the Node.js source script to deploy as a serverless Google Cloud Function or Cloud Run Scheduled Job.

The pipeline is designed to run daily. It aggregates multiple open-source DNS blocklists, sanitizes their records, de-duplicates, classifies them into category buckets, creates a `metadata.json` index with version stamps and sha256 checksums, gzips the category blocklist text payloads, and uploads them to a public Google Cloud Storage bucket.

## Sources Included

1.  **StevenBlack Hosts**: Aggregated hosts containing malware, tracking, and ads. (MIT License)
2.  **AdAway Hosts**: Mobile ad focused hosts list. (CC BY 3.0)
3.  **URLhaus Malware Domains**: Realtime malware and C2 domains. (CC0 1.0 Universal)
4.  **Phishing Army**: Dedicated phishing defense blocklist. (Personal/Commercial usage terms)
5.  **EasyPrivacy / AdguardDNS**: Telemetry and tracker lists. (CC BY-SA 3.0)

---

## Deployment Steps

### 1. Enable Google Cloud APIs
Enable the Google Cloud Functions and Cloud Build APIs:
```bash
gcloud services enable cloudfunctions.googleapis.com cloudbuild.googleapis.com
```

### 2. Create GCS Storage Bucket
Create a Google Cloud Storage bucket to host the public compressed files:
```bash
gcloud storage buckets create gs://YOUR_UNIQUE_BUCKET_NAME --location=US
```

Make sure the bucket allows public read requests or set permissions so files can be made public read on upload.

### 3. Deploy Cloud Function
Deploy the script in this directory using the Google Cloud CLI:
```bash
gcloud functions deploy precompileBlocklists \
    --runtime=nodejs18 \
    --trigger-http \
    --allow-unauthenticated \
    --set-env-vars BLOCKLIST_BUCKET=YOUR_UNIQUE_BUCKET_NAME \
    --region=us-central1 \
    --timeout=60s \
    --memory=512MB
```

### 4. Schedule Daily Runs
To run the pre-compiler daily, create a **Google Cloud Scheduler** trigger:
```bash
gcloud scheduler jobs create http daily-blocklist-precompile \
    --schedule="0 2 * * *" \
    --uri="https://us-central1-YOUR_PROJECT_ID.cloudfunctions.net/precompileBlocklists" \
    --http-method=POST \
    --time-zone="UTC"
```
This triggers the compilation function every day at 02:00 AM UTC.
