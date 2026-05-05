const { NotFound, BadRequest } = require('http-errors');
const { db } = require('../configs/dbConfig');

class Flight {
  static checkBoardingPassDoesNotExist(boardingPass) {
    if (boardingPass) {
      throw new BadRequest('You have already checked in for the specified flight');
    }
  }

  static async getBoardingPass(transaction, booking) {
    const QUERY = `
      SELECT * FROM bookings.boarding_passes
      WHERE ticket_no = $1 AND flight_id = $2;
    `;
    const result = await transaction.query(QUERY, [booking.ticketNo, booking.flightId]);
    return result?.rows[0];
  }

  static checkFlightExists(flight) {
    if (!flight) {
      throw new NotFound('The specified flight for check-in was not found');
    }
  }

  static async getFlightById(transaction, booking) {
    // airplane_code is on routes, not flights
    const QUERY = `
      SELECT f.flight_id AS "flightId", r.airplane_code AS "airplaneCode", f.status
      FROM bookings.flights AS f
      JOIN bookings.routes AS r
        ON r.route_no = f.route_no AND r.validity @> f.scheduled_departure
      WHERE f.flight_id = $1
        AND f.status IN ('On Time', 'Delayed', 'Boarding');
    `;
    const result = await transaction.query(QUERY, [booking.flightId]);
    return result?.rows[0];
  }

  static checkTicketFlight(ticketFlight) {
    if (!ticketFlight) {
      throw new BadRequest('To check in for a flight, a valid ticket for this flight is required');
    }
  }

  static async getSegment(transaction, booking) {
    const QUERY = `
      SELECT flight_id AS "flightId", fare_conditions AS "fareConditions"
      FROM bookings.segments
      WHERE ticket_no = $1 AND flight_id = $2;
    `;
    const result = await transaction.query(QUERY, [booking.ticketNo, booking.flightId]);
    return result?.rows[0];
  }

  static checkSeatAvailable(seat) {
    if (!seat) {
      throw new BadRequest('No available seats for the selected fare class on this flight');
    }
  }

  static async getSeatForBooking(transaction, segment, flight) {
    const QUERY = `
      SELECT seat_no AS "seatNo"
      FROM bookings.seats
      WHERE airplane_code = $1
        AND fare_conditions = $2
        AND seat_no NOT IN (
          SELECT seat_no FROM bookings.boarding_passes WHERE flight_id = $3
        )
      ORDER BY seat_no
      LIMIT 1;
    `;
    const result = await transaction.query(QUERY, [
      flight.airplaneCode,
      segment.fareConditions,
      segment.flightId,
    ]);
    return result?.rows[0];
  }

  static async generateBoardingPassNo(transaction, flightId) {
    const QUERY = `
      SELECT COALESCE(MAX(boarding_no), 0) + 1 AS "boardingNo"
      FROM bookings.boarding_passes
      WHERE flight_id = $1;
    `;
    const result = await transaction.query(QUERY, [flightId]);
    return result?.rows[0].boardingNo;
  }

  static async insertBoardingPass(transaction, boardingPass) {
    const QUERY = `
      INSERT INTO bookings.boarding_passes (ticket_no, flight_id, boarding_no, seat_no)
      VALUES ($1, $2, $3, $4);
    `;
    await transaction.query(QUERY, [
      boardingPass.ticketNo,
      boardingPass.flightId,
      boardingPass.boardingNo,
      boardingPass.seatNo,
    ]);
  }

  static async checkIn(booking) {
    const transaction = await db.connect();
    try {
      await transaction.query('BEGIN');
      const flight = await this.getFlightById(transaction, booking);
      this.checkFlightExists(flight);
      const foundBoardingPass = await this.getBoardingPass(transaction, booking);
      this.checkBoardingPassDoesNotExist(foundBoardingPass);
      const segment = await this.getSegment(transaction, booking);
      this.checkTicketFlight(segment);
      const seat = await this.getSeatForBooking(transaction, segment, flight);
      this.checkSeatAvailable(seat);
      const boardingNo = await this.generateBoardingPassNo(transaction, booking.flightId);
      const boardingPass = {
        boardingNo,
        ticketNo: booking.ticketNo,
        flightId: booking.flightId,
        seatNo: seat.seatNo,
      };
      await this.insertBoardingPass(transaction, boardingPass);
      await transaction.query('COMMIT');
      return boardingPass;
    } catch (error) {
      await transaction.query('ROLLBACK');
      throw error;
    } finally {
      transaction.release();
    }
  }

  // Resolve city name or airport code -> array of airport codes
  static async _resolveAirports(point) {
    const QUERY = `
      SELECT airport_code
      FROM bookings.airports_data
      WHERE airport_code = $1 OR city->>'en' = $1;
    `;
    const { rows } = await db.query(QUERY, [point]);
    return rows.map(r => r.airport_code);
  }

  static async getFlights({ origin, destination, departureDate, fareConditions, transfers }) {
    const originCodes = await this._resolveAirports(origin);
    const destCodes = await this._resolveAirports(destination);
    if (originCodes.length === 0 || destCodes.length === 0) return [];

    const maxTransfers = (transfers === undefined || transfers === null)
      ? 3
      : parseInt(transfers, 10);

    const direct = await this._getDirectFlights(originCodes, destCodes, departureDate, fareConditions);
    if (maxTransfers === 0) return direct;

    const connecting = await this._getConnectingFlights(originCodes, destCodes, departureDate, fareConditions, maxTransfers);
    return [...direct, ...connecting];
  }

  static async _getDirectFlights(originCodes, destCodes, departureDate, fareConditions) {
    const params = [originCodes, destCodes, departureDate];
    const fareFilter = fareConditions
      ? `AND EXISTS (SELECT 1 FROM bookings.segments s WHERE s.flight_id = f.flight_id AND s.fare_conditions = $4)`
      : '';
    if (fareConditions) params.push(fareConditions);

    const QUERY = `
      SELECT
        f.route_no                                        AS "flightNo",
        f.scheduled_departure                             AS "scheduledDeparture",
        f.scheduled_arrival                               AS "scheduledArrival",
        (f.scheduled_arrival - f.scheduled_departure)     AS "scheduledDuration",
        dep_a.airport_name->>'en'                         AS "departureAirportName",
        dep_a.city->>'en'                                 AS "departureCity",
        arr_a.airport_name->>'en'                         AS "arrivalAirportName",
        arr_a.city->>'en'                                 AS "arrivalCity",
        ap.model->>'en'                                   AS "aircraftModel"
      FROM bookings.flights AS f
      JOIN bookings.routes AS r
        ON r.route_no = f.route_no AND r.validity @> f.scheduled_departure
      JOIN bookings.airports_data AS dep_a ON dep_a.airport_code = r.departure_airport
      JOIN bookings.airports_data AS arr_a ON arr_a.airport_code = r.arrival_airport
      JOIN bookings.airplanes_data AS ap ON ap.airplane_code = r.airplane_code
      WHERE r.departure_airport = ANY($1)
        AND r.arrival_airport = ANY($2)
        AND f.scheduled_departure::date = $3::date
        AND f.status NOT IN ('Arrived', 'Cancelled')
        ${fareFilter}
      ORDER BY f.scheduled_departure;
    `;
    const { rows } = await db.query(QUERY, params);
    return rows ?? [];
  }

  static async _getConnectingFlights(originCodes, destCodes, departureDate, fareConditions, maxTransfers) {
    const fareFilter = fareConditions
      ? `AND EXISTS (SELECT 1 FROM bookings.segments s WHERE s.flight_id = f.flight_id AND s.fare_conditions = $4)`
      : '';
    const params = [originCodes, destCodes, departureDate];
    if (fareConditions) params.push(fareConditions);
    params.push(maxTransfers);
    const depthParam = `$${params.length}`;

    const QUERY = `
      WITH RECURSIVE flight_paths AS (
        SELECT
          f.flight_id,
          f.route_no,
          f.scheduled_departure,
          f.scheduled_arrival,
          r.departure_airport,
          r.arrival_airport,
          ARRAY[f.flight_id]                        AS path_ids,
          ARRAY[r.departure_airport::text]          AS path_airports,
          1                          AS depth
        FROM bookings.flights AS f
        JOIN bookings.routes AS r
          ON r.route_no = f.route_no AND r.validity @> f.scheduled_departure
        WHERE r.departure_airport = ANY($1)
          AND f.scheduled_departure::date = $3::date
          AND f.status NOT IN ('Arrived', 'Cancelled')
          ${fareFilter}

        UNION ALL

        SELECT
          f2.flight_id,
          f2.route_no,
          f2.scheduled_departure,
          f2.scheduled_arrival,
          r2.departure_airport,
          r2.arrival_airport,
          fp.path_ids || f2.flight_id,
          fp.path_airports || r2.departure_airport::text,
          fp.depth + 1
        FROM flight_paths AS fp
        JOIN bookings.routes AS r2 ON r2.departure_airport = fp.arrival_airport
        JOIN bookings.flights AS f2
          ON f2.route_no = r2.route_no
          AND r2.validity @> f2.scheduled_departure
          AND f2.scheduled_departure >= fp.scheduled_arrival + INTERVAL '30 minutes'
          AND f2.scheduled_departure < fp.scheduled_departure + INTERVAL '2 days'
          AND f2.status NOT IN ('Arrived', 'Cancelled')
          AND NOT (f2.flight_id = ANY(fp.path_ids))
          AND NOT (r2.arrival_airport = ANY(fp.path_airports))
        WHERE fp.depth < ${depthParam}
          AND NOT (fp.arrival_airport = ANY($2))
      )
      SELECT DISTINCT
        f1.route_no                                         AS "flightNo",
        f1.scheduled_departure                              AS "scheduledDeparture",
        f_last.scheduled_arrival                            AS "scheduledArrival",
        (f_last.scheduled_arrival - f1.scheduled_departure) AS "scheduledDuration",
        dep_a.airport_name->>'en'                           AS "departureAirportName",
        dep_a.city->>'en'                                   AS "departureCity",
        arr_a.airport_name->>'en'                           AS "arrivalAirportName",
        arr_a.city->>'en'                                   AS "arrivalCity",
        ap.model->>'en'                                     AS "aircraftModel",
        fp.path_ids                                         AS "connectionIds",
        fp.depth                                            AS "transfers"
      FROM flight_paths AS fp
      JOIN bookings.flights AS f1 ON f1.flight_id = fp.path_ids[1]
      JOIN bookings.flights AS f_last ON f_last.flight_id = fp.flight_id
      JOIN bookings.routes AS r1
        ON r1.route_no = f1.route_no AND r1.validity @> f1.scheduled_departure
      JOIN bookings.routes AS r_last
        ON r_last.route_no = f_last.route_no AND r_last.validity @> f_last.scheduled_departure
      JOIN bookings.airports_data AS dep_a ON dep_a.airport_code = r1.departure_airport
      JOIN bookings.airports_data AS arr_a ON arr_a.airport_code = r_last.arrival_airport
      JOIN bookings.airplanes_data AS ap ON ap.airplane_code = r1.airplane_code
      WHERE fp.arrival_airport = ANY($2)
        AND fp.depth > 1
      ORDER BY f1.scheduled_departure;
    `;
    const { rows } = await db.query(QUERY, params);
    return rows ?? [];
  }
}

module.exports = Flight;
