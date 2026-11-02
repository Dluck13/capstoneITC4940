# Registration security notes (V4.1)

Public registration (`POST /api/auth/register`) and administrator account creation
(`POST /api/admin/users`) rely on existing session + CSRF protections.

For production deployment, add network-level abuse controls in front of these APIs:

- Per-IP and per-account rate limits for registration and login attempts.
- Optional CAPTCHA or email verification on public registration.
- Monitoring/alerting for spikes in registration failures and duplicate-email attempts.
- Lockout/backoff controls at gateway or WAF for repeated credential stuffing signals.

This project keeps the backend business rules and authentication architecture unchanged,
and does not add infrastructure-specific rate limiting in application code.

