# Taiwan High School Teacher App - Design Document

## 1. Project Overview
A role-centric Android application designed for Taiwan high school teachers, focusing on reducing administrative burden and streamlining daily tasks like attendance, lesson planning, and counseling.

## 2. Core Architecture
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Navigation:** Compose Navigation (Role-based entry)
- **Theme:** Professional, clean, and modern (Educational blue/teal primary colors)

## 3. Role-Based Navigation Strategy
The app will feature a "Role Selector" or a profile-based dashboard. Once a role is selected, the bottom navigation or sidebar will adapt to the specific needs of that role.

### Roles & Key Screens
1. **Homeroom Teacher (導師)**
   - Attendance Dashboard (One-tap check-in)
   - Parent Communication (Broadcasts/Direct messages)
   - Class Management (Student profiles)
2. **Subject Teacher (科任教師)**
   - Lesson Planning (108 Curriculum templates)
   - Assignment & Grading (AI assistant mock-up)
   - Learning Analysis (Performance charts)
3. **Administrative Teacher (行政教師)**
   - School Calendar & Forms
   - Digital Document Approval
   - Resource Booking (Rooms/Equipment)
4. **Counseling Teacher (輔導教師)**
   - Case Management (Secure/Encrypted UI)
   - Mental Health Assessment (Mood tracking)
   - Schedule Management
5. **Department Head (科主任)**
   - Curriculum Planning (Credit structure check)
   - Teacher Supervision (Observation logs)
   - Performance Analytics

## 4. Visual Language
- **Typography:** Noto Sans TC (Standard for Taiwan context)
- **Icons:** Material Icons (Rounded)
- **Color Palette:**
  - Primary: #1E88E5 (Blue) - Trust and Professionalism
  - Secondary: #43A047 (Green) - Growth and Education
  - Surface/Background: Modern light grey/white

## 5. Development Roadmap
1. **Phase 1: Project Scaffolding (Current)**
   - Basic Gradle setup
   - Theme and Foundation (Colors, Shapes, Typography)
2. **Phase 2: Role-Based Shell**
   - Main Entry Activity
   - Role-switching mechanism
   - Generic Dashboard layouts
3. **Phase 3: Feature Prototypes**
   - Interactive Attendance UI
   - Lesson Plan Template Viewer
   - Secure Case Log mock-up
4. **Phase 4: Refinement**
   - Animations and Transitions
   - Data mocking for demonstration
