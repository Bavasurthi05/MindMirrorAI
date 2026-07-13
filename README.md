# Mental Health Analytics Platform

A full-stack AI-powered mental health analytics platform built with React + TypeScript, Spring Boot, FastAPI, MySQL, JWT authentication, and Tailwind CSS.

## Project Structure

- frontend/: React + TypeScript + Vite + Tailwind CSS frontend
- backend/: Spring Boot Java backend
- ml-service/: FastAPI Python machine learning service
- database/: MySQL schema and migration files
- docs/: documentation

## Quick Start

### Frontend
cd frontend
npm install
npm run dev

### Backend
cd backend
mvn spring-boot:run

### ML Service
cd ml-service
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\\Scripts\\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

### Database
- Start MySQL
- Create the database using database/schema/init.sql

## Environment
Copy .env.example to .env and update values as needed.

## Notes
This repository contains the initial project skeleton and configuration only. Business logic and feature implementations are intentionally not included.
