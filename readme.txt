http://localhost:8080/
http://localhost:8080/weather
https://endpoint-one-2-205823180568.us-central1.run.app
https://endpoint-one-2-205823180568.us-central1.run.app/weather
https://msio-205823180568.us-central1.run.app

Gemini
https://ai.google.dev/gemini-api/docs
https://aistudio.google.com/apikey

Create the image ..................... /Users/michaelsimpson/Desktop/service-2025
                 ..................... open docker desktop or run the docker daemon some other way
                 ..................... docker build --platform linux/amd64 -t service2025 .
Run the image ........................ ensure host port and container port are both 8080
Upload it to GCP Artifact registry ... docker tag service2025 us-central1-docker.pkg.dev/endpoint-one/endpoint-one/service2025:<mmddyy>
                                   ... docker push us-central1-docker.pkg.dev/endpoint-one/endpoint-one/service2025:<mmddyy>
Update image in cloud run ............ gcloud run services update endpoint-one-2 --region us-central1 --platform managed --image us-central1-docker.pkg.dev/endpoint-one/endpoint-one/client2026:<mmddyy>

Sign into google cloud ............... gcloud auth login
Sign out of google cloud ............. gcloud auth revoke
Add artifact registry location ....... gcloud auth configure-docker us-central1-docker.pkg.dev

jwt.io can be used to decode and examine oauth2 tokens.

A new Firebase service account key can be generated from Firebase console > Project settings > Service accounts.

https://console.firebase.google.com/
https://console.cloud.google.com/

https://cloud.google.com/run/docs/tutorials/identity-platform#cloudrun_user_auth_jwt-java
https://firebase.google.com/docs/auth/admin/verify-id-tokens
https://firebase.google.com/docs/admin/setup
https://firebase.google.com/docs/auth/admin/verify-id-tokens
