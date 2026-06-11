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

## 6. Advanced Design Principles (Refined)

### 6.1 Role Overlap & Multi-Class Management
Taiwanese high school teachers often juggle multiple roles simultaneously.
- **Unified Subject View**: Every teacher (Homeroom, Counseling, Admin) is fundamentally a **Subject Teacher**. The app must provide a "My Classes" switcher to manage different groups.
- **The Homeroom Anchor**: For Homeroom teachers, their specific class is an "Anchor" but they still access Subject features for other classes they teach.
- **Counseling Integration**: Counseling records are sensitive but need class-based filtering to help counselors manage their teaching load alongside case work.

### 6.2 Interaction Dynamics
- **Parent-Teacher (The Digital Contact Book)**: 
  - Push notifications for announcements.
  - "Read Receipt" and "Digital Signature" for parents to acknowledge daily logs/grades.
- **Student-Teacher (Academic & Emotional Support)**:
  - Secure "Question Box" for academic help.
  - "Mood Check-in" for emotional pulse-taking.
  - Digital assignment submission with instant teacher feedback.

## 7. Feature Breakdown (Updated)

### 1. Homeroom + Subject (導師兼科任)
- **Dashboard**: Switches between "My Class (Homeroom)" and "Subject Classes".
- **Interaction**: Manage one specific class's attendance and contact book, while managing multiple classes' grades.

### 2. Counseling + Subject (輔導兼科任)
- **Searchable Case Logs**: Encrypted search by Class ID and Student Name.
- **Hybrid View**: Toggle between teaching resources and sensitive individual counseling schedules.

### 3. Parent Interaction Hub
- **Digital Signature**: Parents can "sign" the contact book with a single tap, which updates the teacher's dashboard in real-time.
- **Broadcast System**: Tag messages as "Urgent" (bypasses notification fatigue logic) or "Regular".
