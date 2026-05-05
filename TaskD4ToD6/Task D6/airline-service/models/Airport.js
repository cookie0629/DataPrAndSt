const { db } = require('../configs/dbConfig');

// airports_data has jsonb fields: airport_name->>'en', city->>'en'
const GET_AIRPORTS_QUERY = `
  SELECT
    airport_code                      AS code,
    airport_name->>'en'               AS name,
    city->>'en'                       AS city,
    coordinates[0]                    AS longitude,
    coordinates[1]                    AS latitude,
    timezone
  FROM bookings.airports_data
`;

class Airport {
  static async getAirports(limit = 10, page = 1) {
    const QUERY = `${GET_AIRPORTS_QUERY} ORDER BY airport_code LIMIT $1 OFFSET ($2 - 1) * $1;`;
    const { rows } = await db.query(QUERY, [limit, page]);
    return rows ?? [];
  }

  static async getAirportsByCity(cityName) {
    const QUERY = `${GET_AIRPORTS_QUERY} WHERE city->>'en' = $1 ORDER BY airport_code;`;
    const { rows } = await db.query(QUERY, [cityName]);
    return rows ?? [];
  }

  // Inbound: flights arriving at airportCode
  static async getInboundSchedules(airportCode) {
    const QUERY = `
      SELECT DISTINCT ON (r.route_no)
        r.route_no                      AS "flightNo",
        (r.scheduled_time + r.duration)::time AS "arrivalTime",
        r.days_of_week                  AS "daysOfWeek",
        dep_a.city->>'en'               AS "origin"
      FROM bookings.routes AS r
      JOIN bookings.airports_data AS dep_a
        ON dep_a.airport_code = r.departure_airport
      WHERE r.arrival_airport = $1
      ORDER BY r.route_no, upper(r.validity) DESC;
    `;
    const { rows } = await db.query(QUERY, [airportCode]);
    return rows ?? [];
  }

  // Outbound: flights departing from airportCode
  static async getOutboundSchedules(airportCode) {
    const QUERY = `
      SELECT DISTINCT ON (r.route_no)
        r.route_no                      AS "flightNo",
        r.scheduled_time                AS "departureTime",
        r.days_of_week                  AS "daysOfWeek",
        arr_a.city->>'en'               AS "destination"
      FROM bookings.routes AS r
      JOIN bookings.airports_data AS arr_a
        ON arr_a.airport_code = r.arrival_airport
      WHERE r.departure_airport = $1
      ORDER BY r.route_no, upper(r.validity) DESC;
    `;
    const { rows } = await db.query(QUERY, [airportCode]);
    return rows ?? [];
  }
}

module.exports = Airport;
