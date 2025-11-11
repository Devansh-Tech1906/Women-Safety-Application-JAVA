Women's Safety Application


This is a Java-based desktop application designed to enhance women's safety. It features a simple user interface for accessing essential safety tools and managing personal information, all backed by a robust database.

The application's core strength is its backend, which uses Java Database Connectivity (JDBC) to connect to a PostgreSQL database. This allows the program to securely store and retrieve user data, emergency contacts, and other critical information. This project demonstrates a practical integration of a Java Swing frontend with a database backend.

Features


->User System: Secure user registration and login with SHA-256 password hashing.

->Admin Panel: A separate dashboard for administrators to manage the application.

->Emergency Contacts: Users can add and view their personal emergency contacts.

->Incident Reporting: Users can report incidents with details, location, and severity.

->Alerts & Broadcasts: Users can send quick alerts, and admins can broadcast announcements to all users.

->Safety Resources: A built-in dialog provides static safety tips and helpline numbers.



Technologies Used


->Language: Java

->UI: Java Swing

->Database: PostgreSQL

->Connectivity: JDBC

------------------------------------------------------------------------

How to Run
1. Prerequisites


Java JDK 11 or newer.

PostgreSQL database server installed and running.

The PostgreSQL JDBC Driver (.jar file). You can download it from the official website.

2. Database Setup


Open your PostgreSQL admin tool (like psql or PGAdmin).

Create a new database named womensafetydb:



You can log in with the admin user admin (password: adminpass) or register a new user.
