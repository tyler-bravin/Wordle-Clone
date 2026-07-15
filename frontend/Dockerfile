# --- Build stage -----------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Baked into the static bundle at build time - Coolify passes this as a
# build argument pointing at the deployed backend's public URL.
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# --- Runtime stage -----------------------------------------------------------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

# Not guaranteed to already be present on the base image - see backend/Dockerfile
# for why this is installed explicitly rather than assumed.
RUN apk add --no-cache wget

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost:80/ > /dev/null || exit 1
