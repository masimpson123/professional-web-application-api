# professional-web-application-api

The Spring Boot API behind the professional web application: Gemini proxy, streaming search, STOMP pub/sub, Firebase-protected endpoints, and Zoom Video SDK token signing.

## URLs

| | Local | Production |
|---|---|---|
| This service | http://localhost:8081/ | https://endpoint-one-2-205823180568.us-central1.run.app |
| Protected endpoint | http://localhost:8081/weather | https://endpoint-one-2-205823180568.us-central1.run.app/weather |
| Web application | | https://msio-205823180568.us-central1.run.app |

## Running locally

Create `localhost.properties` in the project root. It's gitignored and never copied into the Docker image.

```properties
gemini.api.key=
zoom.sdk.key=
zoom.sdk.secret=
# Lets the local UI (npm start) connect to the WebSocket broker.
websocket.allowed-origins=https://msio-u7qjhl7iia-uc.a.run.app,http://localhost:4200
```

Then run `Service2025Application` from IntelliJ. It serves on port 8081, so the Express server can keep 8080. On Cloud Run the secrets come from the `GEMINI_KEY`, `ZOOM_SDK_KEY`, and `ZOOM_SDK_SECRET` environment variables, and the WebSocket broker allows only the production site.

## Deploying

In Claude Code, run `/deploy-to-gcp`. It deploys the local working tree without committing. To deploy by hand:

1. Start Docker Desktop, or run the Docker daemon some other way.
2. Build the image from this directory:

   ```bash
   docker build --platform linux/amd64 -t service2025 .
   ```

   To run the image, map host port 8080 to container port 8080.
3. Upload it to GCP Artifact Registry:

   ```bash
   docker tag service2025 us-central1-docker.pkg.dev/endpoint-one/endpoint-one/service2025:<mmddyy>
   docker push us-central1-docker.pkg.dev/endpoint-one/endpoint-one/service2025:<mmddyy>
   ```
4. Update the image in Cloud Run:

   ```bash
   gcloud run services update endpoint-one-2 --region us-central1 --platform managed --image us-central1-docker.pkg.dev/endpoint-one/endpoint-one/service2025:<mmddyy>
   ```

### Google Cloud sign-in

| Task | Command |
|---|---|
| Sign in | `gcloud auth login` |
| Sign out | `gcloud auth revoke` |
| Let Docker push to Artifact Registry | `gcloud auth configure-docker us-central1-docker.pkg.dev` |

## Firebase

- Generate a new service account key from Firebase console > Project settings > Service accounts.
- [jwt.io](https://jwt.io) decodes OAuth 2.0 tokens so you can inspect them.

## Links

- Gemini: [API docs](https://ai.google.dev/gemini-api/docs), [API keys](https://aistudio.google.com/apikey)
- Zoom: [Developer platform](https://platform.zoom.us/)
- Consoles: [Firebase](https://console.firebase.google.com/), [Google Cloud](https://console.cloud.google.com/)
- [Cloud Run end-user authentication with Identity Platform (Java)](https://cloud.google.com/run/docs/tutorials/identity-platform#cloudrun_user_auth_jwt-java)
- [Verify Firebase ID tokens](https://firebase.google.com/docs/auth/admin/verify-id-tokens)
- [Firebase Admin SDK setup](https://firebase.google.com/docs/admin/setup)
