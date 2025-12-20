# Changelog

All notable changes to the SCS Android SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-12-20

### Added
- Initial release of SCS Android SDK
- Authentication module with register, login, logout, and profile management
- Auth state observation via Flow and LiveData
- Database module with document CRUD operations and subcollections
- Query builder with filters, ordering, and pagination
- Storage module with file upload, download, and management
- Realtime Database with WebSocket synchronization
- Cloud Messaging for push notifications with topic support
- Remote Configuration for dynamic app settings
- Cloud Functions invocation with HttpsCallable support
- Machine Learning APIs for text recognition and image labeling
- AI Services for chat, completion, and image generation
- Fluent Chat Builder API

### Features
- Kotlin-first API design with coroutines support
- Flow and LiveData integration for reactive programming
- Automatic token management and persistence
- Consumer ProGuard rules included
- Typed error handling with ScsException
