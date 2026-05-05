const { v4: uuidv4 } = require('uuid');
const { BadRequest, NotFound } = require('http-errors');

const { db } = require('../configs/dbConfig');

class Booking {
  static checkFlightStatus(flight) {
    if (flight.status === 'Arrived' || flight.status === 'Cancelled') {
      throw new BadRequest('Cannot book tickets for a flight that has already arrived or been cancelled');
    }
  }

  static checkFlightInResult(result) {
    if (!result || result.rowCount === 0) {
      throw new NotFound('Flight not found');
    }
  }

  static async getFlightForBooking(transaction, booking) {
    // airplane_code lives on routes, not flights; use temporal join to get it
    const QUERY = `
      SELECT
        f.flight_id       AS "flightId",
        f.route_no        AS "routeNo",
        f.status,
        r.airplane_code   AS "airplaneCode"
      FROM bookings.flights AS f
      JOIN bookings.routes AS r
        ON r.route_no = f.route_no AND r.validity @> f.scheduled_departure
      WHERE f.route_no = $1
        AND f.scheduled_departure::date = $2::date
    `;
    const result = await transaction.query(QUERY, [booking.flightNo, booking.flightDate]);
    this.checkFlightInResult(result);
    return result.rows[0];
  }

  static checkSeatsLeft(count) {
    if (parseInt(count, 10) <= 0) {
      throw new BadRequest('There are no available seats for the flight');
    }
  }

  // Count total seats for the fare class on this airplane minus already-booked segments
  static async getSeatsInFlight(transaction, booking, flight) {
    // flights table has airplane_code directly (no separate airplanes view needed)
    const QUERY = `
      SELECT
        (SELECT COUNT(*) FROM bookings.seats
         WHERE airplane_code = $1 AND fare_conditions = $2)
        -
        (SELECT COUNT(*) FROM bookings.segments
         WHERE flight_id = $3 AND fare_conditions = $2)
        AS count
    `;
    const result = await transaction.query(QUERY, [
      flight.airplaneCode,
      booking.fareConditions,
      flight.flightId,
    ]);
    return result.rows[0].count;
  }

  // Get a reference price from pricing_rules; fall back to existing segments price
  static async getTicketPrice(transaction, booking, flight) {
    // Try pricing_rules first (Task D4 output), fall back if table doesn't exist
    let result = { rowCount: 0 };
    try {
      await transaction.query('SAVEPOINT price_lookup');
      const RULES_QUERY = `
        SELECT base_price AS price
        FROM bookings.pricing_rules
        WHERE route_no = $1 AND fare_conditions = $2
        LIMIT 1;
      `;
      result = await transaction.query(RULES_QUERY, [flight.routeNo, booking.fareConditions]);
      await transaction.query('RELEASE SAVEPOINT price_lookup');
    } catch (_) {
      // pricing_rules table doesn't exist yet, roll back to savepoint and fall through
      await transaction.query('ROLLBACK TO SAVEPOINT price_lookup');
    }

    if (result.rowCount === 0) {
      // Fallback: use the minimum price from existing segments for this flight
      const SEGMENTS_QUERY = `
        SELECT price
        FROM bookings.segments
        WHERE flight_id = $1 AND fare_conditions = $2
        ORDER BY price
        LIMIT 1;
      `;
      result = await transaction.query(SEGMENTS_QUERY, [flight.flightId, booking.fareConditions]);
    }

    if (result.rowCount === 0) {
      throw new BadRequest('No pricing information available for the selected fare class');
    }
    return result.rows[0].price;
  }

  static async insertBooking(transaction, booking) {
    const QUERY = `
      INSERT INTO bookings.bookings (book_ref, book_date, total_amount)
      VALUES ($1, now(), $2)
      RETURNING book_ref AS "bookRef";
    `;
    const result = await transaction.query(QUERY, [booking.bookRef, booking.totalAmount]);
    return result.rows[0].bookRef;
  }

  static async insertTicket(transaction, ticket) {
    const QUERY = `
      INSERT INTO bookings.tickets (ticket_no, book_ref, passenger_id, passenger_name, outbound)
      VALUES ($1, $2, $3, $4, $5)
      RETURNING ticket_no AS "ticketNo";
    `;
    const result = await transaction.query(QUERY, [
      ticket.ticketNo,
      ticket.bookRef,
      ticket.passengerId,
      ticket.passengerName,
      ticket.outbound ?? true,
    ]);
    return result.rows[0].ticketNo;
  }

  // Insert into segments (new name for ticket_flights); price field (was amount)
  static async insertSegment(transaction, segment) {
    const QUERY = `
      INSERT INTO bookings.segments (ticket_no, flight_id, fare_conditions, price)
      VALUES ($1, $2, $3, $4);
    `;
    await transaction.query(QUERY, [
      segment.ticketNo,
      segment.flightId,
      segment.fareConditions,
      segment.price,
    ]);
  }

  static async createBooking(data) {
    const transaction = await db.connect();
    try {
      await transaction.query('BEGIN');

      const flight = await this.getFlightForBooking(transaction, data);
      this.checkFlightStatus(flight);

      const seats = await this.getSeatsInFlight(transaction, data, flight);
      this.checkSeatsLeft(seats);

      const price = await this.getTicketPrice(transaction, data, flight);

      const bookRef = uuidv4().replace(/-/g, '').slice(0, 6).toUpperCase();
      await this.insertBooking(transaction, { bookRef, totalAmount: price });
      data.bookRef = bookRef;

      const ticketNo = uuidv4().replace(/-/g, '').slice(0, 13);
      await this.insertTicket(transaction, {
        ticketNo,
        bookRef,
        passengerId: data.passengerId,
        passengerName: data.passengerName,
      });
      data.ticketNo = ticketNo;

      await this.insertSegment(transaction, {
        ticketNo,
        flightId: flight.flightId,
        fareConditions: data.fareConditions,
        price,
      });

      await transaction.query('COMMIT');
      return data;
    } catch (error) {
      await transaction.query('ROLLBACK');
      throw error;
    } finally {
      transaction.release();
    }
  }
}

module.exports = Booking;
