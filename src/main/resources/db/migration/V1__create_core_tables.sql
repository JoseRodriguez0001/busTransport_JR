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
    "order" INTEGER NOT NULL CHECK ("order" >= 0),
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    CONSTRAINT fk_stop_route FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_route_order ON stops(route_id, "order");
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
    "date" DATE NOT NULL,
    departure_at TIMESTAMPTZ,
    arrival_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    overbooking_percent NUMERIC(5,2) DEFAULT 0 CHECK (overbooking_percent >= 0 AND overbooking_percent <= 100),
    CONSTRAINT fk_trip_route FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE RESTRICT,
    CONSTRAINT fk_trip_bus FOREIGN KEY (bus_id) REFERENCES buses(id) ON DELETE RESTRICT
    );

CREATE INDEX IF NOT EXISTS idx_trips_route_date ON trips(route_id, date);
CREATE INDEX IF NOT EXISTS idx_trips_bus_date ON trips(bus_id, date);

-- ---------- purchases ----------
CREATE TABLE IF NOT EXISTS purchases (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    payment_method VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_purchase_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_purchases_user ON purchases(user_id);


-- ---------- seat_holds ----------
CREATE TABLE IF NOT EXISTS seat_holds (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    seat_number VARCHAR(30) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'HOLD',
    CONSTRAINT fk_seathold_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT fk_seathold_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_trip_seathold_seat ON seat_holds(trip_id, seat_number);
CREATE INDEX IF NOT EXISTS idx_seatholds_trip ON seat_holds(trip_id);
CREATE INDEX IF NOT EXISTS idx_seatholds_user ON seat_holds(user_id);


-- ---------- tickets ----------
CREATE TABLE IF NOT EXISTS tickets (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    passenger_id BIGINT NOT NULL,
    seat_number VARCHAR(30) NOT NULL,
    from_stop_id BIGINT NOT NULL,
    to_stop_id BIGINT NOT NULL,
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'SOLD',
    qr_code VARCHAR(255),
    purchase_id BIGINT NOT NULL,
    CONSTRAINT fk_ticket_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_passenger FOREIGN KEY (passenger_id) REFERENCES passengers(id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_fromstop FOREIGN KEY (from_stop_id) REFERENCES stops(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_tostop FOREIGN KEY (to_stop_id) REFERENCES stops(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_purchase FOREIGN KEY (purchase_id) REFERENCES purchases(id) ON DELETE CASCADE
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_trip_ticket_seat ON tickets(trip_id, seat_number, from_stop_id, to_stop_id);
CREATE INDEX IF NOT EXISTS idx_tickets_trip ON tickets(trip_id);
CREATE INDEX IF NOT EXISTS idx_tickets_passenger ON tickets(passenger_id);


-- ---------- baggage ----------
CREATE TABLE IF NOT EXISTS baggage (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    weight_kg NUMERIC(8,2) CHECK (weight_kg >= 0),
    fee NUMERIC(10,2) DEFAULT 0 CHECK (fee >= 0),
    tag_code VARCHAR(100) UNIQUE,
    CONSTRAINT fk_baggage_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_baggage_ticket ON baggage(ticket_id);


-- ---------- parcels ----------
CREATE TABLE IF NOT EXISTS parcels (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    trip_id BIGINT NOT NULL,
    from_stop_id BIGINT NOT NULL,
    to_stop_id BIGINT NOT NULL,
    sender_name VARCHAR(200) NOT NULL,
    sender_phone VARCHAR(50),
    receiver_name VARCHAR(200) NOT NULL,
    receiver_phone VARCHAR(50),
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    proof_photo_url VARCHAR(500),
    delivery_otp VARCHAR(20),
    CONSTRAINT fk_parcel_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT fk_parcel_fromstop FOREIGN KEY (from_stop_id) REFERENCES stops(id) ON DELETE RESTRICT,
    CONSTRAINT fk_parcel_tostop FOREIGN KEY (to_stop_id) REFERENCES stops(id) ON DELETE RESTRICT
    );

CREATE INDEX IF NOT EXISTS idx_parcels_trip ON parcels(trip_id);
CREATE INDEX IF NOT EXISTS idx_parcels_status ON parcels(status);


-- ---------- assignments ----------
CREATE TABLE IF NOT EXISTS assignments (
   id BIGSERIAL PRIMARY KEY,
   trip_id BIGINT NOT NULL,
   driver_id BIGINT NOT NULL,
   dispatcher_id BIGINT NOT NULL,
   checklist_ok BOOLEAN DEFAULT FALSE,
   assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_assignment_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_driver FOREIGN KEY (driver_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_assignment_dispatcher FOREIGN KEY (dispatcher_id) REFERENCES users(id) ON DELETE RESTRICT
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_assignment_trip ON assignments(trip_id);


-- ---------- incidents ----------
CREATE TABLE IF NOT EXISTS incidents (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    "type" VARCHAR(50) NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_incidents_entity ON incidents(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_incidents_type ON incidents(type);


-- ---------- config ----------
CREATE TABLE IF NOT EXISTS config (
    id BIGSERIAL PRIMARY KEY,
    "key" VARCHAR(100) UNIQUE NOT NULL,
    "value" VARCHAR(255) NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_config_key ON config(key);


-- ---------- kpis ----------
CREATE TABLE IF NOT EXISTS kpis (
    id BIGSERIAL PRIMARY KEY,
    "name" VARCHAR(100) UNIQUE NOT NULL,
    "value" NUMERIC(10,4) NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_kpis_name ON kpis(name);

--fin
COMMIT;