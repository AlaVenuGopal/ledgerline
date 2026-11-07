# LedgerLine — AI Prompt & Correction Log (PROMPTS.md)

This log records genuine interactions, recommendations, critical evaluations, and corrections made during the development of LedgerLine for the Snovasys Fresher Hiring Challenge 2026.

---

### Entry 1 — Phase 0: Environment Inspection & Build Tool Strategy
- **Date**: 2026-11-07
- **AI Tool Used**: Google Antigravity (Gemini 3.8 Flash)
- **What I asked it to do**:
  Inspect the computer environment, check installed versions of Java, Maven, Node.js, npm, Git, PostgreSQL, and Docker, confirm whether the workspace is empty, identify missing prerequisites, and propose a pinned stack.
- **What the AI generated or suggested**:
  - The AI ran environment inspection commands across Windows PowerShell.
  - The initial PowerShell scripts failed with syntax errors due to string interpolation of `$_` inside standard heredocs before switching to single-quoted script blocks.
  - The AI reported that Java (JDK 23), Node (v22.17.0), npm (10.9.2), and Git (2.54.0) were installed, but Maven (`mvn`), PostgreSQL, and Docker were missing.
  - The AI suggested installing Apache Maven globally.
- **What was incorrect, incomplete or risky**:
  1. Requiring a global system-wide Maven installation (`mvn`) introduces environment drift and configuration risks for evaluators who run the project on their own machines.
  2. The challenge specification requests Java 21 compatibility, but the installed JDK on this machine is JDK 23.
- **How I verified the issue**:
  1. Executed `mvn -version`, which returned `CommandNotFoundException`.
  2. Executed `javac -version`, confirming JDK 23 is installed at `C:\Program Files\Java\jdk-23`.
- **What I changed or rejected**:
  1. Rejected mandatory global Maven installation. Instead, we use the standard Maven Wrapper (`mvnw` / `mvnw.cmd`) pinned to Maven 3.9.9. This guarantees the evaluator can clone and run `./mvnw test` or `mvnw.cmd test` immediately without manually installing Maven.
  2. Configured the Maven compiler plugin to use `--release 21` to strictly enforce Java 21 bytecode and standard API compatibility on JDK 23.
  3. Identified that PostgreSQL 16 is available via Windows `winget` or can be paired with managed cloud PostgreSQL for production deployment.
- **Relevant Git commit**: (Will be recorded with initial project initialization commit)

---
