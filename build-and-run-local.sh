./gradlew clean buildDockerImage && docker-compose down && docker-compose -f docker-compose.yml -f docker-compose-debug.yml up -d