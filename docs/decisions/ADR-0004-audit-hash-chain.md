# ADR-0004: Tamper-Evident Audit Hash Chain

## Status
Accepted

## Context
Ordinary logs are mutable and insufficient for administrative accountability.

## Decision
Append-only `audit_events` with previous/current hash chain; DB runtime role denied UPDATE/DELETE; integrity verifier job; critical ops fail if audit write fails.

## Consequences
Operational complexity for restore/export; stronger forensic guarantees.
