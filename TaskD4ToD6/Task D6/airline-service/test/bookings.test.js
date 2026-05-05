const supertest = require('supertest');
const app = require('../app.js');
const request = supertest(app);

// All flights are on 2025-12-01 (the demo DB date range)
// PG0191 flight_id=11052: On Time, 33 Economy seats available
// PG0356 flight_id=11051: Delayed, 0 Business seats available (full)
// PG0225 flight_id=11035: Arrived (on 2025-11-30)

describe('Testing endpoints for bookings resource', () => {
  it('POST "/bookings" should return for correct response created booking with ticket number and correct status', async () => {
    const booking = {
      flightNo: 'PG0191',
      flightDate: '2025-12-01',
      fareConditions: 'Economy',
      passengerId: '_01',
      passengerName: 'Test user',
    };
    return request.post('/bookings').send(booking).then((response) => {
      expect(response.status).toBe(201);
      expect(response.headers['content-type']).toMatch('application/json');
      expect(response.body).toHaveProperty('bookRef');
      expect(response.body).toHaveProperty('ticketNo');
    });
  });

  it('POST "/bookings" should return for incorrect request body BadRequest error with message', async () => {
    const booking = { flightNo: null, flightDate: null };
    return request.post('/bookings').send(booking).then((response) => {
      expect(response.status).toBe(400);
      expect(response.body.message).toBeDefined();
    });
  });

  it('POST "/bookings" should return for arrived flight BadRequest error with message', async () => {
    const booking = {
      flightNo: 'PG0225',
      flightDate: '2025-11-30',
      fareConditions: 'Economy',
      passengerId: '_01',
      passengerName: 'Test user',
    };
    return request.post('/bookings').send(booking).then((response) => {
      expect(response.status).toBe(400);
      expect(response.body.message).toBeDefined();
    });
  });

  it('POST "/bookings" should return BadRequest error with message if there are no available seats for the flight', async () => {
    const booking = {
      flightNo: 'PG0356',
      flightDate: '2025-12-01',
      fareConditions: 'Business',
      passengerId: '_01',
      passengerName: 'Test user',
    };
    return request.post('/bookings').send(booking).then((response) => {
      expect(response.status).toBe(400);
      expect(response.body.message).toBeDefined();
    });
  });

  it('POST "/bookings" should return for non-existing flight NotFound error with message', async () => {
    const booking = {
      flightNo: 'XX9999',
      flightDate: '2025-12-01',
      fareConditions: 'Economy',
      passengerId: '_01',
      passengerName: 'Test user',
    };
    return request.post('/bookings').send(booking).then((response) => {
      expect(response.status).toBe(404);
      expect(response.body.message).toBeDefined();
    });
  });
});
