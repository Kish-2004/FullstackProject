# 📚 Course Registration System – Full Stack Spring Boot + React App

This is a complete full stack project that allows users to manage students and courses with features like course assignment, microservices communication, and a user-friendly React frontend.

---

## 🚀 Features

- 👨‍🎓 Add, Edit, Delete Students  
- 📘 Add, Edit, Delete Courses  
- 🔗 Assign Courses to Students  
- 🔍 Service Discovery using Eureka  
- 🌐 Routing with Spring Cloud API Gateway  
- ⚙️ (Optional) Spring Cloud Config Server  
- 🎨 React UI with form validation, toasts, and routing  
- ✅ Postman tested APIs  
- 🧪 Unit Tests for Student and Course services  

---

## 🧾 Project Structure

```
/project-root
├── student-service        # Spring Boot microservice
├── course-service         # Spring Boot microservice
├── eureka-server          # Eureka Discovery Server
├── api-gateway            # API Gateway
├── config-server          # (Optional) Config Server
├── frontend               # React App
└── mysql-db               # MySQL with student_db and course_db
```

---

## 🔧 Tech Stack

| Layer         | Technology               |
|---------------|---------------------------|
| Backend       | Spring Boot, Spring Data JPA |
| Frontend      | React, Axios, Bootstrap   |
| Microservices | Eureka Server, Spring Cloud Gateway |
| Database      | MySQL                     |
| Testing       | JUnit, Mockito, Postman   |

---

## 💻 How to Run the Project

### 🔹 Backend Setup

1. ✅ **Create MySQL databases:**
```sql
CREATE DATABASE student_db;
CREATE DATABASE course_db;
```

2. ✅ **Update DB credentials** in `application.properties` of each service.

3. ✅ **Run the services in this order:**
   - `eureka-server` (port `8761`)
   - `config-server` (port `8888`, optional)
   - `student-service` (port `8081`)
   - `course-service` (port `8082`)
   - `api-gateway` (port `8080`)

> Confirm that each service registers with **Eureka**.

---

### 🔹 Frontend Setup

1. Open the `frontend` folder in terminal.

2. Install dependencies:
```bash
npm install
```

3. Run the app:
```bash
npm start
```

4. App runs at:  
`http://localhost:3000`

> Frontend communicates only with API Gateway on port `8080`.

---

## 📡 API Gateway Routing

| Route            | Forwarded To             |
|------------------|--------------------------|
| `/student/**`    | http://localhost:8081    |
| `/course/**`     | http://localhost:8082    |

---

## 🧪 API Endpoints (via Gateway)

| Endpoint                        | Method | Description              |
|----------------------------------|--------|--------------------------|
| `/student/api/students`         | GET    | Fetch all students       |
| `/student/api/students`         | POST   | Add new student          |
| `/student/api/students/{id}`    | PUT    | Update student           |
| `/student/api/students/{id}`    | DELETE | Delete student           |
| `/course/api/courses`           | GET    | Fetch all courses        |
| `/course/api/courses`           | POST   | Add new course           |
| `/course/api/courses/{id}`      | PUT    | Update course            |
| `/course/api/courses/{id}`      | DELETE | Delete course            |
| `/student/api/assign`           | POST   | Assign course to student |

---

## 🧪 Testing

- ✅ 26 test cases passed in **Student Service**  
- ✅ 22 test cases passed in **Course Service**  
- ✅ APIs tested in **Postman**  
- ✅ Uses JUnit, Mockito, MockMvc  

---

## 📸 Frontend (React) Features

- 🧑‍🎓 Student Management Page  
- 📘 Course Management Page  
- ➕ Assign Course to Student  
- ✅ Validation + Error Handling  
- 🎯 Toast Notifications (success/failure)  
- 🔀 React Router Navigation  
- 📊 (Optional) Service Status Page  

---

## ✅ Final Checklist

| Feature                  | Status   |
|--------------------------|----------|
| Student CRUD             | ✅ Done   |
| Course CRUD              | ✅ Done   |
| Assign Course to Student | ✅ Done   |
| Eureka Server            | ✅ Running|
| API Gateway              | ✅ Configured |
| MySQL Database           | ✅ Connected |
| Frontend React App       | ✅ Working |
| Unit Tests               | ✅ Passed |

---

## 🧑‍💻 Author

**Kishore Kumar**  
Final Full Stack Microservices Project  
Built using Spring Boot + React + MySQL

---

## 📄 License

MIT License – Free to use, modify, and distribute
