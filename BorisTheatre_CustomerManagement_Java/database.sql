PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS tblCustomer (
    CustomerID INTEGER PRIMARY KEY AUTOINCREMENT,
    FullName TEXT NOT NULL,
    EmailAddress TEXT NOT NULL UNIQUE,
    PhoneNumber TEXT,
    DateJoined TEXT NOT NULL,
    IsLoyaltyMember INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tblOrder (
    OrderID INTEGER PRIMARY KEY AUTOINCREMENT,
    CustomerID INTEGER NOT NULL,
    StaffID INTEGER NOT NULL DEFAULT 1,
    OrderDate TEXT NOT NULL,
    OrderStatus TEXT NOT NULL,
    TotalAmount REAL NOT NULL,
    IsPaid INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (CustomerID) REFERENCES tblCustomer(CustomerID)
);

INSERT INTO tblCustomer (FullName, EmailAddress, PhoneNumber, DateJoined, IsLoyaltyMember) VALUES
('Alice Johnson', 'alice@example.com', '07700900111', '2026-11-03', 1),
('Bob Smith', 'bob.smith@example.com', '+447700900222', '2026-11-10', 0),
('Charlie Brown', 'charlie@example.com', '', '2026-11-13', 0);

INSERT INTO tblOrder (CustomerID, StaffID, OrderDate, OrderStatus, TotalAmount, IsPaid) VALUES
(1, 1, '2026-11-08', 'Confirmed', 45.00, 1),
(1, 1, '2026-11-12', 'Pending', 30.00, 0);