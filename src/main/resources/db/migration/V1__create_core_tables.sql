-- V1__create_core_tables.sql
SET search_path = public;

-- ---------- users ----------
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(30),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- ---------- passengers ----------
CREATE TABLE IF NOT EXISTS passengers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL,
    full_name VARCHAR(200) NOT NULL,
    document_type VARCHAR(50),
    document_number VARCHAR(100),
    birth_date DATE,
    phone_number VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_passenger_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
    );

CREATE INDEX IF NOT EXISTS idx_passengers_user ON passengers(user_id);

-- ---------- routes ----------
CREATE TABLE IF NOT EXISTS routes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE,
    name VARCHAR(150),
    origin VARCHAR(150) NOT NULL,
    destination VARCHAR(150) NOT NULL,
    distance_km NUMERIC(10,2) CHECK (distance_km >= 0),
    duration_min INTEGER CHECK (duration_min >= 0)
    );

CREATE INDEX IF NOT EXISTS idx_routes_origin_dest ON routes(origin, destination);

-- ---------- stops ----------
CREATE TABLE IF NOT EXISTS stops (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    order INTEGER NOT NULL CHECK (order_index >= 0),
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    CONSTRAINT fk_stop_route FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_route_orderindex ON stops(route_id, order_index);
CREATE INDEX IF NOT EXISTS idx_stops_route ON stops(route_id);

-- ---------- fare_rules ----------
CREATE TABLE IF NOT EXISTS fare_rules (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL,
    from_stop_id BIGINT NOT NULL,
    to_stop_id BIGINT NOT NULL,
    base_price NUMERIC(12,2) NOT NULL CHECK (base_price >= 0),
    discounts JSONB,
    dynamic_pricing VARCHAR(50) NOT NULL,
    CONSTRAINT fk_farerule_route FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE,
    CONSTRAINT fk_farerule_fromstop FOREIGN KEY (from_stop_id) REFERENCES stops(id) ON DELETE RESTRICT,
    CONSTRAINT fk_farerule_tostop FOREIGN KEY (to_stop_id) REFERENCES stops(id) ON DELETE RESTRICT,
    CONSTRAINT chk_farerule_stops_order CHECK (from_stop_id <> to_stop_id)
    );

CREATE INDEX IF NOT EXISTS idx_farerule_route ON fare_rules(route_id);

-- ---------- buses ----------
CREATE TABLE IF NOT EXISTS buses (
    id BIGSERIAL PRIMARY KEY,
    plate VARCHAR(50) NOT NULL UNIQUE,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    amenities JSONB,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
    );

CREATE INDEX IF NOT EXISTS idx_buses_plate ON buses(plate);

-- ---------- seats ----------
CREATE TABLE IF NOT EXISTS seats (
    id BIGSERIAL PRIMARY KEY,
    bus_id BIGINT NOT NULL,
    number VARCHAR(30) NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    CONSTRAINT fk_seat_bus FOREIGN KEY (bus_id) REFERENCES buses(id) ON DELETE CASCADE
    );

-- unique seat per bus
CREATE UNIQUE INDEX IF NOT EXISTS uq_bus_seatnumber ON seats(bus_id, number);

CREATE INDEX IF NOT EXISTS idx_seats_bus ON seats(bus_id);

-- ---------- trips ----------
CREATE TABLE IF NOT EXISTS trips (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL,
    bus_id BIGINT NOT NULL,
    date DATE NOT NULL,
    departure_at TIMESTAMPTZ,
    arrival_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    overbooking_percent NUMERIC(5,2) DEFAULT 0 CHECK (overbooking_percent >= 0 AND overbooking_percent <= 100),
    CONSTRAINT fk_trip_route FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE RESTRICT,
    CONSTRAINT fk_trip_bus FOREIGN KEY (bus_id) REFERENCES buses(id) ON DELETE RESTRICT
    );

CREATE INDEX IF NOT EXISTS idx_trips_route_date ON trips(route_id, date);
CREATE INDEX IF NOT EXISTS idx_trips_bus_date ON trips(bus_id, date);

