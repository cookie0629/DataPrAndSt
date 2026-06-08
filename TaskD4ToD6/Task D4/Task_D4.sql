-- Clean up from any previous run
DROP TABLE IF EXISTS routes_seats_price, routes_with_seats, partial_price, restored_price, full_price;
DROP TABLE IF EXISTS bookings.pricing_rules;

-- Step 1: Average price per (route_no, fare_conditions, seat_no) from historical segments
CREATE TABLE routes_seats_price AS (
    SELECT
        f.route_no,
        s.fare_conditions,
        bp.seat_no,
        AVG(s.price)::numeric(10,2) AS price
    FROM bookings.segments AS s
    JOIN bookings.boarding_passes AS bp
        ON bp.flight_id = s.flight_id AND bp.ticket_no = s.ticket_no
    JOIN bookings.flights AS f
        ON f.flight_id = s.flight_id
    GROUP BY f.route_no, s.fare_conditions, bp.seat_no
);

-- Step 2: All (route_no, seat_no, fare_conditions) combinations from routes + seats
CREATE TABLE routes_with_seats AS (
    SELECT DISTINCT
        r.route_no,
        se.seat_no,
        se.fare_conditions
    FROM bookings.routes AS r
    JOIN bookings.seats AS se ON se.airplane_code = r.airplane_code
);

-- Step 3: Join to get known prices; NULL means no historical data
CREATE TABLE partial_price AS (
    SELECT
        rws.route_no,
        rws.fare_conditions,
        rws.seat_no,
        COALESCE(rsp.price, 0)::numeric(10,2) AS price,
        -- extract row number for same-row interpolation
        CASE
            WHEN LENGTH(rws.seat_no) = 3 THEN SUBSTRING(rws.seat_no FROM 1 FOR 2)
            ELSE SUBSTRING(rws.seat_no FROM 1 FOR 1)
        END AS row_no
    FROM routes_with_seats AS rws
    LEFT JOIN routes_seats_price AS rsp
        ON rsp.route_no = rws.route_no
        AND rsp.seat_no = rws.seat_no
        AND rsp.fare_conditions = rws.fare_conditions
);

-- Step 4: For seats with price=0, find the max price from same row/class/route
CREATE TABLE restored_price AS (
    SELECT
        pz.route_no,
        pz.fare_conditions,
        pz.seat_no,
        pz.row_no,
        MAX(pn.price)::numeric(10,2) AS price
    FROM partial_price AS pz
    INNER JOIN partial_price AS pn
        ON pn.route_no = pz.route_no
        AND pn.fare_conditions = pz.fare_conditions
        AND pn.row_no = pz.row_no
        AND pn.price > 0
    WHERE pz.price = 0
    GROUP BY pz.route_no, pz.seat_no, pz.fare_conditions, pz.row_no
);

-- Step 5: Merge: use restored price where original was 0
CREATE TABLE full_price AS (
    SELECT
        pp.route_no,
        pp.fare_conditions,
        pp.seat_no,
        CASE
            WHEN pp.price = 0 THEN COALESCE(rp.price, 0)
            ELSE pp.price
        END AS price
    FROM partial_price AS pp
    LEFT JOIN restored_price AS rp
        ON rp.route_no = pp.route_no
        AND rp.fare_conditions = pp.fare_conditions
        AND rp.seat_no = pp.seat_no
);

-- Step 6: Build the final pricing_rules table:
--   one row per (route_no, fare_conditions) with the average price across all seats
CREATE TABLE bookings.pricing_rules AS (
    SELECT
        route_no,
        fare_conditions,
        AVG(price)::numeric(10,2) AS base_price,
        COUNT(seat_no)             AS seat_count
    FROM full_price
    WHERE price > 0
    GROUP BY route_no, fare_conditions
    ORDER BY route_no, fare_conditions
);

-- Add primary key
ALTER TABLE bookings.pricing_rules
    ADD CONSTRAINT pricing_rules_pkey PRIMARY KEY (route_no, fare_conditions);

-- Index for fast lookup by route
CREATE INDEX idx_pricing_rules_route_no ON bookings.pricing_rules (route_no);

-- Cleanup intermediate tables
DROP TABLE routes_with_seats, routes_seats_price, partial_price, restored_price, full_price;

-- Verify
SELECT * FROM bookings.pricing_rules ORDER BY route_no, fare_conditions;
