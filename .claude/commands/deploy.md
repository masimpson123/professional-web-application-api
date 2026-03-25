---
name: deploy
description: Commit all changes and deploy to Cloud Run. Usage: /deploy [commit message]
---

Commit all local changes and deploy to the GCP Cloud Run instance. Follow these steps exactly:

## Step 1: Commit all changes

Run `git status` and `git diff` to understand what's changed.

Stage all changed/new files (excluding any secrets or `.env` files), then commit with the message provided by the user as arguments to this skill. If no message was provided, use "deploy" as the commit message. Always append the standard co-authored-by trailer.

If there are no changes to commit, skip to Step 2.

## Step 2: Push to git remote

Run `git push` to push the commit to the remote.

## Step 3: Build and push the Docker image

Run the following commands in sequence:

```bash
docker build --platform linux/amd64 -t service2025 .
docker tag service2025 us-central1-docker.pkg.dev/endpoint-one/endpoint-one/service2025
docker push us-central1-docker.pkg.dev/endpoint-one/endpoint-one/service2025
```

## Step 4: Deploy to Cloud Run

```bash
gcloud run deploy endpoint-one-2 \
  --image us-central1-docker.pkg.dev/endpoint-one/endpoint-one/service2025 \
  --region us-central1 \
  --project endpoint-one \
  --platform managed
```

## Step 5: Report the result

Tell the user whether the deployment succeeded or failed, and provide the Cloud Run service URL if available.
