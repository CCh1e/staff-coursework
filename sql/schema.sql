-- Структура бази даних «Облік підрозділів і працівників»
-- СУБД: PostgreSQL 14+

DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS departments;

CREATE TABLE departments (
    id       SERIAL       PRIMARY KEY,
    name     VARCHAR(100) NOT NULL UNIQUE,
    location VARCHAR(100),
    phone    VARCHAR(20)
);

CREATE TABLE employees (
    id            SERIAL        PRIMARY KEY,
    last_name     VARCHAR(50)   NOT NULL,
    first_name    VARCHAR(50)   NOT NULL,
    middle_name   VARCHAR(50),
    position      VARCHAR(100)  NOT NULL,
    salary        NUMERIC(12,2) NOT NULL CHECK (salary >= 0),
    hire_date     DATE          NOT NULL,
    email         VARCHAR(100)  UNIQUE,
    department_id INTEGER       NOT NULL REFERENCES departments(id) ON DELETE RESTRICT
);

CREATE INDEX idx_employees_department ON employees(department_id);
