/**
 * Production observability and metrics for security monitoring.
 *
 * ALERTS & THRESHOLDS:
 *   - login_failures_spike: > 5 failed logins per minute (potential brute force)
 *   - account_lockouts_spike: > 2 lockouts per minute (distributed attack)
 *   - audit_persistence_failures: any write failure to audit_events table
 *   - redis_unavailable: connection pool exhausted or health check fails
 *   - postgres_connection_exhaustion: pool > 90% capacity
 *
 * Metrics are tagged with 'service=portal' and 'environment' for routing to dashboards.
 */
package com.company.portal.shared.monitoring;
