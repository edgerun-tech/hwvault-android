# Android StrongBox MVP

This document defines the Android-side hardware-backed approval flow for HWVault.

## Goal

Use Android StrongBox-backed keys to approve sensitive secret operations as a second factor.

## Flow

1. Resolver backend creates approval request (`requestId`, action, secretId, expiry).
2. Android device receives pending request.
3. User confirms request on device.
4. App signs canonical challenge with StrongBox key.
5. App submits signed decision to backend.
6. Backend verifies signature + attestation policy and marks approved/denied.

## Canonical challenge format

`requestId|action|secretId|issuedAtUnix|expiresAtUnix`

## Security properties

- Private signing key is generated in Android Keystore with `setIsStrongBoxBacked(true)` when available.
- Approval signature is bound to request fields, preventing generic replay.
- Backend should enforce expiry and one-time request usage.

## Integration target

Compatible with hwvault resolver HTTP second-factor backend:

- create: `POST /v1/approvals`
- poll: `GET /v1/approvals/{requestId}`
- decision submit (android side): `POST /v1/approvals/{requestId}/decision`

## MVP limitations

- This PR is a scaffold, not a full app integration.
- Backend verification logic is not included in this repo yet.
- BiometricPrompt gating and auth-bound key use are not enforced by default in this MVP.

## Production hardening TODO

- Verify StrongBox attestation chain server-side and pin expected root set.
- Require biometric confirmation before signing (BiometricPrompt + auth-bound key usage where device supports).
- Add nonce/challenge from server to avoid any client-generated ambiguity.
- Add offline denial behavior and secure local queueing.
