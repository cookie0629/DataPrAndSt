const { db } = require('../configs/dbConfig');

class City {
  static async getCities(limit = 10, page = 1) {
    const QUERY = `
      SELECT DISTINCT city->>'en' AS city
      FROM bookings.airports_data
      WHERE city->>'en' IS NOT NULL
      ORDER BY city
      LIMIT $1 OFFSET ($2 - 1) * $1;
    `;
    const { rows } = await db.query(QUERY, [limit, page]);
    return rows?.map(row => row.city) ?? [];
  }
}

module.exports = City;
